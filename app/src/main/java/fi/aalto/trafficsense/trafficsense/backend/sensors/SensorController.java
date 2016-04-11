package fi.aalto.trafficsense.trafficsense.backend.sensors;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import fi.aalto.trafficsense.trafficsense.util.InternalBroadcasts;

/**
 * Created by rinnem2 on 10/04/16.
 */
public class SensorController {

    private LocalBroadcastManager mLocalBroadcastManager;

    private Location lastLocation;

    /* Constructor */
    public SensorController(Context serviceContext) {
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(serviceContext);
    }

    public void addLocation(Location l) {
        lastLocation = l;

        broadcastSensorData(InternalBroadcasts.KEY_LOCATION_UPDATE, l);

    }

    /*************************
     Broadcast handling
     *************************/

    // Broadcast sensor data
    private void broadcastSensorData(String key, Parcelable val) {
        Intent i = new Intent(key);
        i.putExtra(key, val);
        mLocalBroadcastManager.sendBroadcast(i);
    }


}
