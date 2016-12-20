package fi.aalto.trafficsense.trafficsense.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.messaging.FirebaseMessaging;
import fi.aalto.trafficsense.trafficsense.TrafficSenseApplication;
import fi.aalto.trafficsense.trafficsense.backend.backend_util.ServerNotification;
import fi.aalto.trafficsense.trafficsense.util.InternalBroadcasts;

/**
 * Created by mikko.rinne@aalto.fi on 15/05/16.
 *
 * 11.10.2016: Extended to handle play service connectivity and updates
 * 16.12.2016: Added registration to Firebase broadcast topics.
 */
public class SplashActivity extends AppCompatActivity {

    private LocalBroadcastManager mLocalBroadcastManager;
    private BroadcastReceiver mBroadcastReceiver;
    private boolean mRequestLogin = false;

    public static final int REQUEST_GOOGLE_PLAY_SERVICES = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        initBroadcastReceiver();

        testPlayServiceInterface();

        // openActivity(MainActivity.class);
    }

    @Override
    public void onDestroy() {
        mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }

    /*************************
     *
     * Broadcast handler
     *
     *************************/

    /* Local broadcast receiver */
    private void initBroadcastReceiver() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                switch (action) {
                    case InternalBroadcasts.KEY_REQUEST_SIGN_IN:
                        mRequestLogin = true; // Remember the sign-in request, main activity may not be up yet
                        break;
                    case InternalBroadcasts.KEY_MAIN_ACTIVITY_REQ:
                        // Main activity is up and running
                        if (mRequestLogin) openActivity(LoginActivity.class);
                        finish();
                        break;
                }
            }
        };

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(InternalBroadcasts.KEY_REQUEST_SIGN_IN);
        intentFilter.addAction(InternalBroadcasts.KEY_MAIN_ACTIVITY_REQ);

        if (mLocalBroadcastManager != null) {
            mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, intentFilter);
        }
    }

    private void openActivity(Class c) {
        Intent intent = new Intent(this, c);
        startActivity(intent);
    }

    /***************************
     *
     * Google Play Services code
     *
     ***************************/

    private void testPlayServiceInterface() {
        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        int code = api.isGooglePlayServicesAvailable(this);
        if (code == ConnectionResult.SUCCESS) {
            onActivityResult(REQUEST_GOOGLE_PLAY_SERVICES, Activity.RESULT_OK, null);
        } else if (api.isUserResolvableError(code) &&
                api.showErrorDialogFragment(this, code, REQUEST_GOOGLE_PLAY_SERVICES)) {
            // wait for onActivityResult call (see below)
        } else {
            Toast.makeText(this, api.getErrorString(code), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode == Activity.RESULT_OK) { // Play services fine
                    // Register Firebase Messaging broadcast topics
                    FirebaseMessaging fm = FirebaseMessaging.getInstance();
                    fm.subscribeToTopic(ServerNotification.FB_TOPIC_SURVEY);
                    fm.subscribeToTopic(ServerNotification.FB_TOPIC_BROADCAST);
                    // Proceed to main activity
                    openActivity(MainActivity.class);
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }
}