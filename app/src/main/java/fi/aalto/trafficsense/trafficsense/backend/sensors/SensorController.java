package fi.aalto.trafficsense.trafficsense.backend.sensors;

import android.content.Context;
import com.google.android.gms.common.api.GoogleApiClient;
import timber.log.Timber;

/**
 * Created by mikko.rinne@aalto.fi on 12/04/16.
 */
public class SensorController {

    private Context mApplicationContext;
    private Context mServiceContext;
    private SensorFilter mSensorFilter;
    private LocationSensor mLocationSensor;
    private ActivitySensor mActivitySensor;
    /*
        Constructor initialises all sensors and the filter
    */
    public SensorController (GoogleApiClient gApiClient, Context serviceCntxt) {
        mServiceContext = serviceCntxt;
        mApplicationContext = serviceCntxt.getApplicationContext();
        if (gApiClient == null) {
            Timber.e("SensorController created with null GoogleApiClient");
            return;
        }

        if(!gApiClient.isConnected()) {
            Timber.e("SensorController created with non-connected GoogleApiClient");
        } else {
            mSensorFilter = new SensorFilter(this, mServiceContext);
            mLocationSensor = new LocationSensor(gApiClient, mApplicationContext, mSensorFilter);
            mActivitySensor = new ActivitySensor(gApiClient, mApplicationContext, mSensorFilter);
        }

    }

    public void disconnect() {
        mLocationSensor.disconnect();
        mActivitySensor.disconnect();
    }

}
