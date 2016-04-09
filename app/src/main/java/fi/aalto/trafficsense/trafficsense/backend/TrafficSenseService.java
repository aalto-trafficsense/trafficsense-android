package fi.aalto.trafficsense.trafficsense.backend;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import fi.aalto.trafficsense.trafficsense.backend.backend_util.PlayServiceInterface;
import fi.aalto.trafficsense.trafficsense.util.LocalBinder;

import static android.app.Activity.RESULT_OK;

public class TrafficSenseService extends Service {

    /* Private Members */
    private final IBinder mBinder = new LocalBinder<TrafficSenseService>(this);
    private PlayServiceInterface mPlayServiceInterface;

    /* Overridden Methods */

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mPlayServiceInterface = new PlayServiceInterface(this);
        mPlayServiceInterface.initialize();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public void onDestroy() {
        mPlayServiceInterface.disconnect();
        super.onDestroy();
    }

}

