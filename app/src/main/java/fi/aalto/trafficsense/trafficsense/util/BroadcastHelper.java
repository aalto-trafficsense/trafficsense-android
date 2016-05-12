package fi.aalto.trafficsense.trafficsense.util;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

/**
 * Created by mikko.rinne@aalto.fi on 17/04/16.
 */
public class BroadcastHelper {

    // Simple Broadcaster to send only the intent with no content
    public static void simpleBroadcast(LocalBroadcastManager lbm, String messageType) {
        if (lbm != null)
        {
            Intent intent = new Intent(messageType);
            lbm.sendBroadcast(intent);
        }
    }

    // Update (viewing) activity status to service
    public static void broadcastViewResumed(LocalBroadcastManager lbm, boolean resumed) {
        if (lbm != null)
        {
            String key;
            if (resumed) key = InternalBroadcasts.KEY_VIEW_RESUMED;
            else key = InternalBroadcasts.KEY_VIEW_PAUSED;
            Intent intent = new Intent(key);
            lbm.sendBroadcast(intent);
        }
    }


}
