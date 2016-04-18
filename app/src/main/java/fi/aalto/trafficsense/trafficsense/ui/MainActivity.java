package fi.aalto.trafficsense.trafficsense.ui;

import android.Manifest;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.graphics.drawable.DrawableCompat;
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

import static fi.aalto.trafficsense.trafficsense.util.InternalBroadcasts.KEY_DEBUG_SETTINGS_REQ;
import static fi.aalto.trafficsense.trafficsense.util.InternalBroadcasts.LABEL_STATE_INDEX;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,OnMapReadyCallback {

    private LocalBroadcastManager mLocalBroadcastManager;
    private BroadcastReceiver mBroadcastReceiver;
    private ActionBarDrawerToggle mDrawerToggle;
    private Context mContext;
    private MenuItem mStartupItem;
    private MenuItem mShutdownItem;
    private FloatingActionButton mFab;
    private GoogleMap mMap;
    private LatLngBounds mBounds;
    private Marker mMarker=null;
    private ActivityType latestActivityType=ActivityType.STILL;
    private boolean showTraffic=false;
    private boolean mapToolbarEnabled=false;

    private LatLng initPosition=(new LatLng(60.1841396, 24.8300838));
    private float initZoom=12;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
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
            case R.id.nav_login:
                openActivity(LoginActivity.class);
                break;
            case R.id.nav_settings:
                openActivity(SettingsActivity.class);
                break;
            case R.id.nav_shutdown:
                BroadcastHelper.simpleBroadcast(mLocalBroadcastManager, InternalBroadcasts.KEY_SERVICE_STOP);
                fixServiceStateToMenu(false);
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

//         mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
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

    private void updateLocation (Intent i) {
        if (i.hasExtra(InternalBroadcasts.KEY_LOCATION_UPDATE) & mMap != null) {
            Location l = i.getParcelableExtra(InternalBroadcasts.KEY_LOCATION_UPDATE);
            LatLng myPos = new LatLng(l.getLatitude(), l.getLongitude());
            if (mMarker == null) {
                mMarker = mMap.addMarker(new MarkerOptions().position(myPos).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_activity_still)));
            } else {
                mMarker.setPosition(myPos);
            }

            if (mBounds == null) {
                //This is the current user-viewable region of the map
                mBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
            }
            if (!mBounds.contains(myPos)) {
                mMap.moveCamera(CameraUpdateFactory.newLatLng(myPos));
                mBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
                if (!mapToolbarEnabled) { // do this only once
                    mMap.getUiSettings().setMapToolbarEnabled(true);
                    mapToolbarEnabled = true;
                }
            }

        }
    }

    private void updateActivity (Intent i) {
        if (i.hasExtra(InternalBroadcasts.KEY_ACTIVITY_UPDATE)) {
            ActivityData a = i.getParcelableExtra(InternalBroadcasts.KEY_ACTIVITY_UPDATE);
            ActivityType topActivity=a.getFirst().Type;

            if ((mMarker != null) && (topActivity != latestActivityType)) {
                switch (topActivity) {
                    case IN_VEHICLE:
                        mMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_activity_in_vehicle));
                        break;
                    case ON_BICYCLE:
                        mMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_activity_on_bicycle));
                        break;
                    case RUNNING:
                        mMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_activity_running));
                        break;
                    case STILL:
                        mMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_activity_still));
                        break;
                    case TILTING:
                        mMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_activity_tilting));
                        break;
                    case UNKNOWN:
                        mMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_activity_unknown));
                        break;
                    case WALKING:
                        mMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_activity_walking));
                        break;
                    default:
                        mMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_activity_unknown));
                        break;
                }
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
