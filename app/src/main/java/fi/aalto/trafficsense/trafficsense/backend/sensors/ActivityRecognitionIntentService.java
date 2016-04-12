package fi.aalto.trafficsense.trafficsense.backend.sensors;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import fi.aalto.trafficsense.trafficsense.util.ActivityData;
import fi.aalto.trafficsense.trafficsense.util.InternalBroadcasts;
import timber.log.Timber;

import java.math.BigDecimal;
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
        final Date now = new Date();
        final List<DetectedActivity> activities = result.getProbableActivities();
        Timber.d("Activities:" + activities.toString());

        List<Act> activityList = new ArrayList<>();

        // Put all into a HashMap
        for (int i = 0; i < activities.size(); ++i) {
            DetectedActivity act = activities.get(i);
            int type = act.getType();
            if (type != DetectedActivity.ON_FOOT) { // Skip ON_FOOT, only add WALKING and RUNNING
                activityList.add(new Act(type, act.getConfidence()));
            }
        }

        Collections.sort(activityList);
        ActivityData selected = new ActivityData();

        for (int i = 0; i < Math.min(activityList.size(), maxNumberOfActivitiesCollected); ++i) {
            Act entry = activityList.get(i);
            selected.add(entry.getType(),entry.getConfidence());
        }

        Timber.d("Activities sorted and chopped:" + selected.toString());

        // Collect enough data to construct the result on the receiver side
        Intent intent = new Intent(InternalBroadcasts.KEY_ACTIVITY_UPDATE);
        intent.putExtra(InternalBroadcasts.KEY_ACTIVITY_UPDATE, selected);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    private class Act implements Comparable<Act> {
        private Integer act_type;
        private Integer prob;
        Act(Integer l, Integer r){
            this.act_type = l;
            this.prob = r;
        }
        public Integer getType(){ return act_type; }
        public Integer getConfidence(){ return prob; }
        public void setL(Integer l){ this.act_type = l; }
        public void setR(Integer r){ this.prob = r; }
        public int compareTo(Act a2) {
            return a2.prob-this.prob;
        }
    }

}
