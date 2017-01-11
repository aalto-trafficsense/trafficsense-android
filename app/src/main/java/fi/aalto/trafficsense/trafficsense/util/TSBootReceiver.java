package fi.aalto.trafficsense.trafficsense.util;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import fi.aalto.trafficsense.trafficsense.backend.backend_util.ServerNotification;
import timber.log.Timber;

/**
 * Created by mikko.rinne@aalto.fi on 14/12/15.
 */
public class TSBootReceiver extends BroadcastReceiver
{

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if(action.equals(Intent.ACTION_BOOT_COMPLETED)){
            Timber.d("TrafficSense received boot completed.");
        } else if (action.equals(ServerNotification.PTP_ALERT_END_ACTION)) {
            int id = intent.getIntExtra("notification_id", -1);
            if (id != -1) {
                Timber.d("onReceive cancelling notification: %d", id);
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel(id);
            }

        }
    }

}