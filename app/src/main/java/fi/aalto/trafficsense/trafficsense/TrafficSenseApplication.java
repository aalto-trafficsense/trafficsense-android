package fi.aalto.trafficsense.trafficsense;

import android.app.ActivityManager;
import android.app.Application;
import android.content.*;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import fi.aalto.trafficsense.trafficsense.backend.TrafficSenseService;
import fi.aalto.trafficsense.trafficsense.util.InternalBroadcasts;
import fi.aalto.trafficsense.trafficsense.util.LocalBinderServiceConnection;
import fi.aalto.trafficsense.trafficsense.util.TSServiceState;
import timber.log.Timber;

import static fi.aalto.trafficsense.trafficsense.util.InternalBroadcasts.LABEL_SERVICE_STATE_INDEX;
import static fi.aalto.trafficsense.trafficsense.util.TSServiceState.*;

/**
 * Created by mikko.rinne@aalto.fi on 06/04/16.
 */
public class TrafficSenseApplication extends Application {

    private static TrafficSenseService mTSService;
    private static TSServiceState mServiceState = STOPPED;
    private BroadcastReceiver mBroadcastReceiver;
    private LocalBroadcastManager mLocalBroadcastManager;


    @Override
    public void onCreate() {
        // Configure Timber to log only for debug builds
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
        super.onCreate();

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        initBroadcastReceiver();

        // Start TrafficSenseService, if not already running
        if (!isMyServiceRunning(TrafficSenseService.class)) {
            if (startTSService()) {
                Timber.d("TrafficSenseService started");
            } else {
                Timber.d("TrafficSenseService start failed.");
            }
        } else {
            updateServiceState(RUNNING);
            Timber.d("Application started - TSService already running.");
        }

        Timber.d("Application started");
    }

    /******************************
     * TrafficSense Service Handler
     ******************************/

    public TSServiceState getTSServiceState() { return mServiceState; }

    // From: http://stackoverflow.com/questions/600207/how-to-check-if-a-service-is-running-on-android/5921190#5921190
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public boolean startTSService() {
        boolean started = false;
        switch (mServiceState) {
            case STARTING:
                Timber.d("startTSService called when service already starting (double start call)");
                break;
            case RUNNING:
            case SLEEPING:
                Timber.d("startTSService called when service is already running");
                break;
            case STOPPING:
                Timber.d("startTSService called during service stopping");
                break;
            case STOPPED:
                updateServiceState(STARTING);
                Intent serviceIntent = new Intent(this, TrafficSenseService.class);
                startService(serviceIntent);
                bindService(serviceIntent, mServiceConnection, BIND_AUTO_CREATE);
                started = true;
                break;
        }
        return started;
    }

    public boolean stopTSService() {
        boolean stopped = false;
        switch (mServiceState) {
            case STARTING:
                Timber.d("stopTSService called when service is starting");
                break;
            case RUNNING:
            case SLEEPING:
                updateServiceState(STOPPING);
                Intent serviceIntent = new Intent(this, TrafficSenseService.class);
                // MJR: Even though the order of stop and unbind looks reversed, the following results in more
                // stable behavior: http://stackoverflow.com/questions/3385554/do-i-need-to-call-both-unbindservice-and-stopservice-for-android-services
                stopService(serviceIntent);
                unbindService(mServiceConnection);
                updateServiceState(STOPPED);
                stopped = true;
                break;
            case STOPPING:
                Timber.d("stopTSService called during service stopping (double call)");
                break;
            case STOPPED:
                Timber.d("stopTSService called when service already stopped (duplicate stop call)");
                break;
        }
        return stopped;
    }

    /* Private Members */
    private final ServiceConnection mServiceConnection = new LocalBinderServiceConnection<TrafficSenseService>() {
        @Override
        protected void onService(TrafficSenseService service) {
            mTSService = service;
            updateServiceState(RUNNING);
        }
    };


    /* Get Methods */

    // TODO: Check if this is really needed from anywhere else - assuming not in a clean architecture
    /*
    private TrafficSenseService getTSService() {
        return mTSService;
    }
    */

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
                    case InternalBroadcasts.KEY_SERVICE_START:
                        if (startTSService()) {
                            Timber.d("TSService started.");
                        };
                        break;
                    case InternalBroadcasts.KEY_SERVICE_STOP:
                        if (stopTSService()) {
                            Timber.d("TSService stopped.");
                        }
                        break;
                    case InternalBroadcasts.KEY_SERVICE_GOING_TO_SLEEP:
                        updateServiceState(SLEEPING);
                        break;
                    case InternalBroadcasts.KEY_SERVICE_WAKING_UP:
                        updateServiceState(RUNNING);
                        break;
                    case InternalBroadcasts.KEY_DEBUG_SETTINGS_REQ:
                        updateDebugSettings();
                        break;
                    case InternalBroadcasts.KEY_DEBUG_SHOW_REQ:
                        updateDebugShow();
                        break;
                }
            }
        };

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(InternalBroadcasts.KEY_SERVICE_START);
        intentFilter.addAction(InternalBroadcasts.KEY_SERVICE_STOP);
        intentFilter.addAction(InternalBroadcasts.KEY_SERVICE_GOING_TO_SLEEP);
        intentFilter.addAction(InternalBroadcasts.KEY_SERVICE_WAKING_UP);
        intentFilter.addAction(InternalBroadcasts.KEY_DEBUG_SETTINGS_REQ);
        intentFilter.addAction(InternalBroadcasts.KEY_DEBUG_SHOW_REQ);

        if (mLocalBroadcastManager != null) {
            mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, intentFilter);
        }
    }

    // Update service state
    private void updateServiceState(TSServiceState newState) {
        mServiceState = newState;
        if (mLocalBroadcastManager != null)
        {
            Intent intent = new Intent(InternalBroadcasts.KEY_SERVICE_STATE_UPDATE);
            Bundle args = new Bundle();
            args.putInt(LABEL_SERVICE_STATE_INDEX,newState.ordinal());
            intent.putExtras(args);
            mLocalBroadcastManager.sendBroadcast(intent);
        }
    }

    // Update values for Debug Settings
    private void updateDebugSettings () {
        if (mLocalBroadcastManager != null)
        {
            Intent intent = new Intent(InternalBroadcasts.KEY_DEBUG_SETTINGS);
            Bundle args = new Bundle();
            args.putInt(LABEL_SERVICE_STATE_INDEX,mServiceState.ordinal());
            intent.putExtras(args);
            mLocalBroadcastManager.sendBroadcast(intent);
        }
    }

    // Update values for Debug Show
    private void updateDebugShow () {
        if (mLocalBroadcastManager != null)
        {
            Intent intent = new Intent(InternalBroadcasts.KEY_DEBUG_SHOW);
            Bundle args = new Bundle();
            args.putInt(LABEL_SERVICE_STATE_INDEX,mServiceState.ordinal());
            intent.putExtras(args);
            mLocalBroadcastManager.sendBroadcast(intent);
        }
    }


}
