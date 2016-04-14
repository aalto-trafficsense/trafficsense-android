package fi.aalto.trafficsense.trafficsense.backend.sensors;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import fi.aalto.trafficsense.trafficsense.util.ActivityData;
import fi.aalto.trafficsense.trafficsense.util.InternalBroadcasts;

import java.util.*;

/**
 * Created by rinnem2 on 11/04/16.
 */
public class ActivityRecognitionIntentService extends IntentService {
    public ActivityRecognitionIntentService() {
        super("ActivityRecognitionIntentService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            handleActivityRecognitionResult(ActivityRecognitionResult.extractResult(intent));
        }
    }

    /**
     * Receive the activity recognition result
     **/
    private void handleActivityRecognitionResult(final ActivityRecognitionResult result) {
        final int maxNumberOfActivitiesCollected = 3;
        final List<DetectedActivity> activities = result.getProbableActivities();
        // Timber.d("Activities:" + activities.toString());

        List<Act> activityList = new ArrayList<>();

        // Add all into a list
        for (int i = 0; i < activities.size(); ++i) {
            DetectedActivity act = activities.get(i);
            int type = act.getType();
            if (type != DetectedActivity.ON_FOOT) { // Skip ON_FOOT, only add WALKING and RUNNING
                activityList.add(new Act(type, act.getConfidence()));
            }
        }

        Collections.sort(activityList);
        ActivityData selected = new ActivityData(result.getTime());

        for (int i = 0; i < Math.min(activityList.size(), maxNumberOfActivitiesCollected); ++i) {
            Act entry = activityList.get(i);
            selected.add(entry.getType(),entry.getConfidence());
        }

        Intent intent = new Intent(InternalBroadcasts.KEY_ACTIVITY_UPDATE);
        intent.putExtra(InternalBroadcasts.KEY_ACTIVITY_UPDATE, selected);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    private class Act implements Comparable<Act> {
        private Integer type;
        private Integer conf;
        Act(Integer l, Integer r){
            this.type = l;
            this.conf = r;
        }
        public Integer getType(){ return type; }
        public Integer getConfidence(){ return conf; }
        public void setL(Integer l){ this.type = l; }
        public void setR(Integer r){ this.conf = r; }
        public int compareTo(Act a2) {
            return a2.conf - this.conf;
        }
    }

}
