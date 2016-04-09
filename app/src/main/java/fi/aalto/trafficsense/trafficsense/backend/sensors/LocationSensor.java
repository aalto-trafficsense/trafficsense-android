package fi.aalto.trafficsense.trafficsense.backend.sensors;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import fi.aalto.trafficsense.trafficsense.backend.TrafficSenseService;
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

    /* Constructor */
    public LocationSensor(GoogleApiClient apiClient, Context sContext) {
        mGoogleApiClient = apiClient;

        // subscribe for location updates
        if (ContextCompat.checkSelfPermission(sContext, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            // TODO: Implement requester, test with Marshmallow
            Toast.makeText(sContext, "Something wrong - no permission to access fine location.", Toast.LENGTH_SHORT).show();
        } else {
            // Access to the location has been granted to the app.
            locationRequest(interval,priority);
        }
    }

    /* Public interface */

    public void goToSleep() {
        locationRequest(interval,sleep_priority);
    }

    public void wakeUp() {
        locationRequest(interval,priority);
    }

    /* Internal implementation */

    private void locationRequest(int interval, int priority) {
        // Set location request settings
        LocationRequest mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(interval);
        mLocationRequest.setFastestInterval(fastestInterval);
        mLocationRequest.setPriority(priority);

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        Timber.i("Requested location updates with interval: " + interval);
    }


    @Override
    public void onLocationChanged(Location location) {
        // Do something here
        Timber.d("Received location update");
    }

    public void disconnect(GoogleApiClient apiClient) {
        if(apiClient != null)
            LocationServices.FusedLocationApi.removeLocationUpdates(apiClient, this);
        Timber.d("Fused Location Probe stopped");
    }

}
