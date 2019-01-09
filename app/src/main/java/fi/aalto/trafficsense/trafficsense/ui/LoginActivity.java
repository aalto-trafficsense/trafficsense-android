package fi.aalto.trafficsense.trafficsense.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.concurrent.atomic.AtomicReference;

import fi.aalto.trafficsense.trafficsense.R;
import fi.aalto.trafficsense.trafficsense.TrafficSenseApplication;
import fi.aalto.trafficsense.trafficsense.backend.uploader.RegularRoutesPipeline;
import fi.aalto.trafficsense.trafficsense.util.BackendStorage;
import fi.aalto.trafficsense.trafficsense.util.Callback;
import fi.aalto.trafficsense.trafficsense.util.HashUtil;
import fi.aalto.trafficsense.trafficsense.util.InternalBroadcasts;
import timber.log.Timber;

/**
 * Google authentication
 * This activity follows the logic defined on:
 * - https://developers.google.com/identity/sign-in/android/offline-access
 **/

public class LoginActivity extends AppCompatActivity
        implements
            View.OnClickListener {

    /* Request code used to invoke sign in user interactions. */
    private static final int RC_SIGN_IN = 9001;

    /* States used in Google Authentication process (not covering LRR server call phase)
       to prevent multiple concurrent calls when process in already ongoing
    */
    private static final int STATE_DEFAULT = 0;
    private static final int STATE_SIGN_IN = 1;
    private static final int STATE_IN_PROGRESS = 2;
    private static final String KEY_SAVED_PROGRESS = "sign_in_progress";
    private static final String KEY_UPLOAD_ENABLED_STATE = "upload_enabled_state";
    private static final String TAG_ERR_DIALOG = "error_dialog";

    // UI references.
    private View mProgressView;
    private SignInButton mSignInButton;
    private Button mSignOutButton;
    private Button mRevokeButton;
    private Button mCancelButton;
    private Button mContinueButton;
    private TextView mStatus;
    private TextView mConnectionStatus;

    // Other members
    private final AtomicReference<Boolean> mAuthenticationOngoing = new AtomicReference<>(false);
    private LocalBroadcastManager mLocalBroadcastManager;

    private Resources mRes;

    /* Store the connection result from onConnectionFailed callbacks so that we can
     * resolve them when the user clicks sign-in. */
    private ConnectionResult mConnectionResult;

    // We use mSignInProgress to track whether user has clicked sign in.
    // mSignInProgress can be one of three values:
    //
    //       STATE_DEFAULT: The default state of the application before the user
    //                      has clicked 'sign in', or after they have clicked
    //                      'sign out'.  In this state we will not attempt to
    //                      resolve sign in errors and so will display our
    //                      Activity in a signed out state.
    //       STATE_SIGN_IN: This state indicates that the user has clicked 'sign
    //                      in', so resolve successive errors preventing sign in
    //                      until the user has successfully authorized an account
    //                      for our app.
    //   STATE_IN_PROGRESS: This state indicates that we have started an intent to
    //                      resolve an error, and so we should not start further
    //                      intents until the current intent completes.
    //                      NOTE: GoogleSignInClient resolves errors internally - this state is no longer called
    private int mSignInProgress;

    private AtomicReference<Boolean> mClearAccountTriggered = new AtomicReference<>(false);
    private GoogleSignInClient mGoogleSignInClient;
    private BackendStorage mStorage;
    private boolean mUploadEnabledState;

    private BroadcastReceiver mMessageReceiver;

    private final Callback<Boolean> mFetchAndSetAuthenticationTokensCallback = new Callback<Boolean>() {
        @Override
        public void run(Boolean result, RuntimeException error) {
            if (result) {
                // succeeded
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        setGoogleSignInProgress(STATE_DEFAULT);

                        // Return upload enabled state to previous state,
                        // Upload-operation will authenticate when required so no need to wait
                        RegularRoutesPipeline.setUploadEnabledState(mUploadEnabledState);

                        // Trigger RegularRoutes server authentication request:
                        Timber.d("Requesting LRR server authentication");
                        Intent intent = new Intent(InternalBroadcasts.KEY_REQUEST_AUTHENTICATION);
                        mLocalBroadcastManager.sendBroadcast(intent);
                    }

                });

            } else {
                // failed with error
                final String errText = error.getMessage();
                Timber.e("fetchAndSetAuthenticationTokens: " + errText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setGoogleSignInProgress(STATE_DEFAULT);
                        setStatusAsAuthenticationFailed();
                        showErrorInDialog(errText);
                    }
                });
            }
            mAuthenticationOngoing.set(false);
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TrafficSenseApplication.refreshStadi();

        mRes = this.getResources();

        setContentView(R.layout.activity_login);
        initMembers(savedInstanceState);
        if (!isSignedIn())
            // Turn off uploading while authenticating
            RegularRoutesPipeline.setUploadEnabledState(false);

        initBroadcastReceiver();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestServerAuthCode(getWebClientId(), true) // true = issue refresh_token every time - current server code expects it
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isSignedIn()) {
            mSignInButton.setEnabled(false);
            setStatusAsAuthenticated();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        TrafficSenseApplication.refreshStadi();
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
        // GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (isSignedIn()) {
            mSignInButton.setEnabled(false);
            setStatusAsAuthenticated();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SAVED_PROGRESS, mSignInProgress);
        outState.putBoolean(KEY_UPLOAD_ENABLED_STATE, mUploadEnabledState);
        if (mUploadEnabledState)
            mSignInButton.setEnabled(false);
    }

    @Override
    protected void onActivityResult(int requestCode, int responseCode, Intent intent) {
        super.onActivityResult(requestCode, responseCode, intent);

        if (requestCode == RC_SIGN_IN) {
            setGoogleSignInProgress(STATE_SIGN_IN);
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(intent);
            handleSignInResult(task);
        }

    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                // Signed in successfully, show authenticated UI.
                setUiControlStates(true);

                if (isSignedIn()) {
                    // No need to get device authentication id
                    doSignInActions();
                    return;
                }

                if (mAuthenticationOngoing.get())
                    return;

                mAuthenticationOngoing.set(true);
                // Indicate that the sign in process is complete.
                mStatus.setText(getResources().getString(R.string.status_fetch_auth_tokens));
                Timber.i("Login: onConnected");
                mStorage.writeOneTimeTokenAndUserId(account.getServerAuthCode(), HashUtil.getHashStringFromId(account.getId()));
                mFetchAndSetAuthenticationTokensCallback.run(true, null);
            }
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Timber.w("signInResult:failed code=" + e.getStatusCode());
            setUiControlStates(false);
            mFetchAndSetAuthenticationTokensCallback.run(false, new RuntimeException(e.getMessage()));
        }
    }

    @Override
    public void onClick(View v) {
        // login button click event handlers:
        switch (v.getId()) {
            case R.id.plus_sign_in_button:
                mStatus.setText(R.string.status_signing_in);
                getIdToken();
                break;
            case R.id.plus_sign_out_button:
                signOut();
                break;
            case R.id.plus_disconnect_button:
                revokeAccess();
                break;
            case R.id.login_continue_button:
            case R.id.login_close_button:
                close(true);
                break;
        }

    }

    /* Private Helper Methods */
    private void initMembers(Bundle savedInstanceState) {
        mStatus = (TextView)findViewById(R.id.sign_in_status);
        mConnectionStatus = (TextView)findViewById(R.id.connection_status);
        mSignInButton = (SignInButton) findViewById(R.id.plus_sign_in_button);
        mSignOutButton = (Button) findViewById(R.id.plus_sign_out_button);
        mRevokeButton = (Button) findViewById(R.id.plus_disconnect_button);
        mCancelButton = (Button) findViewById(R.id.login_close_button);
        mContinueButton = (Button) findViewById(R.id.login_continue_button);
        mProgressView = findViewById(R.id.login_progress);

        /* Button Handlers */
        mSignInButton.setOnClickListener(this);
        mSignOutButton.setOnClickListener(this);
        mRevokeButton.setOnClickListener(this);
        mCancelButton.setOnClickListener(this);
        mContinueButton.setOnClickListener(this);

        /* Restoration */
        if (savedInstanceState != null) {
            setGoogleSignInProgress(savedInstanceState.getInt(KEY_SAVED_PROGRESS, STATE_DEFAULT));
            if (savedInstanceState.containsKey(KEY_UPLOAD_ENABLED_STATE))
                mUploadEnabledState = savedInstanceState.getBoolean(KEY_UPLOAD_ENABLED_STATE, true);
        }

        /* Other Members */
        mStorage = BackendStorage.create(this);
    }

    private void initBroadcastReceiver() {
        mMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, final Intent intent) {
                final String action = intent.getAction();

                switch (action) {
                    case InternalBroadcasts.KEY_USER_ID_CLEARED:
                        // Sign-out when authentication token is cleared (e.g. reg. failed)
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // The current state should be verified because this message may be delayed
                                if (!mStorage.isUserIdAvailable()) {
                                    Toast.makeText(getBaseContext(), "Signed out (or sign-in failed)", Toast.LENGTH_LONG).show();
                                    signOut();
                                }
                            }
                        });
                        break;
                    case InternalBroadcasts.KEY_USER_ID_SET:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // The current state should be verified because this message may be delayed
                                if (mStorage.isUserIdAvailable()) {
                                    doSignInActions();
                                }
                            }
                        });
                        break;
                    case InternalBroadcasts.KEY_ONE_TIME_TOKEN_SET:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // The current state should be verified because this message may be delayed
                                if (mStorage.isOneTimeTokenAvailable()) {
                                    final String statusText = getResources().getString(R.string.status_connecting_to_server);
                                    setStatusText(statusText);
                                }
                            }
                        });
                        break;
                    case InternalBroadcasts.KEY_SERVER_CONNECTION_FAILURE:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mConnectionStatus.setText(getResources().getString(R.string.connection_status_connection_failed));
                            }
                        });
                        break;
                    case InternalBroadcasts.KEY_SERVER_CONNECTION_SUCCEEDED:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                final String connStatusText = getResources().getString(R.string.connection_status_connected);
                                if (!mConnectionStatus.getText().equals(connStatusText))
                                    mConnectionStatus.setText(connStatusText);
                            }
                        });
                        break;
                    case InternalBroadcasts.KEY_REGISTRATION_SUCCEEDED:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                RegularRoutesPipeline.setUploadEnabledState(true);
                                setStatusAsAuthenticated();
                            }
                        });
                        break;
                    case InternalBroadcasts.KEY_AUTHENTICATION_SUCCEEDED:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                RegularRoutesPipeline.setUploadEnabledState(true);
                                setStatusAsAuthenticated();
                            }
                        });
                        break;
                    case InternalBroadcasts.KEY_AUTHENTICATION_FAILED:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setStatusAsAuthenticationFailed();
                            }
                        });
                        break;
                    case InternalBroadcasts.KEY_RETURNED_AUTHENTICATION_RESULT:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Bundle args = intent.getExtras();
                                if (args != null) {
                                    if (args.containsKey(InternalBroadcasts.KEY_RETURNED_AUTHENTICATION_RESULT_ERR_MSG)) {
                                        // authentication failed
                                        setStatusAsAuthenticationFailed();
                                        final String errMsg = args.getString(InternalBroadcasts.KEY_RETURNED_AUTHENTICATION_RESULT_ERR_MSG);
                                        Toast.makeText(getBaseContext(), errMsg, Toast.LENGTH_LONG).show();
                                        return;
                                    }

                                }

                                setStatusAsAuthenticated();

                            }
                        });
                        break;
                }
            }
        };

        final IntentFilter filter = new IntentFilter();
        filter.addAction(InternalBroadcasts.KEY_USER_ID_CLEARED);
        filter.addAction(InternalBroadcasts.KEY_USER_ID_SET);
        filter.addAction(InternalBroadcasts.KEY_ONE_TIME_TOKEN_SET);
        filter.addAction(InternalBroadcasts.KEY_REGISTRATION_SUCCEEDED);
        filter.addAction(InternalBroadcasts.KEY_SERVER_CONNECTION_FAILURE);
        filter.addAction(InternalBroadcasts.KEY_SERVER_CONNECTION_SUCCEEDED);
        filter.addAction(InternalBroadcasts.KEY_AUTHENTICATION_SUCCEEDED);
        filter.addAction(InternalBroadcasts.KEY_AUTHENTICATION_FAILED);
        filter.addAction(InternalBroadcasts.KEY_RETURNED_AUTHENTICATION_RESULT);

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mLocalBroadcastManager.registerReceiver(mMessageReceiver, filter);
    }

    private void setStatusAsAuthenticated() {
        final String statusText = getResources().getString(R.string.status_authenticated);
        setStatusText(statusText);
    }

    private void setStatusAsAuthenticationFailed() {
        final String statusText = getResources().getString(R.string.status_authentication_failed);
        setStatusText(statusText);
    }

    private void setStatusText(final String statusText) {
        if (!mStatus.getText().equals(statusText))
            mStatus.setText(statusText);
    }

    private void doSignInActions() {

        setGoogleSignInProgress(STATE_DEFAULT);
        mContinueButton.setVisibility(View.VISIBLE);
    }

    private void getIdToken() {
        // Show an account picker to let the user choose a Google account from the device.
        // If the GoogleSignInOptions only asks for IDToken and/or profile and/or email then no
        // consent screen will be shown here.
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void refreshIdToken() {
        // Attempt to silently refresh the GoogleSignInAccount. If the GoogleSignInAccount
        // already has a valid token this method may complete immediately.
        //
        // If the user has not previously signed in on this device or the sign-in has expired,
        // this asynchronous branch will attempt to sign in the user silently and get a valid
        // ID token. Cross-device single sign on will occur in this branch.
        mGoogleSignInClient.silentSignIn()
                .addOnCompleteListener(this, new OnCompleteListener<GoogleSignInAccount>() {
                    @Override
                    public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
                        handleSignInResult(task);
                    }
                });
    }


    private void revokeAccess() {
        mGoogleSignInClient.revokeAccess()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        mContinueButton.setVisibility(View.INVISIBLE);
                        setUiControlStates(false);
                        mStatus.setText(R.string.status_signed_out);
                        RegularRoutesPipeline.setUploadEnabledState(false);
                        mStorage.clearSessionToken();
                        mStorage.clearOneTimeToken();
                        mStorage.clearUserId();
                    }
                });
    }

    private void signOut() {
        mGoogleSignInClient.signOut()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        mContinueButton.setVisibility(View.INVISIBLE);
                        setUiControlStates(false);
                        mStatus.setText(R.string.status_signed_out);
                        RegularRoutesPipeline.setUploadEnabledState(false);
                        mStorage.clearSessionToken();
                        mStorage.clearOneTimeToken();
                        mStorage.clearUserId();
                    }
                });
    }

    private void setUiControlStates(boolean connected) {
        // Update the user interface to reflect that the user is signed in.
        mSignInButton.setEnabled(!connected);
        mSignOutButton.setEnabled(connected);
        mRevokeButton.setEnabled(connected);

        if (!connected)
            mContinueButton.setVisibility(View.INVISIBLE);
        mCancelButton.setVisibility(connected ? View.GONE : View.VISIBLE);

    }

    private void setGoogleSignInProgress(int state) {
        mSignInProgress = state;
        int progressVisibility = (mSignInProgress == STATE_DEFAULT) ? View.GONE : View.VISIBLE;
        mProgressView.setVisibility(progressVisibility);
    }

    private void showErrorInDialog(String msg) {
        ErrorDialogFragment errFr = ErrorDialogFragment.createInstance(msg);
        errFr.show(getFragmentManager(),TAG_ERR_DIALOG );
    }

    private boolean isSignedIn() {
        return mStorage.isUserIdAvailable();
    }

    private void close(boolean cancelled) {
        final int resultCode = cancelled ? RESULT_CANCELED : RESULT_OK;
        Intent returnIntent = new Intent();
        setResult(resultCode,returnIntent);
        finish();
    }

    private String getWebClientId() {
        if (mRes.getBoolean(R.bool.use_test_server)) {
            return mRes.getString(R.string.web_client_id_test);
        } else {
            return mRes.getString(R.string.web_client_id_production);
        }
    }

}



