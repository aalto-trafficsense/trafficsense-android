package fi.aalto.trafficsense.trafficsense.backend.sensors;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import fi.aalto.trafficsense.trafficsense.R;
import fi.aalto.trafficsense.trafficsense.TrafficSenseApplication;
import fi.aalto.trafficsense.trafficsense.backend.TrafficSenseService;
import timber.log.Timber;

import static android.support.v7.preference.PreferenceManager.getDefaultSharedPreferences;

/**
 * Created by mikko.rinne@aalto.fi on 09/04/16.
 */
public class ActivitySensor implements ResultCallback<Status> {

    private int interval = 10; // unit, seconds

    private PendingIntent mCallbackIntent;
    private GoogleApiClient mGoogleApiClient;
    private Resources mRes;
    private SharedPreferences mSettings;


    public ActivitySensor(GoogleApiClient apiClient) {
        mGoogleApiClient = apiClient;

        Intent intent = new Intent(TrafficSenseApplication.getContext(),ActivityRecognitionIntentService.class);
        mCallbackIntent = PendingIntent.getService(TrafficSenseApplication.getContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        mRes = TrafficSenseService.getContext().getResources();
        mSettings = getDefaultSharedPreferences(TrafficSenseService.getContext());

        startActivityRecognitionUpdates();
    }

    private void startActivityRecognitionUpdates() {
        if (mGoogleApiClient != null) {
            interval = mSettings.getInt(mRes.getString(R.string.debug_settings_activity_interval_key), 1);

            Timber.d("ActivitySensor started with interval: %d", interval);

            ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mGoogleApiClient,
                    interval * 1000L, mCallbackIntent).setResultCallback(this);
        }
    }

    private void stopActivityRecognitionUpdates() {
        if(mGoogleApiClient != null)
            ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mGoogleApiClient, mCallbackIntent).setResultCallback(this);
    }

    public void restartActivityRecognition() {
        stopActivityRecognitionUpdates();
        startActivityRecognitionUpdates();
    }

    /**
     * Runs when the result of calling requestActivityUpdates() and removeActivityUpdates() becomes
     * available. Either method can complete successfully or with an error.
     *
     * @param status The Status returned through a PendingIntent when requestActivityUpdates()
     *               or removeActivityUpdates() are called.
     */
    public void onResult(Status status) {
        if (!status.isSuccess()) {
            Timber.e("Error adding or removing activity detection: " + status.getStatusMessage());
        }
    }

    public void disconnect() {
        stopActivityRecognitionUpdates();
        Timber.d("ActivitySensor stopped");
    }


}
