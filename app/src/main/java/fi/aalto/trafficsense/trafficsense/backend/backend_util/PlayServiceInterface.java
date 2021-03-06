package fi.aalto.trafficsense.trafficsense.backend.backend_util;

import android.os.Bundle;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.LocationServices;
import fi.aalto.trafficsense.trafficsense.TrafficSenseApplication;
import fi.aalto.trafficsense.trafficsense.backend.sensors.SensorController;
import timber.log.Timber;

/**
 * Google Play Services Interface
 *
 * 11.10.2016: MJR simplified error handling here, since the splash activity now handles
 *             dialogs in case of play services connection errors.
 */
public class PlayServiceInterface implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient mGoogleApiClient;
    private SensorController mSensorController;

    // Request code to use when launching the resolution activity
    // private static final int REQUEST_RESOLVE_ERROR = 1001;
    // Unique tag for the error dialog fragment
    // private static final String DIALOG_ERROR = "dialog_error";
    // Bool to track whether the app is already resolving an error
    private static boolean mResolvingError = false;

    public PlayServiceInterface() {
        resumePlayServiceInterface();
    }

    public void resumePlayServiceInterface() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(TrafficSenseApplication.getContext())
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .addApi(ActivityRecognition.API)
                    .build();
        }
        if (!mGoogleApiClient.isConnecting() &&
                !mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if (TrafficSenseApplication.getContext() == null) {
            Timber.e("TrafficSenseApplication.getContext() null onConnected");
        } else {
            mSensorController = new SensorController(mGoogleApiClient);
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        switch (cause) {
            case CAUSE_NETWORK_LOST:
                Timber.w("PlayService connection suspended: Network lost");
                break;
            case CAUSE_SERVICE_DISCONNECTED:
                Timber.w("PlayService connection suspended: Service disconnected");
                break;
        }
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (!mResolvingError) {
            if (result.hasResolution()) {
                Timber.w("PlayServiceInterface failed to connect, resolution available: %s", result.toString());
                mResolvingError = false;
            } else {
                Timber.w("PlayServiceInterface failed to connect to Google Play Services: %s", result.toString());
                mResolvingError = false;
            }
        }
    }

    public void disconnect() {
        mSensorController.disconnect();
        mGoogleApiClient.disconnect();
    }

}
