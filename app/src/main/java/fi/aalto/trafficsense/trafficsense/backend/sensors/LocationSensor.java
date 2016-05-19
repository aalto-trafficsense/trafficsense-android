package fi.aalto.trafficsense.trafficsense.backend.sensors;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.support.v4.content.ContextCompat;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import fi.aalto.trafficsense.trafficsense.R;
import fi.aalto.trafficsense.trafficsense.TrafficSenseApplication;
import fi.aalto.trafficsense.trafficsense.backend.TrafficSenseService;
import timber.log.Timber;

import static android.support.v7.preference.PreferenceManager.getDefaultSharedPreferences;

/**
 * Created by mikko.rinne@aalto.fi on 09/04/16.
 */
public class LocationSensor implements LocationListener {

    // Configurations //
    private int interval = 10; // unit, seconds
    private int fastestInterval = 5000; // milliseconds
    private int priority = LocationRequest.PRIORITY_HIGH_ACCURACY;
    private int sleep_priority = LocationRequest.PRIORITY_NO_POWER;

    private GoogleApiClient mGoogleApiClient;
    private SensorFilter mSensorFilter;
    private SharedPreferences mSettings;
    private Resources mRes;

    /* Constructor */
    public LocationSensor(GoogleApiClient apiClient, SensorFilter controller) {
        mGoogleApiClient = apiClient;
        mSensorFilter = controller;
        mRes = TrafficSenseService.getContext().getResources();

        locationRequest(interval,priority);

        mSettings = getDefaultSharedPreferences(TrafficSenseService.getContext());

    }

    /* Public interface */

    public void setSleep(boolean state) {
        if (state) locationRequest(interval,sleep_priority);
        else locationRequest(interval,priority);
    }

    /* Internal implementation */

    private void locationRequest(int interval, int priority) {
        // Check whether we still have location permission!
        if (ContextCompat.checkSelfPermission(TrafficSenseApplication.getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            // MJR: Don't do anything - MainActivity will try to get permission.
            // Context ctx = TrafficSenseApplication.getContext();
            // Toast.makeText(ctx, ctx.getString(R.string.no_location_permission), Toast.LENGTH_SHORT).show();
        } else {
            // Set location request settings
            LocationRequest mLocationRequest = LocationRequest.create();
            mLocationRequest.setInterval(interval);
            mLocationRequest.setFastestInterval(fastestInterval);
            mLocationRequest.setPriority(priority);

            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            // Timber.i("Requested location updates with interval: " + interval);
        }

    }


    @Override
    public void onLocationChanged(Location location) {
        mSensorFilter.addLocation(location);
        Timber.d("Received location update, request interval: ", mSettings.getInt(mRes.getString(R.string.debug_settings_location_interval_key), -1));
    }

    public void disconnect() {
        if(mGoogleApiClient != null)
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        Timber.d("LocationSensor stopped");
    }

}
