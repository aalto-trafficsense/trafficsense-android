package fi.aalto.trafficsense.trafficsense.ui;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.*;
import com.google.common.base.Optional;
import com.google.maps.android.geojson.*;
import fi.aalto.trafficsense.trafficsense.R;
import fi.aalto.trafficsense.trafficsense.TrafficSenseApplication;
import fi.aalto.trafficsense.trafficsense.backend.backend_util.ServerNotification;
import fi.aalto.trafficsense.trafficsense.util.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import timber.log.Timber;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static android.support.v7.preference.PreferenceManager.getDefaultSharedPreferences;
import static fi.aalto.trafficsense.trafficsense.util.InternalBroadcasts.LABEL_STATE_INDEX;

public class MainActivity extends AppCompatActivity
        implements  NavigationView.OnNavigationItemSelectedListener,
                    OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMapLoadedCallback,
                    DatePickerDialog.OnDateSetListener {

    private LocalBroadcastManager mLocalBroadcastManager;
    private BroadcastReceiver mBroadcastReceiver;
    private ActionBarDrawerToggle mDrawerToggle;
    private Context mContext;
    private Resources mRes;
    private BackendStorage mStorage;
    private SharedPreferences mPref; // Local prefs in this activity
    private SharedPreferences.Editor mPrefEditor;
    private SharedPreferences mSettings; // Application settings
    private SharedPreferences mServerNotification; // Server notification prefs
    private ActivityPathConverter mActivityPathConverter; // Helper to translate server-originated activities in path features

    private MenuItem mStartupItem;
    private MenuItem mShutdownItem;
    private MenuItem mDebugItem;
    private MenuItem mPathItem;
    private MenuItem mDestItem;
    private MenuItem mTrafficItem;
    private MenuItem mLangDefaultItem;
    private MenuItem mLangStadiItem;
    private MenuItem mTransportReportItem;
    private MenuItem mSurveyItem;
    private TextView mPathDate;
    private Button mPathEdit;
    private RelativeLayout mPathDateLayout;
    private FrameLayout mServiceOffLayout;
    // private FloatingActionButton mFab;

    private GoogleMap mMap;
    private LatLngBounds mBounds; // Tracks user's visibility
    private boolean updateMapBoundsPath=true;
    private Marker mMarker=null;
    private Marker mTrafficAlertMarker =null;
    private Circle mCircle=null;
    private ActivityType latestActivityType=ActivityType.STILL;
    private LatLng latestPosition;
    private LatLng pathEnd;
    private static Calendar pathCal = Calendar.getInstance();
    private final SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat mTimeFormat = new SimpleDateFormat("HH:mm:ss");
    private Set<String> editableActivities;
    private List<Marker> destMarkers = new ArrayList<>();

    private GeoJsonLayer pathLayer=null;
    private GeoJsonFeature clickedLegFeature;

    // Very first view opens in Otaniemi :-)
    private final float initLat = 60.1841396f;
    private final float initLng = 24.8300838f;
    private final float initZoom = 12;

    private final int DEST_ON_MAP = 5; // TODO: Add into settings
    // private final float minDistToDest = 200.0f; // meters
    // private final float maxDistToDest = 200000.0f; // meters

    private final int MY_PERMISSIONS_ACCESS_FINE_LOCATION = 1;

    private final static String snipSeparator = "!";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TrafficSenseApplication.refreshStadi();
        mContext = this;
        mRes = this.getResources();
        mActivityPathConverter = new ActivityPathConverter();
        editableActivities = new HashSet<>(mActivityPathConverter.getEditableSNames());
        setContentView(R.layout.activity_main);
        new ConsentDialog(this).show(); // Only asks for consent if not agreed before

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);

