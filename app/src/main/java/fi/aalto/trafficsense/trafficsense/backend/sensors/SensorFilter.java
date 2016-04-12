package fi.aalto.trafficsense.trafficsense.backend.sensors;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import fi.aalto.trafficsense.trafficsense.util.ActivityData;
import fi.aalto.trafficsense.trafficsense.util.InternalBroadcasts;

/**
 * Created by rinnem2 on 10/04/16.
 */
public class SensorFilter {

    private LocalBroadcastManager mLocalBroadcastManager;
    private BroadcastReceiver mBroadcastReceiver;

    private SensorController mSensorController;

    private Location lastReceivedLocation;
    private ActivityData lastReceivedActivity;

    /* Constructor */
    public SensorFilter(SensorController sCntrl, Context serviceContext) {
        // Pointer to instruct SensorController
        mSensorController = sCntrl;
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(serviceContext);
        initBroadcastReceiver();
    }

    public void addLocation(Location l) {
        lastReceivedLocation = l;

        broadcastSensorData(InternalBroadcasts.KEY_LOCATION_UPDATE, l);
    }

    public void addActivity(ActivityData a) {
        lastReceivedActivity = a;

        // ActivityRecognitionIntentService already broadcasts the data - do not re-send.
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
                }
            }
        };

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(InternalBroadcasts.KEY_ACTIVITY_UPDATE);

        if (mLocalBroadcastManager != null) {
            mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, intentFilter);
        }
    }


    // Broadcast sensor data
    private void broadcastSensorData(String key, Parcelable val) {
        Intent i = new Intent(key);
        i.putExtra(key, val);
        mLocalBroadcastManager.sendBroadcast(i);
    }

    public void disconnect() {
        mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
    }


}
