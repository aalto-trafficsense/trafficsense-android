package fi.aalto.trafficsense.trafficsense.ui;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
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
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
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
import android.widget.Toast;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import fi.aalto.trafficsense.trafficsense.R;
import fi.aalto.trafficsense.trafficsense.util.*;
import timber.log.Timber;

import static fi.aalto.trafficsense.trafficsense.util.InternalBroadcasts.LABEL_STATE_INDEX;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,OnMapReadyCallback {

    private LocalBroadcastManager mLocalBroadcastManager;
    private BroadcastReceiver mBroadcastReceiver;
    private ActionBarDrawerToggle mDrawerToggle;
    private Context mContext;
    private Resources mRes;
    private BackendStorage mStorage;

    private MenuItem mStartupItem;
    private MenuItem mShutdownItem;
    private FloatingActionButton mFab;
    private GoogleMap mMap;
    private LatLngBounds mBounds;
    private Marker mMarker=null;
    private Circle mCircle=null;
    private ActivityType latestActivityType=ActivityType.STILL;
    private boolean showTraffic=false;
    private boolean mapToolbarEnabled=false;

    private LatLng initPosition=(new LatLng(60.1841396, 24.8300838));
    private float initZoom=12;

    private final int MY_PERMISSIONS_ACCESS_FINE_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        mRes = this.getResources();
        setContentView(R.layout.activity_main);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);

        mFab = (FloatingActionButton) findViewById(R.id.fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Toggle traffic display
                if (mMap!=null) {
                    showTraffic = !showTraffic;
                    mMap.setTrafficEnabled(showTraffic);
                    if (showTraffic) mFab.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(mContext, R.color.colorInVehicle)));
                    else mFab.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(mContext, R.color.white)));
                }
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
            }
        });

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

        // Hide items not implemented yet:
        navMenu.findItem(R.id.nav_settings).setVisible(false);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Toast.makeText(this, "Error - Map Fragment was null!!", Toast.LENGTH_SHORT).show();
        }

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        initBroadcastReceiver();
        mStorage = BackendStorage.create(mContext);
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
        broadcastViewResumed(true);
        BroadcastHelper.simpleBroadcast(mLocalBroadcastManager, InternalBroadcasts.KEY_MAIN_ACTIVITY_REQ);
        checkLocationPermission(); // Check the dynamic location permissions
    }

    @Override
    public void onPause()
    {
        super.onPause();
        broadcastViewResumed(false);
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Permission persistently refused by the user
                Toast.makeText(this, mRes.getString(R.string.location_permission_persistently_refused), Toast.LENGTH_LONG).show();
                requestServiceShutdown();
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
                    requestServiceShutdown();
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void requestServiceShutdown() {
        BroadcastHelper.simpleBroadcast(mLocalBroadcastManager, InternalBroadcasts.KEY_SERVICE_STOP);
        fixServiceStateToMenu(false);
    }

    // MJR: Removing the options menu for now - everything in the drawer
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.main, menu);
//        return true;
//    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        // Handle the nav item:
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

//        int id = item.getItemId();
//
//        switch (id) {
//            case R.id.action_about:
//                openActivity(AboutActivity.class);
//                return true;
//            case R.id.action_exit:
//                finish();
//                return true;
//            case R.id.action_debug:
//                openActivity(DebugActivity.class);
//                return true;
//
//        }

        return super.onOptionsItemSelected(item);
    }

    private void openActivity(Class c) {
        Intent intent = new Intent(this, c);
        startActivity(intent);
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
                requestServiceShutdown();
                break;
            case R.id.nav_startup:
                BroadcastHelper.simpleBroadcast(mLocalBroadcastManager, InternalBroadcasts.KEY_SERVICE_START);
                fixServiceStateToMenu(true);
                break;
            case R.id.nav_debug:
            case R.id.action_debug:
                openActivity(DebugActivity.class);
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void fixServiceStateToMenu(boolean running) {
        if (running) {
            mShutdownItem.setVisible(true);
            mStartupItem.setVisible(false);
            if (mMarker != null) mMarker.setVisible(true);
        } else {
            mStartupItem.setVisible(true);
            mShutdownItem.setVisible(false);
            if (mMarker != null) mMarker.setVisible(false);
        }
    }

    private void openFeedbackForm() {
        String uriString = mRes.getString(R.string.feedback_form_address);
        String clientNumberString = mRes.getString(R.string.not_available);
        if (mStorage.isClientNumberAvailable()) {
            clientNumberString = String.format("%d", mStorage.readClientNumber().get());
        }
        String clientVersionString = "";
        try {
            clientVersionString = getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            clientVersionString = mRes.getString(R.string.not_available);
        }

        String phoneModelString =  Build.MODEL;

        uriString = uriString.replace("client_number", clientNumberString);
        uriString = uriString.replace("client_version", clientVersionString);
        uriString = uriString.replace("phone_model", phoneModelString);

        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriString));
        startActivity(browserIntent);
    }

    private void openRegisterTransportForm() {
        String uriString = mRes.getString(R.string.transport_form_address);
        String clientNumberString = mRes.getString(R.string.not_available);
        if (mStorage.isClientNumberAvailable()) {
            clientNumberString = String.format("%d", mStorage.readClientNumber().get());
        }
        uriString = uriString.replace("client_number", clientNumberString);

        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriString));
        startActivity(browserIntent);
    }



    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

         mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initPosition, initZoom));
    }


    /*************************
     Broadcast handler
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
                LatLng myPos = new LatLng(l.getLatitude(), l.getLongitude());
                if (mMarker == null) {
                    Bitmap bitmap = getBitmap(mContext, ActivityType.getActivityIcon(latestActivityType));
                    mMarker = mMap.addMarker(new MarkerOptions().position(myPos).icon(BitmapDescriptorFactory.fromBitmap(bitmap)));
                } else {
                    mMarker.setPosition(myPos);
                }
                if (l.getAccuracy() > 50.0) {
                    if (mCircle == null) {
                        CircleOptions circleOptions = new CircleOptions()
                                .center(myPos)
                                .radius(l.getAccuracy()).strokeColor(Color.BLUE)
                                .strokeWidth(1.0f);
                        mCircle = mMap.addCircle(circleOptions);
                    } else {
                        mCircle.setRadius(l.getAccuracy());
                        mCircle.setCenter(myPos);
                    }
                } else { // Accuracy <= 50.0 - no circle
                    if (mCircle != null) {
                        mCircle.remove();
                        mCircle = null;
                    }
                }
                if (mBounds == null) {
                    //This is the current user-viewable region of the map
                    mBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
                }
                if (!mBounds.contains(myPos)) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(myPos));
                    mBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
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
        if (newState == TSServiceState.STOPPED) fixServiceStateToMenu(false);
        else fixServiceStateToMenu(true);
    }

    // Update (viewing) activity status to service
    public void broadcastViewResumed(boolean resumed) {
        if (mLocalBroadcastManager != null)
        {
            String key;
            if (resumed) key = InternalBroadcasts.KEY_VIEW_RESUMED;
            else key = InternalBroadcasts.KEY_VIEW_PAUSED;
            Intent intent = new Intent(key);
            mLocalBroadcastManager.sendBroadcast(intent);
        }
    }


}