//        mFab = (FloatingActionButton) findViewById(R.id.fab);
//        mFab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                // Toggle traffic display
//                if (mMap!=null) {
//                    showTraffic = !showTraffic;
//                    mMap.setTrafficEnabled(showTraffic);
//                    if (showTraffic) mFab.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(mContext, R.color.colorInVehicle)));
//                    else mFab.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(mContext, R.color.white)));
//                }
////                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
////                        .setAction("Action", null).show();
//            }
//        });
//
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(
                this, drawer, myToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(mDrawerToggle);

        setSupportActionBar(myToolbar);

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Fetch active menu items:
        Menu navMenu = navigationView.getMenu();
        mStartupItem = navMenu.findItem(R.id.nav_startup);
        mShutdownItem = navMenu.findItem(R.id.nav_shutdown);
        mDebugItem = navMenu.findItem(R.id.nav_debug);
        mLangDefaultItem = navMenu.findItem(R.id.nav_lang_default);
        mLangStadiItem = navMenu.findItem(R.id.nav_lang_stadi);
        mTransportReportItem = navMenu.findItem(R.id.nav_transport);
        mSurveyItem = navMenu.findItem(R.id.nav_survey);

        mPathDate = (TextView) findViewById(R.id.main_path_date);
        mPathEdit = (Button) findViewById(R.id.main_path_edit);
        mPathDateLayout = (RelativeLayout) findViewById(R.id.main_path_date_layout);
        mServiceOffLayout = (FrameLayout) findViewById(R.id.main_service_off_layout);


        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mStorage = BackendStorage.create(mContext);

        mSettings = getDefaultSharedPreferences(this);

        // Initialize shared preferences access for persistent storage of values
        mPref = this.getPreferences(Context.MODE_PRIVATE);
        mPrefEditor = mPref.edit();

        if (mPref.getBoolean(SharedPrefs.KEY_SHOW_PATH, false)) {
            pathCal.set(Calendar.YEAR, mPref.getInt(SharedPrefs.KEY_PATH_YEAR, pathCal.get(Calendar.YEAR)));
            pathCal.set(Calendar.MONTH, mPref.getInt(SharedPrefs.KEY_PATH_MONTH, pathCal.get(Calendar.MONTH)));
            pathCal.set(Calendar.DAY_OF_MONTH, mPref.getInt(SharedPrefs.KEY_PATH_DAY, pathCal.get(Calendar.DAY_OF_MONTH)));
        }

        mServerNotification = this.getSharedPreferences(ServerNotification.NOTIFICATION_PREFS_FILE_NAME, Context.MODE_PRIVATE);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Toast.makeText(this, R.string.error_creating_map, Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
        setDrawerLanguage(mSettings.getBoolean(mRes.getString(R.string.settings_locale_stadi_key), false));
    }

    @Override
    public void onResume()
    {
        super.onResume();
        initBroadcastReceiver();
        BroadcastHelper.broadcastViewResumed(mLocalBroadcastManager, true);
        BroadcastHelper.simpleBroadcast(mLocalBroadcastManager, InternalBroadcasts.KEY_MAIN_ACTIVITY_REQ);
        checkLocationPermission(); // Check the dynamic location permissions
        serviceRunningToDrawer(mSettings.getBoolean(mRes.getString(R.string.debug_settings_service_running_key), true));
        Boolean dbg = mSettings.getBoolean(mRes.getString(R.string.debug_settings_debug_mode_key), false);
        mDebugItem.setVisible(dbg);
        mTransportReportItem.setVisible(dbg);
        // If there is a broadcast notification URI, show the drawer item
        if (!mServerNotification.getString(ServerNotification.KEY_NOTIFICATION_URI, "").isEmpty()) {
            mSurveyItem.setVisible(true);
            int notificationType = mServerNotification.getInt(ServerNotification.KEY_NOTIFICATION_TYPE, 0);
            switch (notificationType) {
                case ServerNotification.NOTIFICATION_TYPE_SURVEY:
                    mSurveyItem.setIcon(R.drawable.md_survey);
                    mSurveyItem.setTitle(R.string.navigation_broadcast_survey);
                    break;
                case ServerNotification.NOTIFICATION_TYPE_BROADCASTMESSAGE:
                    mSurveyItem.setIcon(R.drawable.ic_info_outline_24dp);
                    mSurveyItem.setTitle(R.string.navigation_broadcast_message);
                    break;
                default:
                    mSurveyItem.setIcon(R.mipmap.ic_launcher);
                    mSurveyItem.setTitle(R.string.navigation_broadcast_message);
            }

        } else {
            mSurveyItem.setVisible(false);
        }

        // In case alert comes when the map is already drawn
        // In case the map is already loaded
        if (mMap!=null) {
            if (mTrafficAlertMarker==null) checkTrafficAlert();
            checkDestinations();
        }

    }

    /**
     * This callback is triggered when the map is almost ready. (dimensions do not work yet)
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng((double)mPref.getFloat(SharedPrefs.KEY_MAP_LAT, initLat)
                , (double)mPref.getFloat(SharedPrefs.KEY_MAP_LNG, initLng)), mPref.getFloat(SharedPrefs.KEY_MAP_ZOOM, initZoom)));
        mMap.setOnMapLoadedCallback(this);
        mMap.setOnMarkerClickListener(this);

        // Solution from http://stackoverflow.com/a/31629308/5528498
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            @Override
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {

                LinearLayout info = new LinearLayout(mContext);
                info.setOrientation(LinearLayout.VERTICAL);

                TextView title = new TextView(mContext);
                title.setTextColor(Color.BLACK);
                title.setGravity(Gravity.CENTER);
                title.setTypeface(null, Typeface.BOLD);
                title.setText(marker.getTitle());

                TextView snippet = new TextView(mContext);
                snippet.setTextColor(Color.GRAY);
                snippet.setText(marker.getSnippet());

                info.addView(title);
                info.addView(snippet);

                return info;
            }
        });
    }

    /**
     * This callback is triggered when the map is really ready.
     */
    @Override
    public void onMapLoaded() {
        if (mMap != null) {

            checkDestinations();

            if (getSharedBoolean(SharedPrefs.KEY_SHOW_PATH)) { // Redraw current path, if displayed.
                String geoJsonString = mPref.getString(SharedPrefs.KEY_PATH_OBJECT, null);
                // Timber.d("GeoJsonString: " + geoJsonString);
                if (geoJsonString == null) {
                    // Path on but no cached path (probably today) - fetch a fresh path from the server
                    updateMapBoundsPath = true;
                    fetchPath();
                } else {
                    try {
                        pathToMap(new JSONObject(geoJsonString));
                    }
                    catch (JSONException e) {
                        Timber.e("Cached path GeoJson conversion returned an exception: " + e.toString());
                        setPathOff();
                    }

                }
            }

            if (getSharedBoolean(SharedPrefs.KEY_SHOW_TRAFFIC)) {
                mTrafficItem.setIcon(R.drawable.ic_traffic_24dp_on);
                mTrafficItem.setChecked(true);
                mMap.setTrafficEnabled(true);
            }

            // In case alert could not be drawn at resume due to lack of map
            if (mTrafficAlertMarker == null) checkTrafficAlert();

        }
    }

    private void checkDestinations() {
        if (getSharedBoolean(SharedPrefs.KEY_SHOW_DEST)) { // Redraw current destinations, if displayed.
            String destString = null; // Always reload from server: mPref.getString(SharedPrefs.KEY_DEST_OBJECT, null);
            // Timber.d("GeoJsonString: " + geoJsonString);
            if (destString == null) {
                // Destinations on but nothing fetched - get again
                fetchDest();
            } else {
                //Draw destinations to map
                try {
                    processDest(new JSONObject(destString));
                } catch (JSONException e) {
                    Timber.e("onMapLoaded: Destination GeoJson conversion returned an exception: %s", e.toString());
                    setDestOff();
                }
            }
        }
    }

    private void checkTrafficAlert() {
        if (mMap != null) {
            String alertMsg = mServerNotification.getString(ServerNotification.KEY_PTP_ALERT_MESSAGE, "");
            if (!alertMsg.isEmpty()) {
                // An active traffic disorder to be shown
                Timber.d("Showing a traffic alert message");
                Bitmap bitmap = getBitmap(mContext, R.drawable.ic_warning_black_24dp);
                LatLng alertPos = new LatLng(mServerNotification.getFloat(ServerNotification.KEY_PTP_ALERT_LAT, 0),
                        mServerNotification.getFloat(ServerNotification.KEY_PTP_ALERT_LNG, 0));
                mTrafficAlertMarker = mMap.addMarker(new MarkerOptions()
                        .position(alertPos)
                        .title(mServerNotification.getString(ServerNotification.KEY_PTP_ALERT_TITLE, ""))
                        .snippet(alertMsg)
                        .icon(BitmapDescriptorFactory.fromBitmap(bitmap)));
                mTrafficAlertMarker.showInfoWindow();
                if (latestPosition != null) {
                    // User position known, show both the user and the alert on the map
                    MapBounds mBuildBounds = new MapBounds();
                    mBuildBounds.include(alertPos);
                    mBuildBounds.include(latestPosition);
                    // Arrange space for the info window
                    mBuildBounds.include(alertInfoStretch(alertPos));
                    mBuildBounds.update(mMap);
                } else {
                    // User position unknown, just put the alert in the center and zoom back
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(alertPos, initZoom-2));
                }
                // Set also traffic display on for assistance
                mTrafficItem.setIcon(R.drawable.ic_traffic_24dp_on);
                mTrafficItem.setChecked(true);
                mPrefEditor.putBoolean(SharedPrefs.KEY_SHOW_TRAFFIC, true);
                mPrefEditor.commit();
                mMap.setTrafficEnabled(true);
            }
        }
    }

    // Create an extra position to arrange space for the alert info window
    private LatLng alertInfoStretch(LatLng apos) {
        double alertLat = apos.latitude;
        double latestLat = latestPosition.latitude;
        double newLat = alertLat;
        if (alertLat > latestLat) {  // Alert above us - include more
            newLat = alertLat + (alertLat - latestLat);
        }
        double alertLng = apos.longitude;
        double newLng = alertLng + (alertLng - latestPosition.longitude);
        return new LatLng(newLat, newLng);
    }

    @Override
    public boolean onMarkerClick(final Marker clickedMarker) {

        if (clickedMarker.equals(mTrafficAlertMarker)) {
            // Remove the prefs, so the alert doesn't accidentally stick forever
            SharedPreferences.Editor mNotificationPrefEditor = mServerNotification.edit();
            mNotificationPrefEditor.remove(ServerNotification.KEY_PTP_ALERT_LAT);
            mNotificationPrefEditor.remove(ServerNotification.KEY_PTP_ALERT_LNG);
            mNotificationPrefEditor.remove(ServerNotification.KEY_PTP_ALERT_MESSAGE);
            mNotificationPrefEditor.apply();
            mTrafficAlertMarker.remove();
            mTrafficAlertMarker = null;
            Timber.d("Accident marker clicked, removing it.");
            return true;
        }

        int pathEditButtonVisibility = View.INVISIBLE; // Default outcome
        if (pathLayer!=null) {
            // Extract feature ID from marker tag
            String markerId = (String)clickedMarker.getTag();
            String tmpSnip = null;
            Boolean updateTag = false;
            if (markerId==null) { // If not in marker tag, get candidate from the snippet
                tmpSnip = clickedMarker.getSnippet();
            } else {
                if (markerId.length()<1) {
                    tmpSnip = clickedMarker.getSnippet();
                }
            }
            if (tmpSnip!=null) { // Marker tag did not qualify, obtained ID candidate from snippet
                updateTag = true;
                String[] parts = tmpSnip.split(snipSeparator); // Try to split ID and end-user snippet
                if (parts.length>1) { // Snippet had more than one part
                    markerId = parts[0]; // First part is the ID
                    tmpSnip = parts[1]; // Second part should be the end-user snippet
                } else { // Only one part; shouldn't normally happen
                    markerId = parts[0]; // See if there is an ID in there anyway
                    tmpSnip = null; // Reset snippet if it was an ID
                }
            }
            if (markerId!=null) {
                Iterator i = pathLayer.getFeatures().iterator();
                GeoJsonFeature feature;
                Boolean cnt = true;
                while (cnt) {
                    feature = (GeoJsonFeature)i.next();
                    if (feature.hasProperty("id")) {
                        if (feature.getProperty("id").equals(markerId)) { // The clicked marker had a matching id
                            if (updateTag) { // ID did not come from marker tag -> put it there
                                clickedMarker.setTag(markerId);
                                clickedMarker.setSnippet(tmpSnip); // Id moved to marker tag, reduce snippet to end-user content or null
                            }
                            clickedLegFeature = feature;
                            pathEditButtonVisibility = View.VISIBLE;
                            cnt = false;
                        }
                    }
                    if (!i.hasNext()) { // Marker didn't match with a path feature
                        cnt = false;
                    }
                }
            }
        }
        mPathEdit.setVisibility(pathEditButtonVisibility);

        // Return false to indicate that we have not consumed the event and that we wish
        // for the default behavior to occur (which is for the camera to move such that the
        // marker is centered and for the marker's info window to open, if it has one).
        return false;
    }

    public void pathEditClick(View view) { // Path edit button pressed
        Optional<String> token = mStorage.readSessionToken();
        if (!token.isPresent()) {
            Toast.makeText(this, R.string.not_signed_in, Toast.LENGTH_SHORT).show();
        } else {
            new PathEditDialog(this, token.get(), mStorage.getServerName(), mActivityPathConverter, clickedLegFeature).show(); // edit the path
        }
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        TrafficSenseApplication.refreshStadi();
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onPause()
    {
        super.onPause();

        if (mMap != null) { // Updating these prefs could probably work also in onDestroy? Not tested yet
            mPrefEditor.putFloat(SharedPrefs.KEY_MAP_LAT, (float)mMap.getCameraPosition().target.latitude);
            mPrefEditor.putFloat(SharedPrefs.KEY_MAP_LNG, (float)mMap.getCameraPosition().target.longitude);
            mPrefEditor.putFloat(SharedPrefs.KEY_MAP_ZOOM, mMap.getCameraPosition().zoom);
        }
        mPrefEditor.putInt(SharedPrefs.KEY_PATH_YEAR, pathCal.get(Calendar.YEAR));
        mPrefEditor.putInt(SharedPrefs.KEY_PATH_MONTH, pathCal.get(Calendar.MONTH));
        mPrefEditor.putInt(SharedPrefs.KEY_PATH_DAY, pathCal.get(Calendar.DAY_OF_MONTH));
        mPrefEditor.commit();

        BroadcastHelper.broadcastViewResumed(mLocalBroadcastManager, false);
        mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
    }

    private boolean getSharedBoolean(String key) {
        return mPref.getBoolean(key, false);
    }

    private boolean flipSharedBoolean(String key) {
        boolean b = !mPref.getBoolean(key, false);
        // b = !b; //MJR: simplified, check that there are no adverse impacts!
        mPrefEditor.putBoolean(key, b);
        mPrefEditor.commit();
        return b;
    }

    /********************************
     *
     * Runtime permission checking
     *
     ********************************/

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Permission persistently refused by the user
                Toast.makeText(this, mRes.getString(R.string.location_permission_persistently_refused), Toast.LENGTH_LONG).show();
                setServiceRunning(false);
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_ACCESS_FINE_LOCATION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Timber.d("ACCESS_FINE_LOCATION permission granted.");
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, mRes.getString(R.string.location_permission_persistently_refused), Toast.LENGTH_LONG).show();
                    setServiceRunning(false);
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    /********************************
     *
     * Process options (toolbar) menu
     *
     ********************************/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main_toolbar, menu);
        mPathItem = menu.findItem(R.id.main_toolbar_path);
        mDestItem = menu.findItem(R.id.main_toolbar_dest);
        mTrafficItem = menu.findItem(R.id.main_toolbar_traffic);
        if (getSharedBoolean(SharedPrefs.KEY_SHOW_TRAFFIC)) mTrafficItem.setIcon(R.drawable.ic_traffic_24dp_on);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        // Handle the nav item:
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        int id = item.getItemId();

        switch (id) {
            case R.id.main_toolbar_dest:
                if (!getSharedBoolean(SharedPrefs.KEY_SHOW_DEST)) {
                    fetchDest();
                } else {
                    setDestOff();
                }
                return true;
            case R.id.main_toolbar_path:
                if (!getSharedBoolean(SharedPrefs.KEY_SHOW_PATH)) {
                    DialogFragment newFragment = new DatePickerFragment();
                    newFragment.show(getSupportFragmentManager(), "datePicker");
                } else {
                    setPathOff();
                }
                return true;
            case R.id.main_toolbar_traffic:
                Boolean b = flipSharedBoolean(SharedPrefs.KEY_SHOW_TRAFFIC);
                mMap.setTrafficEnabled(b);
                mTrafficItem.setIcon(b ? R.drawable.ic_traffic_24dp_on : R.drawable.ic_traffic_24dp_off);
                mTrafficItem.setChecked(b);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static class DatePickerFragment extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use pathCal as the default date in the picker
            int year = pathCal.get(Calendar.YEAR);
            int month = pathCal.get(Calendar.MONTH);
            int day = pathCal.get(Calendar.DAY_OF_MONTH);

            // Create a new instance of DatePickerDialog and return it
            DatePickerDialog dpd = new DatePickerDialog(getActivity(), (DatePickerDialog.OnDateSetListener) getActivity(), year, month, day);
            DatePicker dp = dpd.getDatePicker();
            dp.setMaxDate(Calendar.getInstance().getTimeInMillis());
            return dpd;
        }
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int day) {
        // Only query for current or past dates
        pathCal = Calendar.getInstance();
        pathCal.set(Calendar.YEAR, year);
        pathCal.set(Calendar.MONTH, month);
        pathCal.set(Calendar.DAY_OF_MONTH, day);
        if (pathCal.after(Calendar.getInstance())) {
            setPreviousMatchingWeekday(); // Max date set to today = should no longer happen
        }
        updateMapBoundsPath = true;
        fetchPath();
    }

    private void setPreviousMatchingWeekday() {
        int req = pathCal.get(Calendar.DAY_OF_WEEK);
        int curr = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        pathCal = Calendar.getInstance();
        if (curr>req) pathCal.add(Calendar.DATE, req-curr);
        if (curr==req) pathCal.add(Calendar.DATE, -7);
        if (curr<req) pathCal.add(Calendar.DATE, req-curr-7);
        Timber.d("PathCal set to date: " + mDateFormat.format(pathCal.getTime()) + " Day of week: " + pathCal.get(Calendar.DAY_OF_WEEK));
    }

    /*******************************
     *
     * Process drawer menu
     *
     *******************************/

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        switch (id) {
            case R.id.nav_about:
                openActivity(AboutActivity.class);
                break;
            case R.id.nav_survey:
                String surveyUriStr = mServerNotification.getString(ServerNotification.KEY_NOTIFICATION_URI, "");
                if (!surveyUriStr.isEmpty()) {
                    launchBrowser(EnvInfo.replaceUriFields(surveyUriStr));
                }
                break;
            case R.id.nav_energy:
                openActivity(EnergyCertificateActivity.class);
                break;
            case R.id.nav_transport:
                openRegisterTransportForm();
                break;
            case R.id.nav_feedback:
                launchBrowser(EnvInfo.replaceUriFields(mRes.getString(R.string.feedback_form_address)));
                break;
            case R.id.nav_lang_default:
                selectStadi(false);
                break;
            case R.id.nav_lang_stadi:
                selectStadi(true);
                break;
            case R.id.nav_login:
                openActivity(LoginActivity.class);
                break;
            case R.id.nav_settings:
                // Even the menu item should currently be invisible
                Toast.makeText(this, "Not implemented yet.", Toast.LENGTH_LONG).show();
                break;
            case R.id.nav_shutdown:
                setServiceRunning(false);
                break;
            case R.id.nav_startup:
                setServiceRunning(true);
                break;
            case R.id.nav_debug:
                openActivity(DebugActivity.class);
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void openActivity(Class c) {
        Intent intent = new Intent(this, c);
        startActivity(intent);
    }

    private void selectStadi(boolean stadi) {
        TrafficSenseApplication.setStadi(stadi);
        if (stadi) {
            Toast.makeText(this, R.string.lang_select_stadi, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, R.string.lang_select_default, Toast.LENGTH_LONG).show();
        }
        this.recreate();
    }

    private void setDrawerLanguage(boolean stadi) {
        mLangStadiItem.setVisible(!stadi);
        mLangDefaultItem.setVisible(stadi);
    }

    // Carry out all service start/stop request tasks
    private void setServiceRunning(boolean running) {
        serviceRunningToDrawer(running);
        if (running) {
            BroadcastHelper.simpleBroadcast(mLocalBroadcastManager, InternalBroadcasts.KEY_SERVICE_START);
        } else {
            BroadcastHelper.simpleBroadcast(mLocalBroadcastManager, InternalBroadcasts.KEY_SERVICE_STOP);
        }
        if (mMarker != null) mMarker.setVisible(running);
        SharedPreferences.Editor edit = mSettings.edit();
        edit.putBoolean(getResources().getString(R.string.debug_settings_service_running_key), running);
        edit.apply();
    }

    // Align drawer menu items with current service state
    private void serviceRunningToDrawer(boolean running) {
        mShutdownItem.setVisible(running);
        mStartupItem.setVisible(!running);
        if (running) {
            mServiceOffLayout.setVisibility(View.GONE);
        } else {
            mServiceOffLayout.setVisibility(View.VISIBLE);

        }
    }

    private void openRegisterTransportForm() {
        String uriString = mRes.getString(R.string.transport_form_address);
        uriString = uriString.replace("client_number", EnvInfo.getClientNumberString());
        launchBrowser(uriString);
    }

    private void launchBrowser(String us) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(us));
        startActivity(browserIntent);
    }

    /*************************
     *
     * Send email
     *
     * onClick of project title and email fields in the main drawer
     *************************/

    public void sendEmail(View view) {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }
        final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);

        emailIntent.setType("plain/text");
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{getString(R.string.navigation_header_email)});
        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.email_subject));
