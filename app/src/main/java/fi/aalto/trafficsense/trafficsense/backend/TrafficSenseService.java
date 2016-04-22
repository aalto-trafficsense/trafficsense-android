package fi.aalto.trafficsense.trafficsense.backend;

import android.app.Notification;
import android.app.NotificationManager;
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

import static fi.aalto.trafficsense.trafficsense.util.ActivityType.STILL;
import static fi.aalto.trafficsense.trafficsense.util.InternalBroadcasts.LABEL_STATE_INDEX;
import static fi.aalto.trafficsense.trafficsense.util.TSServiceState.RUNNING;
import static fi.aalto.trafficsense.trafficsense.util.TSServiceState.SLEEPING;
import static fi.aalto.trafficsense.trafficsense.util.TSServiceState.getServiceStateString;
import static fi.aalto.trafficsense.trafficsense.util.TSUploadState.*;

/**
 * Created by mikko.rinne@aalto.fi on 06/04/16.
 */

public class TrafficSenseService extends Service {

//    private final int foregroundCheckDelay = 5000; // milliseconds, delay after view inactive until checking, whether to go foreground
//    private final int foregroundBootCheckDelay = 15000; // milliseconds, delay after servicestart until checking, whether to go foreground
                                                        // Activates the notification, if service is starting after a reboot
    /* Private Members */
    private static Context mContext;
    private final IBinder mBinder = new LocalBinder<TrafficSenseService>(this);
    private PlayServiceInterface mPlayServiceInterface;
    private static RegularRoutesPipeline mPipeline;
    private static TSServiceState mServiceState;
    private static TSUploadState mUploadState;
    private static LocalBroadcastManager mLocalBroadcastManager;
    private BroadcastReceiver mBroadcastReceiver;
    private NotificationManager mNotificationManager;
    private Resources mRes;
    private Handler mHandler = new Handler();
    private static BackendStorage mStorage;
    private AtomicReference<Boolean> mClientNumberFetchOngoing = new AtomicReference<>(false);

    private static boolean viewActive = false;
//    private static boolean isForeground = false;
//    private boolean uploadInProgress = false;
//    private boolean uploadFailed = false;

    private ActivityType mPreviousActivity = STILL;

    private static final int ONGOING_NOTIFICATION_ID = 1212; // Not zero, pulled from sleeve

    // Delayed check for going foreground:
//    private final Runnable mDelayedForegroundCheck = new Runnable() {
//        @Override
//        public void run() {
//            checkForeground();
//        }
//    };


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
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateServiceState(RUNNING);
        startForeground(ONGOING_NOTIFICATION_ID, buildServiceStateNotification());
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // viewActive = true; // If someone binds, there is likely an active view
//        mHandler.postDelayed(mDelayedForegroundCheck, foregroundBootCheckDelay);

        // Request sign-in if user-id is not available
        if (!mStorage.isUserIdAvailable()) {
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
            updateUploadState(READY);
        } else {
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
        stopForeground(true);
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
        if (newState != mServiceState) {
            updateServiceState(newState);
            if (newState==SLEEPING) { //  && isForeground
                NotificationManager nM = (NotificationManager) TrafficSenseService.getContext().getSystemService(NOTIFICATION_SERVICE);
                nM.notify(ONGOING_NOTIFICATION_ID, buildServiceStateNotification());
            }
        }
    }

    public static TSServiceState getServiceState() { return mServiceState; }

//    private void checkForeground() {
//        // If nobody is watching after the delay, go foreground
//        if (!isViewActive()) {
//            if (TrafficSenseService.getServiceState() == SLEEPING) {
//                startForeground(ONGOING_NOTIFICATION_ID, buildSleepNotification());
//                mPreviousActivity = STILL; // Prevent replacing sleep notification with still
//            } else {
//                startForeground(ONGOING_NOTIFICATION_ID, buildActivityNotification(mPreviousActivity));
//            }
//            isForeground=true;
//        }
//    }

    private Notification buildActivityNotification(ActivityType act) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        Notification notification = builder.setContentIntent(pendingIntent)
                .setSmallIcon(ActivityType.getActivityIcon(act))
                .setWhen(System.currentTimeMillis())
                .setContentTitle(getText(R.string.app_name))
                .setContentText(ActivityType.getActivityString(act)).build();
        return notification;
    }

    private static Notification buildServiceStateNotification() {
        Context ctx = TrafficSenseService.getContext();
        Intent notificationIntent = new Intent(ctx, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(ctx, 0, notificationIntent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx);
        Notification notification = builder.setContentIntent(pendingIntent)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(ctx.getText(R.string.app_name))
                .setContentText(getServiceStateString(getServiceState())).build();
        return notification;
    }

    /*************************
     Broadcast handlers
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
//                        if (isForeground) {
//                            stopForeground(true);
//                            isForeground = false;
//                        }
                        break;
                    case InternalBroadcasts.KEY_VIEW_PAUSED:
                        viewActive = false;
//                        mHandler.postDelayed(mDelayedForegroundCheck, foregroundCheckDelay);
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
                        updateUploadState(TSUploadState.INPROGRESS);
                        break;
                    case InternalBroadcasts.KEY_UPLOAD_SUCCEEDED:
                        updateUploadState(TSUploadState.READY);
//                        uploadInProgress = false;
//                        uploadFailed = false;
//                        testUploadEnabled();
                        break;
                    case InternalBroadcasts.KEY_UPLOAD_FAILED:
                        updateUploadState(TSUploadState.FAILED);
//                        testUploadEnabled();
                        break;
                    case InternalBroadcasts.KEY_ACTIVITY_UPDATE:
//                        if (isForeground) updateActivity(intent);
                        updateActivity(intent);
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
        intentFilter.addAction(InternalBroadcasts.KEY_UPLOAD_FAILED);
        intentFilter.addAction(InternalBroadcasts.KEY_ACTIVITY_UPDATE);

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
            args.putInt(InternalBroadcasts.LABEL_STATE_INDEX,newState.ordinal());
            if (mStorage.isClientNumberAvailable()) {
                args.putInt(InternalBroadcasts.LABEL_CLIENT_NUMBER,mStorage.readClientNumber().get());
            }
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

    private void updateActivity (Intent i) {
        if (i.hasExtra(InternalBroadcasts.KEY_ACTIVITY_UPDATE)) {
            ActivityData a = i.getParcelableExtra(InternalBroadcasts.KEY_ACTIVITY_UPDATE);
            ActivityType topActivity=a.getFirst().Type;
            if (ActivityType.getGood().contains(topActivity)) { // a good activity?
                if (mPreviousActivity != topActivity) { // different from previous notification?
                    mNotificationManager.notify(ONGOING_NOTIFICATION_ID, buildActivityNotification(topActivity));
                    mPreviousActivity = topActivity;
                }
            }
        }
    }
}

