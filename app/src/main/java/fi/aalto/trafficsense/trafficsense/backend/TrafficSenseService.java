package fi.aalto.trafficsense.trafficsense.backend;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import com.google.common.base.Optional;
import fi.aalto.trafficsense.trafficsense.R;
import fi.aalto.trafficsense.trafficsense.backend.backend_util.PlayServiceInterface;
import fi.aalto.trafficsense.trafficsense.backend.uploader.RegularRoutesPipeline;
import fi.aalto.trafficsense.trafficsense.ui.MainActivity;
import fi.aalto.trafficsense.trafficsense.util.*;

import java.util.concurrent.atomic.AtomicReference;

import static fi.aalto.trafficsense.trafficsense.util.InternalBroadcasts.LABEL_STATE_INDEX;
import static fi.aalto.trafficsense.trafficsense.util.TSServiceState.RUNNING;
import static fi.aalto.trafficsense.trafficsense.util.TSUploadState.*;

/**
 * Created by mikko.rinne@aalto.fi on 06/04/16.
 */

public class TrafficSenseService extends Service {

    private final int foregroundCheckDelay = 5000; // milliseconds, delay after view inactive until checking, whether to go foreground

    /* Private Members */
    private static Context mContext;
    private final IBinder mBinder = new LocalBinder<TrafficSenseService>(this);
    private PlayServiceInterface mPlayServiceInterface;
    private static RegularRoutesPipeline mPipeline;
    private static TSServiceState mServiceState;
    private static TSUploadState mUploadState;
    private static LocalBroadcastManager mLocalBroadcastManager;
    private BroadcastReceiver mBroadcastReceiver;
    private Resources mRes;
    private Handler mHandler = new Handler();
    private BackendStorage mStorage;
    private AtomicReference<Boolean> mClientNumberFetchOngoing = new AtomicReference<>(false);

    private static boolean viewActive = false;
    private boolean isForeground = false;
    private boolean uploadInProgress = false;

    private final int ONGOING_NOTIFICATION_ID = 1212;

    // Delayed check for going foreground:
    private final Runnable mDelayedForegroundCheck = new Runnable() {
        @Override
        public void run() {
            checkForeground();
        }
    };


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
        mRes = this.getResources();

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
        if (!mStorage.isUserIdAvailable()) {
            RegularRoutesPipeline.setUploadEnabledState(false);
            if (mLocalBroadcastManager!=null)
            {
                Intent i = new Intent(InternalBroadcasts.KEY_REQUEST_SIGN_IN);
                mLocalBroadcastManager.sendBroadcast(i);
            }

        }  else {
            obtainClientNumber();
        }