//        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Text");

        startActivity(Intent.createChooser(emailIntent, getString(R.string.email_startactivity_text)));
    }

    /*************************
     *
     * Open TrafficSense web
     *
     * onClick of project logo in main drawer
     *************************/

    public void openTrafficSenseWeb(View view) {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }
        launchBrowser(getString(R.string.trafficsense_web_page));
    }

    /*************************
     *
     * Broadcast handler
     *
     *************************/

    /* Local broadcast receiver */
    private void initBroadcastReceiver() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                switch (action) {
                    case InternalBroadcasts.KEY_REQUEST_SIGN_IN:
                        openActivity(LoginActivity.class);
                        break;
                    case InternalBroadcasts.KEY_LOCATION_UPDATE:
                        updateLocation(intent);
                        break;
                    case InternalBroadcasts.KEY_ACTIVITY_UPDATE:
                        updateActivity(intent);
                        break;
                    case InternalBroadcasts.KEY_SENSORS_UPDATE:
                        updateLocation(intent);
                        updateActivity(intent);
                        break;
                    case InternalBroadcasts.KEY_SERVICE_STATE_UPDATE:
                        updateServiceState (intent);
                        break;
                    case InternalBroadcasts.KEY_REQUEST_PATH_UPDATE:
                        if (pathLayer!=null) {
                            pathLayer.removeLayerFromMap();
                            pathLayer = null;
                        }
                        updateMapBoundsPath = false;
                        fetchPath();
                        break;
                }
            }
        };

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(InternalBroadcasts.KEY_REQUEST_SIGN_IN);
        intentFilter.addAction(InternalBroadcasts.KEY_LOCATION_UPDATE);
        intentFilter.addAction(InternalBroadcasts.KEY_ACTIVITY_UPDATE);
        intentFilter.addAction(InternalBroadcasts.KEY_SENSORS_UPDATE);
        intentFilter.addAction(InternalBroadcasts.KEY_SERVICE_STATE_UPDATE);
        intentFilter.addAction(InternalBroadcasts.KEY_REQUEST_PATH_UPDATE);

        if (mLocalBroadcastManager != null) {
            mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, intentFilter);
        }
    }

    // MJR: The following two methods circumvent a bug in Lollipop, which is causing the
    // Application to crash when drawing vector icons.
    // Original marker add was: mMarker = mMap.addMarker(new MarkerOptions().position(myPos).icon(BitmapDescriptorFactory.fromResource(ActivityType.getMapActivityIcon(topActivity)));
    // Same issue found in activity change.

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static Bitmap getBitmap(VectorDrawable vectorDrawable) {
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);
        return bitmap;
    }

    private static Bitmap getBitmap(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        if (drawable instanceof BitmapDrawable) {
            return BitmapFactory.decodeResource(context.getResources(), drawableId);
        } else if (drawable instanceof VectorDrawable) {
            return getBitmap((VectorDrawable) drawable);
        } else {
            throw new IllegalArgumentException("unsupported drawable type");
        }
    }

    private void updateLocation (Intent i) {
        if (i.hasExtra(InternalBroadcasts.KEY_LOCATION_UPDATE) & mMap != null) {
            Location l = i.getParcelableExtra(InternalBroadcasts.KEY_LOCATION_UPDATE);
            // Timber.d("Location came back as:" + l.toString());
            if (l != null) {
                LatLng nextPosition = new LatLng(l.getLatitude(), l.getLongitude());
                if (getSharedBoolean(SharedPrefs.KEY_SHOW_PATH)) {
                    if (isTodaysPath() && latestPosition!=null) {
                        // Requesting today's path, have both previous and new position --> let's draw a line!
                        addLine(latestPosition, nextPosition, ActivityType.getActivityColor(latestActivityType));
                    }
                }
                latestPosition = nextPosition;
                if (mMarker == null) {
                    Bitmap bitmap = getBitmap(mContext, ActivityType.getMapActivityIcon(latestActivityType));
                    mMarker = mMap.addMarker(new MarkerOptions()
                            .position(latestPosition)
                            .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                            .title(ActivityType.getActivityString(latestActivityType))
                            .snippet(mTimeFormat.format(new Date())));
                } else {
                    mMarker.setPosition(latestPosition);
                }
                if (l.getAccuracy() > (float) mSettings.getInt(mRes.getString(R.string.debug_settings_location_accuracy_key), 50)) {
                    if (mCircle == null) {
                        CircleOptions circleOptions = new CircleOptions()
                                .center(latestPosition)
                                .radius(l.getAccuracy()).strokeColor(Color.BLUE)
                                .strokeWidth(1.0f);
                        mCircle = mMap.addCircle(circleOptions);
                    } else {
                        mCircle.setRadius(l.getAccuracy());
                        mCircle.setCenter(latestPosition);
                    }
                } else { // Accuracy <= 50.0 - no circle
                    if (mCircle != null) {
                        mCircle.remove();
                        mCircle = null;
                    }
                }
                // Only adjust camera position when the path is not shown or path is for today
                if (!mPref.getBoolean(SharedPrefs.KEY_SHOW_PATH, false) || isTodaysPath()) {
                    if (mBounds == null) {
                        //This is the current user-viewable region of the map
                        mBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
                    }
                    if (!mBounds.contains(latestPosition)) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(latestPosition));
                        mBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
                    }
                }
            }
        }
    }

    private void updateActivity (Intent i) {
        if (i.hasExtra(InternalBroadcasts.KEY_ACTIVITY_UPDATE)) {
            ActivityData a = i.getParcelableExtra(InternalBroadcasts.KEY_ACTIVITY_UPDATE);
            ActivityType topActivity=a.getFirst().Type;
            if ((mMarker != null) && (topActivity != latestActivityType)) {
                // This one works with everything earlier than Lollipop
                // mMarker.setIcon(BitmapDescriptorFactory.fromResource(ActivityType.getMapActivityIcon(topActivity)));
                Bitmap bitmap = getBitmap(mContext, ActivityType.getMapActivityIcon(topActivity));
                mMarker.setIcon(BitmapDescriptorFactory.fromBitmap(bitmap));
                mMarker.setTitle(ActivityType.getActivityString(latestActivityType));
                mMarker.setSnippet(mTimeFormat.format(new Date()));
                latestActivityType = topActivity;
            }
        }
    }

    private void updateServiceState (Intent i) {
        TSServiceState newState = TSServiceState.values()[i.getIntExtra(LABEL_STATE_INDEX,0)];
        if (newState == TSServiceState.STOPPED) serviceRunningToDrawer(false);
        else serviceRunningToDrawer(true);
    }

    /******************************************
     *
     * Show the destinations of the user on map
     *
     ******************************************/

    public void fetchDest() {
        mDestItem.setEnabled(false);
        Optional<String> token = mStorage.readSessionToken();
        if (!token.isPresent()) {
            Toast.makeText(this, R.string.not_signed_in, Toast.LENGTH_SHORT).show();
            mDestItem.setEnabled(true);
        } else {
            try {
                String lPos = "";
                if (latestPosition!=null) {
                  lPos = "?lat=" + Double.toString(latestPosition.latitude) + "&lng=" + Double.toString(latestPosition.longitude);
                }
                // Additional parameters:
                // If "lat" or "lng" is empty, results are not filtered. "lat" or "lng" missing, use last position in device_data to filter destinations (too near + too far). "lat" and "lng" given, filter based on those.
                // "limit=" specified as: <Empty> ("limit=") returns all destinations, <No limit-parameter> returns 5 destinations, <Number> ("limit=NUM") returns NUM destinations. If NUM is negative, NUM last destinations are omitted.
                URL url = new URL(mStorage.getServerName() + "/destinations/" + token.get() + lPos);
                DownloadDestTask downloader = new DownloadDestTask();
                downloader.execute(url);
            } catch (MalformedURLException e) {
                Toast.makeText(this, R.string.dest_url_broken, Toast.LENGTH_SHORT).show();
                mDestItem.setEnabled(true);
            }
        }
    }

    private class DownloadDestTask extends AsyncTask<URL, Void, JSONObject> {
        protected JSONObject doInBackground(URL... urls) {
            String geoJsonString = null;
            if (urls.length != 1) {
                Timber.e("Path downloader attempted to get more or less than one URL");
                mDestItem.setEnabled(true);
                return null;
            }

            HttpURLConnection urlConnection = null;
            try {
                Timber.d("Opening with URL: " + urls[0].toString());
                urlConnection = (HttpURLConnection) urls[0].openConnection();

                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                java.util.Scanner s = new java.util.Scanner(in).useDelimiter("\\A");
                if (s.hasNext()) {
                    geoJsonString = s.next();
                } else {
                    Timber.e("DownloadDestTask received no data!");
                }
                in.close();
            } catch (IOException e) {
                Timber.e("DownloadDestTask error connecting to URL.");
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

            JSONObject geoJson = null;
            if (geoJsonString != null) {
                try {
                    geoJson = new JSONObject(geoJsonString);
                    JSONArray ga = geoJson.getJSONArray("features");
                    if (ga.length() > 0) mPrefEditor.putString(SharedPrefs.KEY_DEST_OBJECT, geoJsonString);
                    else mPrefEditor.putString(SharedPrefs.KEY_DEST_OBJECT, null);
                    mPrefEditor.commit();
                } catch (JSONException e) {
                    Timber.e("Destination GeoJson conversion returned an exception: %s", e.toString());
                    mPathItem.setEnabled(true);
                    return null;
                }
            }
            return geoJson;
        }

        protected void onPostExecute(JSONObject geoJson) {
//            Timber.d("DownloadDestTask Received a string of length: " + info.length());
//            Timber.d("First 200 characters: \n" + info.substring(0,min(200,info.length())));
            if (geoJson != null) {
                processDest(geoJson);
            }
            mDestItem.setEnabled(true);
        }
    }

    /**
     * Processes all destinations for viewing
     */
    private void processDest(JSONObject geoJson) {
        MapBounds mBuildBounds = new MapBounds();
        JSONArray features = null;
        int l = -1;
        try {
            features = geoJson.getJSONArray("features");
            l = features.length();
        } catch (JSONException e) {
            Timber.e("Destination features array conversion threw a JSONException: %s", e.toString());
        }
        if (l>0) {
            if (l>DEST_ON_MAP) l=DEST_ON_MAP;
            JSONObject feature;
            JSONArray lngLat;
            LatLng markerPos = null;
            int visits = 0;
            // float[] distResults = new float[5];
            boolean useMarker;
            destMarkers.clear();
            int i = 0;
            int iconIndex = 1;
            do {
                useMarker = true;
                try {
                    feature = features.getJSONObject(i);
                    lngLat = feature.getJSONObject("geometry").getJSONArray("coordinates");
                    markerPos = new LatLng(lngLat.getDouble(1),lngLat.getDouble(0));
                    // Timber.d("Received position: %s", markerPos.toString());
                    visits = feature.getJSONObject("properties").getInt("visits");
                } catch (JSONException e) {
                    Timber.e("Destination feature object extraction threw a JSONException: %s", e.toString());
                    useMarker = false;
                }
                // Commenting out the following filter - to be done by the server
//                if ((latestPosition != null) && (markerPos != null)) {
//                    Location.distanceBetween(latestPosition.latitude,
//                            latestPosition.longitude,
//                            markerPos.latitude,
//                            markerPos.longitude,
//                            distResults);
//                    // Skip markers too close and too far
//                    if (distResults[0]<minDistToDest || distResults[0]>maxDistToDest) useMarker=false;
//                }

                // Test code for the emulator with Mikko's coordinates - comment out!!
                /*
                if (markerPos != null) {
                    Location.distanceBetween(60.19067498360351,
                            24.764974265193345,
                            markerPos.latitude,
                            markerPos.longitude,
                            distResults);
                    if (distResults[0]<200.0f) {
                        Timber.d("Destination %d matched with current test location.", i+1);
                        useMarker=false;
                    }
                } */

                if (useMarker) {
                    mBuildBounds.include(markerPos);
                    if (mMap != null) {
                        destMarkers.add(mMap.addMarker(new MarkerOptions()
                                .position(markerPos)
                                .title(this.getString(R.string.dest_visits) + ": " + visits)
                                .icon(destIcon(iconIndex))));
                        iconIndex++;
                    }
                } else { // Skipping this marker - we are too close
                    if (features.length() > l) l++;
                }
                i++;
            } while (i < l);
            if (latestPosition!=null) {
                mBuildBounds.include(latestPosition);
            }
            setDestOn();
            mBuildBounds.update(mMap);
        } else { // !l>0 -> no destinations
            Toast.makeText(this, R.string.dest_no_data, Toast.LENGTH_SHORT).show();
        }
    }

    private void setDestOn() {
        mPrefEditor.putBoolean(SharedPrefs.KEY_SHOW_DEST, true);
        mPrefEditor.commit();
        mDestItem.setIcon(R.drawable.ic_dest_on);
        mDestItem.setChecked(true);
    }

    private void setDestOff() {
        mPrefEditor.putBoolean(SharedPrefs.KEY_SHOW_DEST, false);
        mPrefEditor.putString(SharedPrefs.KEY_DEST_OBJECT, null);
        mPrefEditor.commit();
        mDestItem.setIcon(R.drawable.ic_dest_off);
        mDestItem.setChecked(false);
        /* Remove destinations from map */
        if (destMarkers.size() > 0) {
            for (Marker m: destMarkers) {
                m.remove();
            }
            destMarkers.clear();
        }
    }

    private BitmapDescriptor destIcon(int destNr) {
        int res = 0;
        switch (destNr) {
            case 1:
                res = R.drawable.map_dest1;
                break;
            case 2:
                res = R.drawable.map_dest2;
                break;
            case 3:
                res = R.drawable.map_dest3;
                break;
            case 4:
                res = R.drawable.map_dest4;
                break;
            case 5:
                res = R.drawable.map_dest5;
                break;
            default:
                res = R.mipmap.ic_launcher;
        }

        return BitmapDescriptorFactory.fromResource(res);
    }

    /**********************************
     *
     * Show the path of the user on map
     *
     **********************************/

    public void pathDateClick(View view) { // Shortcut to change date
        setPathOff();
        DialogFragment newFragment = new DatePickerFragment();
        newFragment.show(getSupportFragmentManager(), "datePicker");
    }

    public void fetchPath() {
        mPathItem.setEnabled(false);
        String pathDate = mDateFormat.format(pathCal.getTime());

        Optional<String> token = mStorage.readSessionToken();
        if (!token.isPresent()) {
            Toast.makeText(this, R.string.not_signed_in, Toast.LENGTH_SHORT).show();
            mPathItem.setEnabled(true);
        } else {
            try {
                // Additional parameters
                // "maxpts" = maximum number of points accepted for a single feature
                // "mindist" = minimum distance between points
                // Example: "&maxpts=20000&mindist=20"
                URL url = new URL(mStorage.getServerName() + "/path/" + token.get() + "?date=" + pathDate);
                DownloadPathTask downloader = new DownloadPathTask();
                downloader.execute(url);
            } catch (MalformedURLException e) {
                Toast.makeText(this, R.string.path_url_broken, Toast.LENGTH_SHORT).show();
                mPathItem.setEnabled(true);
            }
        }
    }


    private class DownloadPathTask extends AsyncTask<URL, Void, JSONObject> {
        protected JSONObject doInBackground(URL... urls) {
            String geoJsonString = null;
            if (urls.length != 1) {
                Timber.e("Path downloader attempted to get more or less than one URL");
                mPathItem.setEnabled(true);
                return null;
            }

            HttpURLConnection urlConnection = null;
            try {
                Timber.d("Opening with URL: %s", urls[0].toString());
                urlConnection = (HttpURLConnection) urls[0].openConnection();

                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                java.util.Scanner s = new java.util.Scanner(in).useDelimiter("\\A");
                if (s.hasNext()) {
                    geoJsonString = s.next();
                }
                else {
                    Timber.e("DownloadPathTask received no data!");
                }
                in.close();
            }
            catch (IOException e) {
                Timber.e("DownloadPathTask error connecting to URL.");
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

            JSONObject geoJson = null;
            if (geoJsonString != null) {
                try {
                    geoJson = new JSONObject(geoJsonString);
                    JSONArray ga = geoJson.getJSONArray("features");
                    int i = ga.length();
                    boolean cachePath = false; // reset unless a non-zero path from a past day
                    if (isTodaysPath()) {
                        if (i>0) {
                            // Dig out the latest coordinates of today.
                            JSONArray coord = ga.getJSONObject(i-1).getJSONObject("geometry").getJSONArray("coordinates");
                            JSONArray lngLat = coord.getJSONArray(coord.length()-1);
                            pathEnd = new LatLng(lngLat.getDouble(1),lngLat.getDouble(0));
                        } else pathEnd=null;
                    } else { // Not today
                        if (i>0) {
                            cachePath = true;  // save a copy for screen redraws
                        }
                    }
                    if (cachePath) mPrefEditor.putString(SharedPrefs.KEY_PATH_OBJECT, geoJsonString);
                    else mPrefEditor.putString(SharedPrefs.KEY_PATH_OBJECT, null);
                    mPrefEditor.commit();
                } catch (JSONException e) {
                    Timber.e("Path GeoJson conversion returned an exception: " + e.toString());
                    mPathItem.setEnabled(true);
                    return null;
                }
            }
            return geoJson;
        }

        protected void onPostExecute(JSONObject geoJson) {
//            Timber.d("DownloadPathTask Received a string of length: " + info.length());
//            Timber.d("First 200 characters: \n" + info.substring(0,min(200,info.length())));
            if (geoJson != null) {
                int l = -1;
                try {
                    l = geoJson.getJSONArray("features").length();
                } catch (JSONException e) {
                    Timber.e("JSONException: " + e.toString());
                }
                if (l>0) { // received one or more lines
                    pathToMap(geoJson);
                } else {
                    if (isTodaysPath()) {
                        setPathOn(); // Today can be on even without content
                    } else {
                        // No content, not today
                        Toast.makeText(mContext, R.string.path_no_data_for_date, Toast.LENGTH_SHORT).show();
                    }
                }
//                if (isTodaysPath()) {
//                    Toast.makeText(mContext, R.string.path_no_public_transport, Toast.LENGTH_SHORT).show();
//                }
            }
            mPathItem.setEnabled(true);
        }
    }

    private boolean isTodaysPath() {
        return mDateFormat.format(pathCal.getTime()).equals(mDateFormat.format(Calendar.getInstance().getTime()));
    }

    private void setPathOn() {
        mPrefEditor.putBoolean(SharedPrefs.KEY_SHOW_PATH, true);
        mPrefEditor.commit();
        mPathItem.setIcon(R.drawable.road_variant_on);
        mPathItem.setChecked(true);
        if (isTodaysPath()) mPathDate.setText(R.string.today);
        else mPathDate.setText(DateFormat.getDateInstance().format(pathCal.getTime()));
        mPathEdit.setVisibility(View.INVISIBLE);
        mPathDateLayout.setVisibility(View.VISIBLE);
    }

    private void setPathOff() {
        mPrefEditor.putBoolean(SharedPrefs.KEY_SHOW_PATH, false);
        mPrefEditor.putString(SharedPrefs.KEY_PATH_OBJECT, null);
        mPrefEditor.commit();
        mPathItem.setIcon(R.drawable.road_variant_off);
        mPathItem.setChecked(false);
        mPathEdit.setVisibility(View.INVISIBLE);
        mPathDateLayout.setVisibility(View.GONE);
        if (pathLayer!=null) {
            pathLayer.removeLayerFromMap();
            pathLayer = null;
        }
    }

    /**
     * Adds the appropriate color to each linestring based on the activity
     * Construct new bounds for the window
     * Add icons for identified public transport
     */
    private void processPath() {
        Boolean today = isTodaysPath(); // optimise a bit = no lookup for every feature
        MapBounds mBuildBounds = new MapBounds();
        List<LatLng> coordinates;
        Set<GeoJsonFeature> newFeatures = new HashSet<>();
        for (GeoJsonFeature feature : pathLayer.getFeatures()) {
            coordinates = null;
            if (feature.hasGeometry()) {
                coordinates = ((GeoJsonLineString) feature.getGeometry()).getCoordinates();
            }
            // Check if the activity property exists
            if (feature.hasProperty("activity")) {
                String activity = feature.getProperty("activity");
                GeoJsonLineStringStyle mStyle = new GeoJsonLineStringStyle();
                mStyle.setColor(ContextCompat.getColor(mContext, mActivityPathConverter.getColor(activity)));
                feature.setLineStringStyle(mStyle);
                String idProperty = "null";
                if (feature.hasProperty("id")) {
                    idProperty = feature.getProperty("id");
                }
//                if (!today) { // Uncomment to remove markers and disable editing for current day
                    if ((!idProperty.equals("null")) && editableActivities.contains(activity)) { // Add markers and ID:s only for those activities that can be edited
                        if (coordinates != null) {
                            // Find mid-coordinates of the trip
                            LatLng pos = coordinates.get(coordinates.size()/2);
                            GeoJsonFeature transportIconFeature = new GeoJsonFeature(new GeoJsonPoint(pos), null, null, null);
                            GeoJsonPointStyle pointStyle = new GeoJsonPointStyle();
                            transportIconFeature.setProperty("activity", activity);
                            // title to marker
                            StringBuilder title = new StringBuilder();
                            title.append(mRes.getString(mActivityPathConverter.getLocalizedNameRes(activity)));
                            if (feature.hasProperty("line_name")) {
                                String lineName = feature.getProperty("line_name");
                                if (!lineName.equals("null")) {
                                    title.append(": ").append(lineName);
                                }
                                transportIconFeature.setProperty("line_name", lineName);
                            }
                            pointStyle.setTitle(title.toString());
                            // id to marker
                            transportIconFeature.setProperty("id", idProperty);
                            StringBuilder snip = new StringBuilder(idProperty);
                            snip.append(snipSeparator); // Snippet separator between id and time
                            // trip start - end times to marker
                            if (feature.hasProperty("time_start")) {
                                String tripStart = feature.getProperty("time_start");
                                if (!tripStart.equals("null")) {
                                    snip.append(tripStart.substring(11,16)); // Parse from "2016-01-17 12:43:54.837" --> HH:MM
                                }
                            }
                            if (feature.hasProperty("time_end")) {
                                String tripEnd = feature.getProperty("time_end");
                                if (!tripEnd.equals("null")) {
                                    snip.append("-").append(tripEnd.substring(11,16));
                                }
                            }
                            pointStyle.setSnippet(snip.toString());
                            Bitmap bitmap = getBitmap(mContext, mActivityPathConverter.getIcon(activity));
                            pointStyle.setIcon(BitmapDescriptorFactory.fromBitmap(bitmap));
                            transportIconFeature.setPointStyle(pointStyle);
                            newFeatures.add(transportIconFeature);
                        }
                    }
//                }
            }
            // Check that our route is included in the bounds
            if (updateMapBoundsPath && (coordinates != null)) {
                for (LatLng pos : coordinates) {
                    mBuildBounds.include(pos);
                }
            }
        } // for-loop
        if (!newFeatures.isEmpty()) { // Add any route icons we may have accumulated
            for (GeoJsonFeature feature : newFeatures) {
                pathLayer.addFeature(feature);
            }
        }
        if (today && latestPosition!=null) {
            mBuildBounds.include(latestPosition);
            // Quick-n-dirty solution to bypass all the queued points with one line
            if (pathEnd!=null) addLine(pathEnd, latestPosition, ActivityType.getActivityColor(latestActivityType));
        }
        if (updateMapBoundsPath) mBuildBounds.update(mMap);
    }

    private void addLine(LatLng start, LatLng end, int color) {
        if (pathLayer==null) { // This happens when today's path was selected with no data on the server
            try { // generate an empty geojsonlayer
                // Timber.d("Generating a one-line geojsonlayer");
                JSONObject firstLine = new JSONObject(String.format("{ \"features\": [ { \"geometry\": { \"coordinates\": [ [%f,%f], [%f,%f] ], \"type\": \"LineString\" }, \"properties\": { \"type\": \"line\" }, \"type\": \"Feature\" } ], \"type\": \"FeatureCollection\" }", start.longitude, start.latitude, end.longitude, end.latitude));
                pathLayer = new GeoJsonLayer(mMap, firstLine);
                GeoJsonLineStringStyle mStyle = new GeoJsonLineStringStyle();
                mStyle.setColor(ContextCompat.getColor(mContext, color));
                pathLayer.getFeatures().iterator().next().setLineStringStyle(mStyle);
                pathLayer.addLayerToMap();
            }
            catch (JSONException e) {
                Timber.e("Dummy GeoJson conversion returned an exception: " + e.toString());
            }
        } else {
            ArrayList<LatLng> lineStringArray = new ArrayList<>();
            lineStringArray.add(start);
            lineStringArray.add(end);
            GeoJsonLineString lineString = new GeoJsonLineString(lineStringArray);
            GeoJsonFeature lineStringFeature = new GeoJsonFeature(lineString, null, null, null);
            GeoJsonLineStringStyle lineStringStyle = new GeoJsonLineStringStyle();
            lineStringStyle.setColor(ContextCompat.getColor(mContext, color));
            lineStringFeature.setLineStringStyle(lineStringStyle);
            pathLayer.addFeature(lineStringFeature);
        }
    }

    public void pathToMap(JSONObject geoJson) {
            pathLayer = new GeoJsonLayer(mMap, geoJson);
            processPath();
            pathLayer.addLayerToMap();
            setPathOn();
    }

}
