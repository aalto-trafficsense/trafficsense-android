package fi.aalto.trafficsense.trafficsense.backend.backend_util;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import fi.aalto.trafficsense.trafficsense.backend.sensors.ActivitySensor;
import fi.aalto.trafficsense.trafficsense.backend.sensors.LocationSensor;
import fi.aalto.trafficsense.trafficsense.backend.sensors.SensorController;
import timber.log.Timber;

import static android.app.Activity.RESULT_OK;

/**
 * Google Play Services Interface
 */
public class PlayServiceInterface implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient mGoogleApiClient;
    private SensorController mSensorController;
    private LocationSensor mLocationSensor;
    private ActivitySensor mActivitySensor;
    private Context mApplicationContext;
    private Context mServiceContext;

    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";
    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError = false;

    public PlayServiceInterface(Context context) {
        mServiceContext = context;
        mApplicationContext = context.getApplicationContext();

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(mApplicationContext)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .addApi(ActivityRecognition.API)
                    .build();
        }
        mGoogleApiClient.connect();

    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if (mApplicationContext == null) {
            Timber.e("mApplicationContext null onConnected");
        } else {
            mSensorController = new SensorController(mServiceContext);
            initSensors();
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
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (result.hasResolution()) {
            try {
                mResolvingError = true;
                result.startResolutionForResult((Activity) mApplicationContext, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
            // Show dialog using GoogleApiAvailability.getErrorDialog()
            Timber.w("Fused Location Probe connection to Google Location API failed: " + result.toString());
            showErrorDialog(result.getErrorCode());
            mResolvingError = true;
        }
    }

    // The rest of this code is all about building the error dialog

    /* Creates a dialog for an error message */
    private void showErrorDialog(int errorCode) {
        // Create a fragment for the error dialog
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        FragmentActivity fAct = (FragmentActivity)mApplicationContext;
        Timber.e("Play service interface failure - trying to show the error dialog");
        dialogFragment.show(fAct.getSupportFragmentManager(), "errordialog");
    }

    /* Called from ErrorDialogFragment when the dialog is dismissed. */
    public void onDialogDismissed() {
        mResolvingError = false;
    }

    /* A fragment to display an error dialog */
    public static class ErrorDialogFragment extends DialogFragment {
        public ErrorDialogFragment() { }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GoogleApiAvailability.getInstance().getErrorDialog(
                    this.getActivity(), errorCode, REQUEST_RESOLVE_ERROR);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            // TODO: Figure out how to get the activity that this one likes
            // ((MyActivity) getActivity()).onDialogDismissed();
        }
    }


    private void initSensors() {
        if (mGoogleApiClient == null) {
            Timber.e("initLocationClient called with null mGoogleApiClient");
            return;
        }

        if(!mGoogleApiClient.isConnected()) {
            Timber.e("initLocationClient called with non-connected mGoogleApiClient");
        } else {
            mLocationSensor = new LocationSensor(mGoogleApiClient, mApplicationContext, mSensorController);
            mActivitySensor = new ActivitySensor(mGoogleApiClient, mApplicationContext);
        }
    }

    /*

     // TODO: Figure out how to handle this result outside of an activity
     // Brute force: Implement in all activities a local broadcast to the service to handle this

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_RESOLVE_ERROR) {
            mResolvingError = false;
            if (resultCode == RESULT_OK) {
                // Make sure the app is not already connected or attempting to connect
                if (!mGoogleApiClient.isConnecting() &&
                        !mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.connect();
                }
            }
        }
    }

    */

    public void disconnect() {
        mLocationSensor.disconnect(mGoogleApiClient);
        mGoogleApiClient.disconnect();
    }

}
