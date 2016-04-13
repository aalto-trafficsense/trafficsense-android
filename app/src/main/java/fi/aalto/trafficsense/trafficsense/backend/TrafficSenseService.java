package fi.aalto.trafficsense.trafficsense.backend;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import fi.aalto.trafficsense.trafficsense.backend.backend_util.PlayServiceInterface;
import fi.aalto.trafficsense.trafficsense.util.InternalBroadcasts;
import fi.aalto.trafficsense.trafficsense.util.LocalBinder;
import fi.aalto.trafficsense.trafficsense.util.TSServiceState;
import timber.log.Timber;

import static android.app.Activity.RESULT_OK;
import static fi.aalto.trafficsense.trafficsense.util.InternalBroadcasts.LABEL_SERVICE_STATE_INDEX;
import static fi.aalto.trafficsense.trafficsense.util.TSServiceState.RUNNING;
import static fi.aalto.trafficsense.trafficsense.util.TSServiceState.SLEEPING;

public class TrafficSenseService extends Service {

    /* Private Members */
    private final IBinder mBinder = new LocalBinder<TrafficSenseService>(this);
    private PlayServiceInterface mPlayServiceInterface;
    private static TSServiceState mServiceState;
    private static LocalBroadcastManager mLocalBroadcastManager;
    private BroadcastReceiver mBroadcastReceiver;
    private static boolean viewActive = false;


    /* Overridden Methods */
    @Override
    public void onCreate() {
        super.onCreate();
        mPlayServiceInterface = new PlayServiceInterface(this);
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        initBroadcastReceiver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateServiceState(RUNNING);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        viewActive = true; // If someone binds, there is likely an active view
        return mBinder;
    }

    @Override
    public void onDestroy() {
        mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
        mPlayServiceInterface.disconnect();
        super.onDestroy();
    }

    public static boolean isViewActive() { return viewActive; }

    public static void setServiceState(TSServiceState newState) {
        if (newState != mServiceState) updateServiceState(newState);
    }

    public static TSServiceState getServiceState() { return mServiceState; }

    /*************************
     Broadcast handler
     *************************/

    /* Local broadcast receiver */
    private void initBroadcastReceiver() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                switch (action) {
                    case InternalBroadcasts.KEY_DEBUG_SETTINGS_REQ:
                    case InternalBroadcasts.KEY_DEBUG_SHOW_REQ:
                        updateServiceState(mServiceState);
                        break;
                    case InternalBroadcasts.KEY_VIEW_RESUMED:
                        viewActive = true;
                        break;
                    case InternalBroadcasts.KEY_VIEW_PAUSED:
                        viewActive = false;
                        break;

                }
            }
        };

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(InternalBroadcasts.KEY_DEBUG_SETTINGS_REQ);
        intentFilter.addAction(InternalBroadcasts.KEY_DEBUG_SHOW_REQ);
        intentFilter.addAction(InternalBroadcasts.KEY_VIEW_RESUMED);
        intentFilter.addAction(InternalBroadcasts.KEY_VIEW_PAUSED);

        if (mLocalBroadcastManager != null) {
            mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, intentFilter);
        }
    }

    // Update service state
    private static void updateServiceState(TSServiceState newState) {
        mServiceState = newState;
        if (mLocalBroadcastManager!=null && isViewActive())
        {
            Intent intent = new Intent(InternalBroadcasts.KEY_SERVICE_STATE_UPDATE);
            Bundle args = new Bundle();
            args.putInt(LABEL_SERVICE_STATE_INDEX,newState.ordinal());
            intent.putExtras(args);
            mLocalBroadcastManager.sendBroadcast(intent);
        }
    }

}

