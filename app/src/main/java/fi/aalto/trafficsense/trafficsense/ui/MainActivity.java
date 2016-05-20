package fi.aalto.trafficsense.trafficsense.ui;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.*;
import com.google.common.base.Optional;
import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonLayer;
import com.google.maps.android.geojson.GeoJsonLineString;
import com.google.maps.android.geojson.GeoJsonLineStringStyle;
import fi.aalto.trafficsense.trafficsense.R;
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
import java.util.ArrayList;
import java.util.Calendar;

import static android.support.v7.preference.PreferenceManager.getDefaultSharedPreferences;
import static fi.aalto.trafficsense.trafficsense.util.InternalBroadcasts.LABEL_STATE_INDEX;

public class MainActivity extends AppCompatActivity
        implements  NavigationView.OnNavigationItemSelectedListener,
                    OnMapReadyCallback,
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

    private MenuItem mStartupItem;
    private MenuItem mShutdownItem;
    private MenuItem mDebugItem;
    private MenuItem mPathItem;
    private MenuItem mTrafficItem;
    private TextView mPathDate;
    private FrameLayout mPathDateLayout;
    // private FloatingActionButton mFab;

    private GoogleMap mMap;
    private LatLngBounds mBounds;
    private Marker mMarker=null;
    private Circle mCircle=null;
    private ActivityType latestActivityType=ActivityType.STILL;
    private LatLng latestPosition;
    private LatLng pathEnd;
    private static Calendar pathCal = Calendar.getInstance();
    private final SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private GeoJsonLayer pathLayer=null;

    // Very first view opens in Otaniemi :-)
    private final float initLat = 60.1841396f;
    private final float initLng = 24.8300838f;
    private final float initZoom = 12;

    private final int MY_PERMISSIONS_ACCESS_FINE_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        mRes = this.getResources();
        setContentView(R.layout.activity_main);
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

        mPathDate = (TextView) findViewById(R.id.main_path_date);
        mPathDateLayout = (FrameLayout) findViewById(R.id.main_path_date_layout);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Toast.makeText(this, R.string.error_creating_map, Toast.LENGTH_SHORT).show();
        }

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
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
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
        mDebugItem.setVisible(mSettings.getBoolean(mRes.getString(R.string.debug_settings_debug_mode_key), false));
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
    }

    /**
     * This callback is triggered when the map is really ready.
     */
    @Override
    public void onMapLoaded() {
        if (mMap != null) {

            if (getSharedBoolean(SharedPrefs.KEY_SHOW_PATH)) { // Redraw current path, if displayed.
                String geoJsonString = mPref.getString(SharedPrefs.KEY_PATH_OBJECT, null);
                // Timber.d("GeoJsonString: " + geoJsonString);
                if (geoJsonString == null) {
                    // Path on but no cached path (probably today) - fetch a fresh path from the server
                    fetchPath();
                } else {
                    cachedPath(geoJsonString);
                }
            }

            if (getSharedBoolean(SharedPrefs.KEY_SHOW_TRAFFIC)) {
                mMap.setTrafficEnabled(true);
            }

        }
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
        boolean b = mPref.getBoolean(key, false);
        b = !b;
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
        mTrafficItem = menu.findItem(R.id.main_toolbar_traffic);
//        if (getSharedBoolean(SharedPrefs.KEY_SHOW_PATH)) mPathItem.setIcon(R.drawable.road_variant_on);
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
            case R.id.main_toolbar_traffic:
                Boolean b = flipSharedBoolean(SharedPrefs.KEY_SHOW_TRAFFIC);
                mMap.setTrafficEnabled(b);
                mTrafficItem.setIcon(b ? R.drawable.ic_traffic_24dp_on : R.drawable.ic_traffic_24dp_off);
                return true;
            case R.id.main_toolbar_path:
                if (!getSharedBoolean(SharedPrefs.KEY_SHOW_PATH)) {
                    DialogFragment newFragment = new DatePickerFragment();
                    newFragment.show(getSupportFragmentManager(), "datePicker");
                } else {
                    setPathOff();
                }
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
            return new DatePickerDialog(getActivity(), (DatePickerDialog.OnDateSetListener) getActivity(), year, month, day);
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
//            Toast.makeText(this, R.string.path_future_date_request, Toast.LENGTH_LONG).show();
//        } else {
            setPreviousMatchingWeekday(); // A little joke for the young at heart
        }
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
            case R.id.nav_energy:
                openActivity(EnergyCertificateActivity.class);
                break;
            case R.id.nav_transport:
                openRegisterTransportForm();
                break;
            case R.id.nav_feedback:
                openFeedbackForm();
                break;
            case R.id.nav_login:
                openActivity(LoginActivity.class);
                break;
            case R.id.nav_settings:
                openActivity(SettingsActivity.class);
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
        if (running) {
            mShutdownItem.setVisible(true);
            mStartupItem.setVisible(false);
        } else {
            mStartupItem.setVisible(true);
            mShutdownItem.setVisible(false);
        }
    }

    private void openFeedbackForm() {
        String uriString = mRes.getString(R.string.feedback_form_address);

        uriString = uriString.replace("client_number", getClientNumberString());
        String clientVersionString = "";
        try {
            clientVersionString = getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            clientVersionString = mRes.getString(R.string.not_available);
        }
        uriString = uriString.replace("client_version", clientVersionString);
        uriString = uriString.replace("phone_model", Build.MODEL);
        launchBrowser(uriString);
    }

    private void openRegisterTransportForm() {
        String uriString = mRes.getString(R.string.transport_form_address);
        uriString = uriString.replace("client_number", getClientNumberString());
        launchBrowser(uriString);
    }

    private String getClientNumberString() {
        String clientNumberString;
        if (mStorage.isClientNumberAvailable()) {
            clientNumberString = String.format("%d", mStorage.readClientNumber().get());
        } else {
            clientNumberString = mRes.getString(R.string.not_available);
        }
        return clientNumberString;
    }

    private void launchBrowser(String us) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(us));
        startActivity(browserIntent);
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
                }
            }
        };

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(InternalBroadcasts.KEY_REQUEST_SIGN_IN);
        intentFilter.addAction(InternalBroadcasts.KEY_LOCATION_UPDATE);
        intentFilter.addAction(InternalBroadcasts.KEY_ACTIVITY_UPDATE);
        intentFilter.addAction(InternalBroadcasts.KEY_SENSORS_UPDATE);
        intentFilter.addAction(InternalBroadcasts.KEY_SERVICE_STATE_UPDATE);

        if (mLocalBroadcastManager != null) {
            mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, intentFilter);
        }
    }

    // MJR: The following two methods circumvent a bug in Lollipop, which is causing the
    // Application to crash when drawing vector icons.
    // Original marker add was: mMarker = mMap.addMarker(new MarkerOptions().position(myPos).icon(BitmapDescriptorFactory.fromResource(ActivityType.getActivityIcon(topActivity)));
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
                    Bitmap bitmap = getBitmap(mContext, ActivityType.getActivityIcon(latestActivityType));
                    mMarker = mMap.addMarker(new MarkerOptions()
                            .position(latestPosition)
                            .icon(BitmapDescriptorFactory.fromBitmap(bitmap)));
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
                // Only adjust camera position when the path is not shown
                if (!mPref.getBoolean(SharedPrefs.KEY_SHOW_PATH, false)) {
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
                // mMarker.setIcon(BitmapDescriptorFactory.fromResource(ActivityType.getActivityIcon(topActivity)));
                Bitmap bitmap = getBitmap(mContext, ActivityType.getActivityIcon(topActivity));
                mMarker.setIcon(BitmapDescriptorFactory.fromBitmap(bitmap));
                latestActivityType = topActivity;
            }
        }
    }

    private void updateServiceState (Intent i) {
        TSServiceState newState = TSServiceState.values()[i.getIntExtra(LABEL_STATE_INDEX,0)];
        if (newState == TSServiceState.STOPPED) serviceRunningToDrawer(false);
        else serviceRunningToDrawer(true);
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
                URL url = new URL(mStorage.getServerName().toString() + "/path/" + token.get() + "?date=" + pathDate + "&maxpts=20000&mindist=20");
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
                Timber.d("Opening with URL: " + urls[0].toString());
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
                    pathLayer = new GeoJsonLayer(mMap, geoJson);
                    processPath();
                    pathLayer.addLayerToMap();
                    setPathOn();
                } else {
                    if (isTodaysPath()) {
                        setPathOn(); // Today can be on even without content
                    } else {
                        // No content - uncheck the menu item
                        Toast.makeText(mContext, R.string.path_no_data_for_date, Toast.LENGTH_SHORT).show();
                    }
                }
                if (isTodaysPath()) {
                    Toast.makeText(mContext, R.string.path_no_public_transport, Toast.LENGTH_SHORT).show();
                }
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
        if (isTodaysPath()) mPathDate.setText(R.string.today);
        else mPathDate.setText(DateFormat.getDateInstance().format(pathCal.getTime()));
        mPathDateLayout.setVisibility(View.VISIBLE);
    }

    private void setPathOff() {
        mPrefEditor.putBoolean(SharedPrefs.KEY_SHOW_PATH, false);
        mPrefEditor.putString(SharedPrefs.KEY_PATH_OBJECT, null);
        mPrefEditor.commit();
        mPathItem.setIcon(R.drawable.road_variant_off);
        mPathDateLayout.setVisibility(View.GONE);
        if (pathLayer!=null) {
            pathLayer.removeLayerFromMap();
            pathLayer = null;
        }
    }

    /**
     * Adds the appropriate color to each linestring based on the activity
     * Construct new bounds for the window
     * Modified from "addColorsToMarkers" from: https://github.com/googlemaps/android-maps-utils/blob/master/demo/src/com/google/maps/android/utils/demo/GeoJsonDemoActivity.java
     */
    private void processPath() {
        // Iterate over all the features stored in the layer
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        boolean boundsOk = false; // Only build the new bounds if there is some content
        for (GeoJsonFeature feature : pathLayer.getFeatures()) {
            // Check if the activity property exists
            if (feature.hasProperty("activity")) {
                String activity = feature.getProperty("activity");
                GeoJsonLineStringStyle mStyle = new GeoJsonLineStringStyle();
                mStyle.setColor(ContextCompat.getColor(mContext, getActivityColorByString(activity)));
                feature.setLineStringStyle(mStyle);
            }
            if (feature.hasGeometry()) {
                boundsOk = true; // A LineString will always contain at least two points
                GeoJsonLineString ls = (GeoJsonLineString)feature.getGeometry();
                for (LatLng pos : ls.getCoordinates()) {
                    boundsBuilder.include(pos);
                }
            }
        }
        if (isTodaysPath() && latestPosition!=null) {
            boundsBuilder.include(latestPosition);
            // Quick-n-dirty solution to bypass all the queued points with one line
            if (pathEnd!=null) addLine(pathEnd, latestPosition, ActivityType.getActivityColor(latestActivityType));
        }
        if (boundsOk) mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 20));
    }

    private int getActivityColorByString(String activity) {
        switch(activity) {
            case "IN_VEHICLE":
                return R.color.colorInVehicle;
            case "ON_BICYCLE":
                return R.color.colorOnBicycle;
            case "RUNNING":
                return R.color.colorRunning;
            case "STILL":
                return R.color.colorStill;
            case "TILTING":
                return R.color.colorTilting;
            case "UNKNOWN":
                return R.color.colorUnknown;
            case "WALKING":
                return R.color.colorWalking;
            case "TRAIN":
                return R.color.colorTrain;
            case "TRAM":
            case "SUBWAY":
                return R.color.colorSubway;
            case "BUS":
            case "FERRY":
                return R.color.colorBus;
            default:
                return R.color.colorUnknown;
        }
    }

    private void addLine(LatLng start, LatLng end, int color) {
        if (pathLayer==null) { // This happens when today's path was selected with no data on the server
            try { // generate an empty geojsonlayer
                Timber.d("Generating a one-line geojsonlayer");
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

    public void cachedPath(String geoJsonString) {
        try {
            JSONObject geoJson = new JSONObject(geoJsonString);
            pathLayer = new GeoJsonLayer(mMap, geoJson);
            processPath();
            pathLayer.addLayerToMap();
            setPathOn();
        }
        catch (JSONException e) {
            Timber.e("Cached path GeoJson conversion returned an exception: " + e.toString());
            setPathOff();
        }

    }

}
