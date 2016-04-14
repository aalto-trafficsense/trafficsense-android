package fi.aalto.trafficsense.trafficsense.backend.sensors;

import android.content.Context;
import com.google.android.gms.common.api.GoogleApiClient;
import fi.aalto.trafficsense.trafficsense.TrafficSenseApplication;
import timber.log.Timber;

/**
 * Created by mikko.rinne@aalto.fi on 12/04/16.
 */
public class SensorController {

    private Context mServiceContext;
    private SensorFilter mSensorFilter;
    private LocationSensor mLocationSensor;
    private ActivitySensor mActivitySensor;
    /*
        Constructor initialises all sensors and the filter
    */
    public SensorController (GoogleApiClient gApiClient, Context serviceCntxt) {
        mServiceContext = serviceCntxt;
        if (gApiClient == null) {
            Timber.e("SensorController created with null GoogleApiClient");
            return;
        }

        if(!gApiClient.isConnected()) {
            Timber.e("SensorController created with non-connected GoogleApiClient");
        } else {
            mSensorFilter = new SensorFilter(this, mServiceContext);
            mLocationSensor = new LocationSensor(gApiClient, TrafficSenseApplication.getContext(), mSensorFilter);
            mActivitySensor = new ActivitySensor(gApiClient, TrafficSenseApplication.getContext(), mSensorFilter);
        }

    }

    public void setSleep(boolean state) {
        mLocationSensor.setSleep(state);
    }

    public void disconnect() {
        mLocationSensor.disconnect();
        mActivitySensor.disconnect();
    }

}
