package fi.aalto.trafficsense.trafficsense.backend.sensors;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import timber.log.Timber;

/**
 * Created by rinnem2 on 09/04/16.
 */
public class ActivitySensor implements ResultCallback<Status> {

    private int interval = 10; // unit, seconds

    private PendingIntent mCallbackIntent;
    private GoogleApiClient mGoogleApiClient;

    public ActivitySensor(GoogleApiClient apiClient, Context sContext) {
        Timber.d("ActivitySensor constructor");
        mGoogleApiClient = apiClient;

        Intent intent = new Intent(sContext,ActivityRecognitionIntentService.class);

        mCallbackIntent = PendingIntent.getService(sContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // subscribe for activity recognition updates
        final long intervalInMilliseconds = interval * 1000L;
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mGoogleApiClient,
                intervalInMilliseconds, mCallbackIntent).setResultCallback(this);

    }

    /**
     * Runs when the result of calling requestActivityUpdates() and removeActivityUpdates() becomes
     * available. Either method can complete successfully or with an error.
     *
     * @param status The Status returned through a PendingIntent when requestActivityUpdates()
     *               or removeActivityUpdates() are called.
     */
    public void onResult(Status status) {
        if (status.isSuccess()) {
            Timber.d("Activity updates successfully requested");
        } else {
            Timber.e("Error adding or removing activity detection: " + status.getStatusMessage());
        }
    }

}
