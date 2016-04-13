package fi.aalto.trafficsense.trafficsense.backend.sensors;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import com.google.android.gms.location.DetectedActivity;
import fi.aalto.trafficsense.trafficsense.backend.TrafficSenseService;
import fi.aalto.trafficsense.trafficsense.util.ActivityData;
import fi.aalto.trafficsense.trafficsense.util.InternalBroadcasts;
import fi.aalto.trafficsense.trafficsense.util.TSServiceState;

/**
 * Created by rinnem2 on 10/04/16.
 */
public class SensorFilter {

    private LocalBroadcastManager mLocalBroadcastManager;
    private BroadcastReceiver mBroadcastReceiver;

    private SensorController mSensorController;

    private Location lastReceivedLocation;
    private ActivityData lastReceivedActivity;
    private int lastReceivedActType;
    private long inactivityTimer;
    private boolean inactivityTimerRunning = false;

    private long stillLimitSeconds=40;

    /* Constructor */
    public SensorFilter(SensorController sCntrl, Context serviceContext) {
        // Pointer to instruct SensorController
        mSensorController = sCntrl;

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(serviceContext);
        initBroadcastReceiver();
    }

    public void addLocation(Location l) {
        lastReceivedLocation = l;

        broadcastSingleSensorData(InternalBroadcasts.KEY_LOCATION_UPDATE, l);
        if (inactivityTimerRunning) checkSleep(); // Check here also
    }

    private void addActivity(ActivityData a) {
        lastReceivedActivity = a;
        lastReceivedActType = a.getFirst().Type;
        if (TrafficSenseService.getServiceState() == TSServiceState.SLEEPING) {
            if (lastReceivedActType != DetectedActivity.STILL) {
                mSensorController.setSleep(false);
                TrafficSenseService.setServiceState(TSServiceState.RUNNING);
            }
        } else { // Not SLEEPING
            if (lastReceivedActType == DetectedActivity.STILL) {
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
        if (System.currentTimeMillis()-inactivityTimer > stillLimitSeconds*1000) {
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
                        broadcastLatestSensorData();
                        break;
                }
            }
        };

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(InternalBroadcasts.KEY_ACTIVITY_UPDATE);
        intentFilter.addAction(InternalBroadcasts.KEY_DEBUG_SHOW_REQ);

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
