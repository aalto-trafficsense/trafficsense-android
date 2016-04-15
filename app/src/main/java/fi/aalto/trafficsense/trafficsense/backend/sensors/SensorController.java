package fi.aalto.trafficsense.trafficsense.backend.sensors;

import android.content.Context;
import com.google.android.gms.common.api.GoogleApiClient;
import fi.aalto.trafficsense.trafficsense.TrafficSenseApplication;
import fi.aalto.trafficsense.trafficsense.backend.uploader.RegularRoutesPipeline;
import timber.log.Timber;

/**
 * Created by mikko.rinne@aalto.fi on 12/04/16.
 */
public class SensorController {

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

    }

    public void setSleep(boolean state) {
        mLocationSensor.setSleep(state);
    }

    public void disconnect() {
        mLocationSensor.disconnect();
        mActivitySensor.disconnect();
        mSensorFilter.disconnect();
    }

}
