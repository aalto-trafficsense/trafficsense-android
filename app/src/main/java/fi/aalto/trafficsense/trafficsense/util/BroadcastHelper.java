package fi.aalto.trafficsense.trafficsense.util;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

/**
 * Created by mikko.rinne@aalto.fi on 17/04/16.
 */
public class BroadcastHelper {

    // Broadcaster
    public static void simpleBroadcast(LocalBroadcastManager manager, String messageType) {
        if (manager != null)
        {
            Intent intent = new Intent(messageType);
            manager.sendBroadcast(intent);
        }
    }

}