        testUploadEnabled();
        return mBinder;
    }

    private void testUploadEnabled() {
        if (mStorage.isUserIdAvailable() && mStorage.isClientNumberAvailable()) {
            RegularRoutesPipeline.setUploadEnabledState(true);
            updateUploadState(READY);
        } else {
            RegularRoutesPipeline.setUploadEnabledState(false);
            TSUploadState testState = SWITCHEDOFF;
            if (!mStorage.isUserIdAvailable()) testState=SIGNEDOUT;
            if (!mStorage.isClientNumberAvailable()) testState=NOCLIENTNUMBER;
            updateUploadState(testState);
        }
    }

    private void obtainClientNumber() {
        if (mClientNumberFetchOngoing.get()) {
            return;
        }

        // The following value is cleared based on local broadcast message
        mClientNumberFetchOngoing.set(true);

        fetchClientNumber(new Callback<Optional<Integer>>() {
            @Override
            public void run(Optional<Integer> result, RuntimeException error) {
                if (result.isPresent()) mStorage.writeClientNumber(result.get());
            }
        });

    }

    private void fetchClientNumber(Callback<Optional<Integer>> callback) {
        RegularRoutesPipeline.fetchClientNumber(callback);
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

    private void checkForeground() {
        // If nobody is still watching, go foreground
        if (!isViewActive()) {
            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
            Notification notification = builder.setContentIntent(pendingIntent)
                    .setSmallIcon(R.drawable.md_activity_still)
                    .setTicker("ticker text here")
                    .setWhen(System.currentTimeMillis())
                    .setContentTitle(getText(R.string.app_name))
                    .setContentText("content text shows here.").build();
            startForeground(ONGOING_NOTIFICATION_ID, notification);
            isForeground=true;
        }
    }

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
                    case InternalBroadcasts.KEY_DEBUG_SHOW_REQ:
                        updateUploadState(mUploadState);
                    case InternalBroadcasts.KEY_DEBUG_SETTINGS_REQ:
                    case InternalBroadcasts.KEY_MAIN_ACTIVITY_REQ:
                        updateServiceState(mServiceState);
                        break;
                    case InternalBroadcasts.KEY_VIEW_RESUMED:
                        viewActive = true;
                        if (isForeground) {
                            stopForeground(true);
                            isForeground = false;
                        }
                        break;
                    case InternalBroadcasts.KEY_VIEW_PAUSED:
                        viewActive = false;
                        mHandler.postDelayed(mDelayedForegroundCheck, foregroundCheckDelay);
                        break;
                    case InternalBroadcasts.KEY_CLIENT_NUMBER_FETCH_COMPLETED:
                        mClientNumberFetchOngoing.set(false);
                        testUploadEnabled();
                        break;
                    case InternalBroadcasts.KEY_REGISTRATION_SUCCEEDED:
                    case InternalBroadcasts.KEY_AUTHENTICATION_SUCCEEDED:
                        if (!mStorage.isClientNumberAvailable()) obtainClientNumber();
                        else testUploadEnabled();
                        break;
                    case InternalBroadcasts.KEY_AUTHENTICATION_FAILED:
                    case InternalBroadcasts.KEY_USER_ID_CLEARED:
                        testUploadEnabled();
                        break;
                    case InternalBroadcasts.KEY_UPLOAD_STARTED:
                        uploadInProgress = true;
                        updateUploadState(TSUploadState.INPROGRESS);
                        break;
                    case InternalBroadcasts.KEY_UPLOAD_SUCCEEDED:
                        uploadInProgress = false;
                        testUploadEnabled();
                        break;
                }
            }
        };

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(InternalBroadcasts.KEY_DEBUG_SETTINGS_REQ);
        intentFilter.addAction(InternalBroadcasts.KEY_DEBUG_SHOW_REQ);
        intentFilter.addAction(InternalBroadcasts.KEY_MAIN_ACTIVITY_REQ);
        intentFilter.addAction(InternalBroadcasts.KEY_VIEW_RESUMED);
        intentFilter.addAction(InternalBroadcasts.KEY_VIEW_PAUSED);
        intentFilter.addAction(InternalBroadcasts.KEY_CLIENT_NUMBER_FETCH_COMPLETED);
        intentFilter.addAction(InternalBroadcasts.KEY_REGISTRATION_SUCCEEDED);
        intentFilter.addAction(InternalBroadcasts.KEY_AUTHENTICATION_SUCCEEDED);
        intentFilter.addAction(InternalBroadcasts.KEY_AUTHENTICATION_FAILED);
        intentFilter.addAction(InternalBroadcasts.KEY_USER_ID_CLEARED);
        intentFilter.addAction(InternalBroadcasts.KEY_UPLOAD_STARTED);
        intentFilter.addAction(InternalBroadcasts.KEY_UPLOAD_SUCCEEDED);


        if (mLocalBroadcastManager != null) {
            mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, intentFilter);
        }
    }

    // Update service state
    private static void updateServiceState(TSServiceState newState) {
        mServiceState = newState;
        if (mLocalBroadcastManager!=null && isViewActive())
        {
            Bundle args = new Bundle();
            args.putInt(LABEL_STATE_INDEX,newState.ordinal());
            broadcastNewState(InternalBroadcasts.KEY_SERVICE_STATE_UPDATE, args);
        }
    }

    // Update upload state
    private static void updateUploadState(TSUploadState newState) {
        mUploadState = newState;
        if (mLocalBroadcastManager!=null && isViewActive())
        {
            Bundle args = new Bundle();
            args.putInt(LABEL_STATE_INDEX,newState.ordinal());
            broadcastNewState(InternalBroadcasts.KEY_UPLOAD_STATE_UPDATE, args);

        }
    }

    // Broadcast new state
    private static void broadcastNewState(String msg, Bundle args) {
            Intent intent = new Intent(msg);
            intent.putExtras(args);
            mLocalBroadcastManager.sendBroadcast(intent);
    }

}

