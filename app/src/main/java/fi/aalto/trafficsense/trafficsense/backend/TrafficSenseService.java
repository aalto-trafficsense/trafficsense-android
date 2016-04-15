package fi.aalto.trafficsense.trafficsense.backend;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import com.google.common.base.Optional;
import fi.aalto.trafficsense.trafficsense.backend.backend_util.PlayServiceInterface;
import fi.aalto.trafficsense.trafficsense.backend.uploader.RegularRoutesPipeline;
import fi.aalto.trafficsense.trafficsense.util.*;
import timber.log.Timber;

import static android.app.Activity.RESULT_OK;
import static fi.aalto.trafficsense.trafficsense.util.InternalBroadcasts.LABEL_SERVICE_STATE_INDEX;
import static fi.aalto.trafficsense.trafficsense.util.TSServiceState.RUNNING;
import static fi.aalto.trafficsense.trafficsense.util.TSServiceState.SLEEPING;

/**
 * Created by mikko.rinne@aalto.fi on 06/04/16.
 */

public class TrafficSenseService extends Service {

    /* Private Members */
    private static Context mContext;
    private final IBinder mBinder = new LocalBinder<TrafficSenseService>(this);
    private PlayServiceInterface mPlayServiceInterface;
    private static RegularRoutesPipeline mPipeline;
    private static TSServiceState mServiceState;
    private static LocalBroadcastManager mLocalBroadcastManager;
    private BroadcastReceiver mBroadcastReceiver;
    private BackendStorage mStorage;

    private static boolean viewActive = false;


    /* Overridden Methods */
    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        mPipeline = new RegularRoutesPipeline();
        // PlayServiceInterface -> SensorController -> SensorFilter requires pipeline to be set up.
        mPlayServiceInterface = new PlayServiceInterface();
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        initBroadcastReceiver();
        mStorage = BackendStorage.create(mContext);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateServiceState(RUNNING);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        viewActive = true; // If someone binds, there is likely an active view

        // Request sign-in if user-id is not available
        if (!mStorage.isClientNumberAvailable()) {
            if (mLocalBroadcastManager!=null)
            {
                Intent i = new Intent(InternalBroadcasts.KEY_REQUEST_SIGN_IN);
                mLocalBroadcastManager.sendBroadcast(i);
            }

        }
        return mBinder;
    }

    @Override
    public void onDestroy() {
        mPipeline.flushDataQueueToServer();
        mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
        mPlayServiceInterface.disconnect();
        mPipeline.disconnect();
        super.onDestroy();
    }

    public static Context getContext() {
        return mContext;
    }

    public static RegularRoutesPipeline getPipeline() {
        return mPipeline;
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

