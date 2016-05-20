package fi.aalto.trafficsense.trafficsense.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import timber.log.Timber;

/**
 * Created by mikko.rinne@aalto.fi on 14/12/15.
 */
public class TSBootReceiver extends BroadcastReceiver
{

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)){
            Timber.d("TrafficSense received boot completed.");
        }
    }

}