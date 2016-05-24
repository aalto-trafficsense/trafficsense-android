package fi.aalto.trafficsense.trafficsense;

import android.app.ActivityManager;
import android.app.Application;
import android.content.*;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.preference.PreferenceManager;
import fi.aalto.trafficsense.trafficsense.backend.TrafficSenseService;
import fi.aalto.trafficsense.trafficsense.util.InternalBroadcasts;
import fi.aalto.trafficsense.trafficsense.util.LocalBinderServiceConnection;
import fi.aalto.trafficsense.trafficsense.util.TSServiceState;
import timber.log.Timber;

import java.util.Locale;

import static android.support.v7.preference.PreferenceManager.getDefaultSharedPreferences;
import static fi.aalto.trafficsense.trafficsense.util.InternalBroadcasts.LABEL_STATE_INDEX;
import static fi.aalto.trafficsense.trafficsense.util.TSServiceState.*;

/**
 * Created by mikko.rinne@aalto.fi on 06/04/16.
 */
public class TrafficSenseApplication extends Application {

    private static SharedPreferences mSettings;
    private static Resources mRes;

    private static Context mContext;
    private static TrafficSenseService mTSService;
    private TSServiceState mTSServiceState; // Keep track of "STOPPED" state, since the service cannot respond to that
    private BroadcastReceiver mBroadcastReceiver;
    private LocalBroadcastManager mLocalBroadcastManager;

    private static Configuration configDefault = new Configuration();
    private static Configuration configStadi = new Configuration();

    @Override
    public void onCreate() {
        // Configure Timber to log only for debug builds
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
        super.onCreate();
        mContext = this;

        mRes = getContext().getResources();
        mSettings = getDefaultSharedPreferences(getContext());

//        Timber.d("--- TrafficSenseApplication pre-def sees activitysensor interval as: %d", mSettings.getInt(mRes.getString(R.string.debug_settings_activity_interval_key), -1));

        // Load preferences from xml on the first execution
        PreferenceManager.setDefaultValues(this, R.xml.debug_settings, false);

//        Timber.d("--- TrafficSenseApplication post-def sees activitysensor interval as: %d", mSettings.getInt(mRes.getString(R.string.debug_settings_activity_interval_key), -1));

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        initBroadcastReceiver();

        boolean serviceSwitchedOn = mSettings.getBoolean(mRes.getString(R.string.debug_settings_service_running_key), false);

        if (isMyServiceRunning(TrafficSenseService.class)) {
            // Already running
            if (serviceSwitchedOn) { // Should be running - bind to it
                Intent serviceIntent = new Intent(this, TrafficSenseService.class);
                bindService(serviceIntent, mServiceConnection, BIND_AUTO_CREATE);
                Timber.d("Application started - TSService already running.");
            } else { // Switched off but running?!?! - not sure when this case could occur, but stop it
                stopTSService();
            }
        } else { // Not yet running
            if (serviceSwitchedOn) { // Does the user want it to run?
                startTSService();
            }
        }

        // Configure locales
        configDefault.locale = Locale.getDefault();
        configStadi.locale = new Locale("fi", "HI");

        setStadi(mSettings.getBoolean(mRes.getString(R.string.settings_locale_stadi_key), false));
    }

    public static Context getContext() {
        return mContext;
    }

    public static void setStadi(boolean b) {
        if (b) {
            mContext.getResources().updateConfiguration(configStadi, null);
       } else {
            mContext.getResources().updateConfiguration(configDefault, null);
        }
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putBoolean(mRes.getString(R.string.settings_locale_stadi_key), b);
        editor.apply();
    }
    /******************************
     * TrafficSense Service Handler
     ******************************/

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

    public void startTSService() {
        mTSServiceState = STARTING;
        updateServiceState();
        Intent serviceIntent = new Intent(this, TrafficSenseService.class);
        startService(serviceIntent);
        bindService(serviceIntent, mServiceConnection, BIND_AUTO_CREATE | BIND_IMPORTANT);
        mTSServiceState = RUNNING;
    }

    public void stopTSService() {
        mTSServiceState = STOPPING;
        updateServiceState();
        Intent serviceIntent = new Intent(this, TrafficSenseService.class);
        // MJR: Even though the order of stop and unbind looks reversed, the following is said to result in more
        // stable behavior: http://stackoverflow.com/questions/3385554/do-i-need-to-call-both-unbindservice-and-stopservice-for-android-services
        stopService(serviceIntent);
        unbindService(mServiceConnection);
        mTSServiceState = STOPPED;
        updateServiceState();
    }

    /* Private Members */
    private final ServiceConnection mServiceConnection = new LocalBinderServiceConnection<TrafficSenseService>() {
        @Override
        protected void onService(TrafficSenseService service) {
            mTSService = service;
        }
    };

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
                        startTSService();
                        break;
                    case InternalBroadcasts.KEY_SERVICE_STOP:
                        if (isMyServiceRunning(TrafficSenseService.class)) stopTSService();
                        break;
                    case InternalBroadcasts.KEY_DEBUG_SETTINGS_REQ:
                    case InternalBroadcasts.KEY_DEBUG_SHOW_REQ:
                    case InternalBroadcasts.KEY_MAIN_ACTIVITY_REQ:
                        if (mTSServiceState == STOPPED) updateServiceState();
                        break;
                    case Intent.ACTION_BOOT_COMPLETED:
                        Timber.d("TrafficSenseApplication received boot completed");
                        break;
                }
            }
        };

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(InternalBroadcasts.KEY_SERVICE_START);
        intentFilter.addAction(InternalBroadcasts.KEY_SERVICE_STOP);
        intentFilter.addAction(InternalBroadcasts.KEY_DEBUG_SETTINGS_REQ);
        intentFilter.addAction(InternalBroadcasts.KEY_DEBUG_SHOW_REQ);
        intentFilter.addAction(InternalBroadcasts.KEY_MAIN_ACTIVITY_REQ);

        if (mLocalBroadcastManager != null) {
            mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, intentFilter);
        }
    }

    // Update service state
    private void updateServiceState() {
        if (mLocalBroadcastManager != null)
        {
            Intent intent = new Intent(InternalBroadcasts.KEY_SERVICE_STATE_UPDATE);
            Bundle args = new Bundle();
            args.putInt(LABEL_STATE_INDEX,mTSServiceState.ordinal());
            intent.putExtras(args);
            mLocalBroadcastManager.sendBroadcast(intent);
        }
    }

}
