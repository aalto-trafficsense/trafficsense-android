package fi.aalto.trafficsense.trafficsense.backend.sensors;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import fi.aalto.trafficsense.trafficsense.R;
import fi.aalto.trafficsense.trafficsense.TrafficSenseApplication;
import fi.aalto.trafficsense.trafficsense.backend.TrafficSenseService;
import fi.aalto.trafficsense.trafficsense.util.InternalBroadcasts;
import timber.log.Timber;

import static android.support.v7.preference.PreferenceManager.getDefaultSharedPreferences;

/**
 * Created by mikko.rinne@aalto.fi on 09/04/16.
 */
public class LocationSensor implements LocationListener {

    private final int AWAKE_PRIORITY = LocationRequest.PRIORITY_HIGH_ACCURACY;
    private final int SLEEP_PRIORITY = LocationRequest.PRIORITY_NO_POWER;

    private GoogleApiClient mGoogleApiClient;
    private SensorFilter mSensorFilter;
    private SharedPreferences mSettings;
    private Resources mRes;
    private LocalBroadcastManager mLocalBroadcastManager;

    /* Constructor */
    public LocationSensor(GoogleApiClient apiClient, SensorFilter controller) {
        mGoogleApiClient = apiClient;
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(TrafficSenseService.getContext());
        mSensorFilter = controller;
        mRes = TrafficSenseService.getContext().getResources();
        mSettings = getDefaultSharedPreferences(TrafficSenseService.getContext());

        locationRequest(AWAKE_PRIORITY);
    }

    /* Public interface */

    public void setSleep(boolean state) {
        if (state) locationRequest(SLEEP_PRIORITY);
        else locationRequest(AWAKE_PRIORITY);
    }

    /* Internal implementation */

    private void locationRequest(int priority) {
        if (mGoogleApiClient != null) {
            if (mGoogleApiClient.isConnected()) {
                // Confirm that location permission is still valid
                if (ContextCompat.checkSelfPermission(TrafficSenseApplication.getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    // Set location request settings
                    LocationRequest mLocationRequest = LocationRequest.create();
                    int interval = mSettings.getInt(mRes.getString(R.string.debug_settings_location_interval_key), 10);
                    mLocationRequest.setInterval(interval);
                    mLocationRequest.setFastestInterval(interval*1000);
                    mLocationRequest.setPriority(priority);

                    LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
                    Timber.i("Requested location updates with interval: %d", interval);
                }

            } else {
                Timber.w("locationRequest called, but GoogleApiClient is not connected");
                requestPlayReEstablish();
            }
        } else {
            Timber.w("locationRequest called, but GoogleApiClient is null");
            requestPlayReEstablish();
        }
    }

// debug_settings_location_interval_key

    @Override
    public void onLocationChanged(Location location) {
        mSensorFilter.addLocation(location);
        Timber.d("Received location update, request interval: %d", mSettings.getInt(mRes.getString(R.string.debug_settings_location_interval_key), -1));
    }

    public void disconnect() {
        if(mGoogleApiClient != null) {
            if (mGoogleApiClient.isConnected()) {
                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
                mGoogleApiClient = null;
            }
        }
        Timber.d("LocationSensor stopped");
    }

    // Request TrafficSenseService to re-establish the whole play services & sensor chain
    // Used when there is a problem with GoogleApiClient
    private void requestPlayReEstablish() {
        mLocalBroadcastManager.sendBroadcast(new Intent(InternalBroadcasts.KEY_REQUEST_PLAY_RE_ESTABLISH));
    }

}
