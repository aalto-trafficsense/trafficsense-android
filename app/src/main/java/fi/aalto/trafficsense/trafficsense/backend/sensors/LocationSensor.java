package fi.aalto.trafficsense.trafficsense.backend.sensors;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import fi.aalto.trafficsense.trafficsense.R;
import fi.aalto.trafficsense.trafficsense.TrafficSenseApplication;
import fi.aalto.trafficsense.trafficsense.backend.TrafficSenseService;
import fi.aalto.trafficsense.trafficsense.ui.MainActivity;
import timber.log.Timber;

/**
 * Created by rinnem2 on 09/04/16.
 */
public class LocationSensor implements LocationListener {

    // Configurations //
    private int interval = 10; // unit, seconds
    private int fastestInterval = 5000; // milliseconds
    private int priority = LocationRequest.PRIORITY_HIGH_ACCURACY;
    private int sleep_priority = LocationRequest.PRIORITY_NO_POWER;

    private GoogleApiClient mGoogleApiClient;
    private SensorFilter mSensorFilter;

    /* Constructor */
    public LocationSensor(GoogleApiClient apiClient, SensorFilter controller) {
        mGoogleApiClient = apiClient;
        mSensorFilter = controller;

        locationRequest(interval,priority);
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
            Context ctx = TrafficSenseApplication.getContext();
            Toast.makeText(ctx, ctx.getString(R.string.no_location_permission), Toast.LENGTH_SHORT).show();
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
//        Timber.d("Received location update");
    }

    public void disconnect() {
        if(mGoogleApiClient != null)
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        Timber.d("LocationSensor stopped");
    }

}
