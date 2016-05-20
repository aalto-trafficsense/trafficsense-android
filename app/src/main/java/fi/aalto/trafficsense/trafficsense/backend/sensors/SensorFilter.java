package fi.aalto.trafficsense.trafficsense.backend.sensors;

import android.content.*;
import android.content.res.Resources;
import android.location.Location;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import com.google.android.gms.location.DetectedActivity;
import fi.aalto.trafficsense.trafficsense.R;
import fi.aalto.trafficsense.trafficsense.backend.TrafficSenseService;
import fi.aalto.trafficsense.trafficsense.backend.uploader.PipelineThread;
import fi.aalto.trafficsense.trafficsense.util.*;
import timber.log.Timber;

import static android.support.v7.preference.PreferenceManager.getDefaultSharedPreferences;

/**
 * Created by mikko.rinne@aalto.fi on 10/04/16.
 */
public class SensorFilter {

    private LocalBroadcastManager mLocalBroadcastManager;
    private BroadcastReceiver mBroadcastReceiver;
    private PipelineThread mPipelineThread;
    private Resources mRes;
    private SharedPreferences mSettings;


    private SensorController mSensorController;

    // private long stillLimitSeconds=40;
    // private long queuePingThresholdMinutes=60;
    // private double queueAccuracyThreshold=1000.0; // meters

    private Location lastReceivedLocation;
    private ActivityData lastReceivedActivity = null;
    private ActivityType lastReceivedActType;
    private long inactivityTimer;
    private boolean inactivityTimerRunning = false;

    private Location lastQueuedLocation = null;
    private ActivityType lastQueuedActType = ActivityType.STILL;
    private long lastQueuedTime = 0;

    private ActivityData dummyActivity;

    /* Constructor */
    public SensorFilter(SensorController sCntrl) {
        // Pointer to instruct SensorController
        mSensorController = sCntrl;
        mRes = TrafficSenseService.getContext().getResources();

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(TrafficSenseService.getContext());
        initBroadcastReceiver();

        mPipelineThread = TrafficSenseService.getPipeline().getPipelineThread();
        if (mPipelineThread == null) Timber.e("PipelineThread null in SensorFilter init!");

        dummyActivity = new ActivityData(System.currentTimeMillis());
        dummyActivity.add(DetectedActivity.UNKNOWN,0);

        mSettings = getDefaultSharedPreferences(TrafficSenseService.getContext());

    }

    public void addLocation(Location l) {
        lastReceivedLocation = l;

        broadcastSingleSensorData(InternalBroadcasts.KEY_LOCATION_UPDATE, l);
        if (inactivityTimerRunning) checkSleep(); // Check here also
        filterOutgoing();
    }

    private void filterOutgoing() {
        // Queue whatever, if time after last queued is > queuePingThresholdMinutes
        if (lastQueuedTime > 0) {
            int queuePingThresholdMinutes = Integer.valueOf(mSettings.getString(mRes.getString(R.string.debug_settings_ping_threshold_key), "60"));
            if (queuePingThresholdMinutes > 0) { // -1 = no ping
                if (System.currentTimeMillis() - lastQueuedTime > queuePingThresholdMinutes*60000) {
                    queueOutgoing();
                    return;
                }
            }
        }
        Timber.d("Queue accuracy threshold from settings: %s", mSettings.getString("debug_settings_queue_location_accuracy", "ei saatu"));
        // Proceed if accuracy is <= location_accuracy m.
        if (lastReceivedLocation.getAccuracy() <= (float) mSettings.getInt(mRes.getString(R.string.debug_settings_location_accuracy_key), 50)) {
            // Queue if activity is different
            if (lastReceivedActType != lastQueuedActType) {
                queueOutgoing();
                return;
            }
            // Queue when distance to previously queued > lastAccuracy m.
            if (lastQueuedLocation == null) {
                queueOutgoing();
                return;
            } else {
                if (lastQueuedLocation.distanceTo(lastReceivedLocation) >= lastReceivedLocation.getAccuracy()) {
                    // Moving. If inactivity timer is running (= mistake), restart timer
                    if (inactivityTimerRunning) inactivityTimer = System.currentTimeMillis();
                    queueOutgoing();
                    return;
                }
            }
        }
    }

