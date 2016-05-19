package fi.aalto.trafficsense.trafficsense.backend.sensors;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import com.google.android.gms.common.api.GoogleApiClient;
import fi.aalto.trafficsense.trafficsense.TrafficSenseApplication;
import fi.aalto.trafficsense.trafficsense.backend.TrafficSenseService;
import fi.aalto.trafficsense.trafficsense.backend.uploader.RegularRoutesPipeline;
import fi.aalto.trafficsense.trafficsense.util.ActivityData;
import fi.aalto.trafficsense.trafficsense.util.InternalBroadcasts;
import timber.log.Timber;

/**
 * Created by mikko.rinne@aalto.fi on 12/04/16.
 */
public class SensorController {

    private LocalBroadcastManager mLocalBroadcastManager;
    private BroadcastReceiver mBroadcastReceiver;

    private SensorFilter mSensorFilter;
    private LocationSensor mLocationSensor;
    private ActivitySensor mActivitySensor;
    /*
        Constructor initialises all sensors and the filter
    */
    public SensorController (GoogleApiClient gApiClient) {
        if (gApiClient == null) {
            Timber.e("SensorController created with null GoogleApiClient");
            return;
        }

        if(!gApiClient.isConnected()) {
            Timber.e("SensorController created with non-connected GoogleApiClient");
        } else {
            mSensorFilter = new SensorFilter(this);
            mLocationSensor = new LocationSensor(gApiClient, mSensorFilter);
            mActivitySensor = new ActivitySensor(gApiClient);
        }

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(TrafficSenseService.getContext());
        initBroadcastReceiver();

    }

    public void setSleep(boolean state) {
        mLocationSensor.setSleep(state);
    }

    public void disconnect() {
        mLocationSensor.disconnect();
        mActivitySensor.disconnect();
        mSensorFilter.disconnect();
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
                    case InternalBroadcasts.KEY_SETTINGS_ACTIVITY_INTERVAL:
                        mActivitySensor.restartActivityRecognition();
                        break;
                }
            }
        };

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(InternalBroadcasts.KEY_SETTINGS_ACTIVITY_INTERVAL);

        if (mLocalBroadcastManager != null) {
            mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, intentFilter);
        }
    }


}