    private void queueOutgoing() {
        ActivityData ad;
        if (lastReceivedActivity == null) ad = dummyActivity; else ad = lastReceivedActivity;
        LocationData ld = new LocationData(lastReceivedLocation.getAccuracy(),lastReceivedLocation.getLatitude(),lastReceivedLocation.getLongitude(),lastReceivedLocation.getTime());
        mPipelineThread.sendData(new DataPacket(ld, ad));

        lastQueuedTime = System.currentTimeMillis();
        lastQueuedLocation = lastReceivedLocation;
        lastQueuedActType = lastReceivedActType;
    }

    private void addActivity(ActivityData a) {
        lastReceivedActivity = a;
        lastReceivedActType = a.getFirst().Type;
        if (TrafficSenseService.getServiceState() == TSServiceState.SLEEPING) {
            if (lastReceivedActType != ActivityType.STILL) {
                mSensorController.setSleep(false);
                TrafficSenseService.setServiceState(TSServiceState.RUNNING);
            }
        } else { // Not SLEEPING
            if (lastReceivedActType == ActivityType.STILL) {
                if (inactivityTimerRunning) {
                    checkSleep();
                } else { // inActivityTimer not yet on
                    inactivityTimerRunning = true;
                    inactivityTimer = System.currentTimeMillis();
                }
            } else { // Not SLEEPING, nor STILL
                inactivityTimerRunning = false;
            }
        }

        // ActivityRecognitionIntentService already broadcasts the data - do not re-send.
    }

    private void checkSleep() {
        if (System.currentTimeMillis()-inactivityTimer > mSettings.getInt(mRes.getString(R.string.debug_settings_sleep_threshold_key), 40)*1000) {
            inactivityTimerRunning = false;
            mSensorController.setSleep(true);
            TrafficSenseService.setServiceState(TSServiceState.SLEEPING);
        }
    }

    /*************************
     Broadcast handling
     *************************/

    /* Local broadcast receiver */
    private void initBroadcastReceiver() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                switch (action) {
                    case InternalBroadcasts.KEY_ACTIVITY_UPDATE:
                        ActivityData ad = intent.getParcelableExtra(InternalBroadcasts.KEY_ACTIVITY_UPDATE);
                        addActivity(ad);
                        break;
                    case InternalBroadcasts.KEY_DEBUG_SHOW_REQ:
                    case InternalBroadcasts.KEY_MAIN_ACTIVITY_REQ:
                        broadcastLatestSensorData();
                        break;
                }
            }
        };

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(InternalBroadcasts.KEY_ACTIVITY_UPDATE);
        intentFilter.addAction(InternalBroadcasts.KEY_DEBUG_SHOW_REQ);
        intentFilter.addAction(InternalBroadcasts.KEY_MAIN_ACTIVITY_REQ);

        if (mLocalBroadcastManager != null) {
            mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, intentFilter);
        }
    }


    // Broadcast single sensor data
    private void broadcastSingleSensorData(String key, Parcelable val) {
        if (TrafficSenseService.isViewActive()) {
            Intent i = new Intent(key);
            i.putExtra(key, val);
            mLocalBroadcastManager.sendBroadcast(i);
        }
    }

    // Combined sensor broadcast
    private void broadcastLatestSensorData() {
        if (TrafficSenseService.isViewActive()) {
            Intent i = new Intent(InternalBroadcasts.KEY_SENSORS_UPDATE);
            Boolean content = false;
            if (lastReceivedLocation != null) {
                i.putExtra(InternalBroadcasts.KEY_LOCATION_UPDATE, lastReceivedLocation);
                content = true;
            }
            if (lastReceivedActivity != null) {
                i.putExtra(InternalBroadcasts.KEY_ACTIVITY_UPDATE, lastReceivedActivity);
                content = true;
            }
            if (content) mLocalBroadcastManager.sendBroadcast(i);
        }
    }

    public void disconnect() {
        mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
    }

}
