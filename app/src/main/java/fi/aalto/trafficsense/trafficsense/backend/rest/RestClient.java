package fi.aalto.trafficsense.trafficsense.backend.rest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.Pair;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.firebase.iid.FirebaseInstanceId;
import fi.aalto.trafficsense.trafficsense.backend.TrafficSenseService;
import fi.aalto.trafficsense.trafficsense.backend.rest.types.*;
import fi.aalto.trafficsense.trafficsense.backend.uploader.DataQueue;
import fi.aalto.trafficsense.trafficsense.util.*;
import org.json.JSONObject;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import timber.log.Timber;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;

public class RestClient {
    private static final String THREAD_NAME_FORMAT = "rest-client";

    /* Private Members */
    private final BackendStorage mStorage;
    private final ExecutorService mHttpExecutor;
    private final RestApi mApi;
    private final AtomicReference<Boolean> mUploadEnabled = new AtomicReference<>(true);
    private final AtomicReference<Boolean> mUploading = new AtomicReference<>(false);
    private final AtomicReference<Boolean> mAuthenticating = new AtomicReference<>(false);
    // private final AtomicReference<Boolean> mAuthenticated = new AtomicReference<>(false);
    private final AtomicReference<Boolean> mRegisterFailed = new AtomicReference<>(false);
    private final Object uploadingStateLock = new Object();
    private final ThreadGlue mThreadGlue = new ThreadGlue();
    private final LocalBroadcastManager mLocalBroadcastManager;
    private final BroadcastReceiver mBroadcastReceiver;
    private final String mAndroidDeviceId;

    private AtomicReference<Optional<String>> mSessionTokenCache = new AtomicReference<>(Optional.<String>absent());
    private AtomicReference<Optional<Integer>> mClientNumberCache = new AtomicReference<>(Optional.<Integer>absent());

    private long mLatestUploadTime=0;


    /* Constructor(s) */
    public RestClient(Context context, Uri server, BackendStorage storage, Handler mainHandler) {
        this.mStorage = storage;
        this.mHttpExecutor = Executors.newSingleThreadExecutor(
                new ThreadFactoryBuilder().setNameFormat(THREAD_NAME_FORMAT).build());
        this.mApi = new RestAdapter.Builder()
                .setExecutors(mHttpExecutor, new HandlerExecutor(mainHandler))
                .setEndpoint(server.toString())
                .build().create(RestApi.class);
        // mAuthenticated.set(mStorage.isUserIdAvailable());

        if (context != null) {
            mLocalBroadcastManager = LocalBroadcastManager.getInstance(context);
            mBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    final String action = intent.getAction();

                    switch (action) {
                        case InternalBroadcasts.KEY_SESSION_TOKEN_CLEARED:
                            mSessionTokenCache.set(mStorage.readSessionToken());
                            mClientNumberCache.set(Optional.<Integer>absent());
                            mStorage.clearClientNumber();
                            break;
                        case InternalBroadcasts.KEY_REQUEST_AUTHENTICATION:
                            Timber.d("Authentication request received");
                            requestAuthentication();
                            break;
                        case InternalBroadcasts.KEY_DEBUG_SHOW_REQ:
                            broadcastUploadTime();
                            break;
                    }
                }
            };

            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(InternalBroadcasts.KEY_SESSION_TOKEN_CLEARED);
            intentFilter.addAction(InternalBroadcasts.KEY_REQUEST_AUTHENTICATION);
            intentFilter.addAction(InternalBroadcasts.KEY_DEBUG_SHOW_REQ);

            mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, intentFilter);
            mAndroidDeviceId =  Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        }
        else {
            mAndroidDeviceId = "";
            mLocalBroadcastManager = null;
            mBroadcastReceiver = null;
        }

    }

    /* Public Methods */

    public boolean isUploadEnabled() {
        return mUploadEnabled.get();
    }

    public void setUploadEnabledState(boolean enabled) {
        mUploadEnabled.set(enabled);
    }



    /**
     * Wait until previous upload operation(s) are completed and then triggers data upload.
     * If uploading is disabled, method returns instantly.
     *
     * @return true, if upload was triggered; false if upload is/was disabled
     */
    public boolean waitAndUploadData(final DataQueue queue) throws InterruptedException {
        if (!isUploadEnabled())
            return false;

        boolean uploadTriggered = false;

        while (!uploadTriggered && isUploadEnabled()) {
            waitTillNotUploading();
            uploadTriggered = uploadData(queue);
        }

        return uploadTriggered;
    }

    /**
     * Triggers uploading if other upload process is not ongoing
     * Remarks: this call is allowed only when signed in (as is all the API calls not related
     *          to authentication).
     * @return false if upload is disabled, other uploading is ongoing and operation was therefore aborted; true otherwise.
     */
    public boolean uploadData(final DataQueue queue) {
        Intent intent = new Intent(InternalBroadcasts.KEY_UPLOAD_STARTED);
        mLocalBroadcastManager.sendBroadcast(intent);
        mThreadGlue.verify();
        // Timber.d("uploadData called with mAuthenticated: "+mAuthenticated.get());
        if (!isUploadEnabled()) { // Try to resolve for next time
            if (!mStorage.isUserIdAvailable()) {
                Intent i = new Intent(InternalBroadcasts.KEY_REQUEST_SIGN_IN);
                mLocalBroadcastManager.sendBroadcast(i);
            } else { // User ID *is* available (= signed in)
                if (!mStorage.isClientNumberAvailable()) {
                    fetchClientNumber(new Callback<Optional<Integer>>() {
                        @Override
                        public void run(Optional<Integer> result, RuntimeException error) {
                            if (result.isPresent()) mStorage.writeClientNumber(result.get());
                        }
                    });
                }
            }
        }
        if (!isUploadEnabled() || isUploading()) // || !mAuthenticated.get())
            return false;

        if (queue.isEmpty()) {
            Timber.d("skipping upload operation: Queue is empty");
            notifyRestClientResults(InternalBroadcasts.KEY_UPLOAD_SUCCEEDED);
            return true;
        }

        setUploading(true);
        final DataBody body = DataBody.createSnapshot(queue);


        uploadDataInternal(queue, body, new Callback<Void>() {
            @Override
            public void run(Void result, RuntimeException error) {
                setUploading(false);
                if (error != null) {
                    Timber.e(error, "Data upload failed");
                    notifyRestClientResults(InternalBroadcasts.KEY_UPLOAD_FAILED);
                } else {
                    queue.removeUntilSequence(body.mSequence);
                    Timber.d("Uploaded data up to sequence #%d", body.mSequence);
                    mLatestUploadTime = System.currentTimeMillis();
                    broadcastUploadTime();
                    notifyRestClientResults(InternalBroadcasts.KEY_UPLOAD_SUCCEEDED);
                }
            }
        });

        return true;
    }



    public void destroy() {
        mHttpExecutor.shutdownNow();
        if (mLocalBroadcastManager != null) {
            mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
        }
    }


    public boolean isUploading() {
        return mUploading.get();
    }

    /**
     * Waits till uploading is completed or disabled
     */
    public void waitTillNotUploading() throws InterruptedException {
        synchronized (uploadingStateLock) {

            while (isUploadEnabled() && isUploading()) {
                uploadingStateLock.wait();
            }

        }
    }

    /**
     * Fetch client number that corresponds session token
     * Remarks: this call is allowed only when signed in (as is all the API calls not related
     *          to authentication or for research purposes)
     **/
    public void fetchClientNumber(final Callback<Optional<Integer>> callback) {
        final Optional<Integer> cachedValue = mClientNumberCache.get();
        if (cachedValue.isPresent()) {
            // use cached value
            callback.run(Optional.fromNullable(cachedValue.get()), null);
            return;
        }
        device(new Callback<Pair<String, Integer>>() {
            @Override
            public void run(Pair<String, Integer> result, RuntimeException error) {
                if (error != null) {
                    callback.run(Optional.<Integer>absent(), error);
                }
                else if (result.second > 0)
                {
                    // Proper id value //
                    callback.run(Optional.fromNullable(result.second), null);

                }
                else {
                    callback.run(Optional.<Integer>absent(), null);
                }


            }
        });
    }

    /* Private Methods */
    /**
     * Trigger Authentication / Register -operation per authentication request
     **/
    private void requestAuthentication() {
        if (!authenticateInternal(new Callback<Void>() {
            @Override
            public void run(Void result, RuntimeException error) {
                if (error != null) {
                    // mAuthenticated.set(false);
                    Timber.e("Authentication request failed: " + error.getMessage());
                    final Bundle args = new Bundle();
                    final String errMsg = "Authentication error: " + error.getMessage();
                    args.putString(InternalBroadcasts.KEY_RETURNED_AUTHENTICATION_RESULT_ERR_MSG, errMsg);
                    notifyRestClientResults(InternalBroadcasts.KEY_RETURNED_AUTHENTICATION_RESULT, args);
                } else {
                    // authenticated
                    Timber.d("Authentication request completed successfully");
                    // mAuthenticated.set(true);
                    notifyRestClientResults(InternalBroadcasts.KEY_RETURNED_AUTHENTICATION_RESULT);
                }
            }
        })) {
            final Bundle args = new Bundle();
            args.putString(InternalBroadcasts.KEY_RETURNED_AUTHENTICATION_RESULT_ERR_MSG, "Authentication cancelled");
            notifyRestClientResults(InternalBroadcasts.KEY_RETURNED_AUTHENTICATION_RESULT, args);
            Timber.e("Authenticate executed while not signed in or authentication is ongoing");
        }
    }

    private void register(final RegisterRequest request, final Callback<RegisterResponse> callback) {
        try {
            mApi.register(request,new retrofit.Callback<RegisterResponse>() {

                @Override
                public void success(RegisterResponse registerResponse, Response response) {
                    Timber.i("Registration succeeded");
                    callback.run(registerResponse, null);
                }

                @Override
                public void failure(RetrofitError error) {
                    if (error != null) {
                        final String msg = error.getMessage();
                        callback.run(null, new RuntimeException(msg, error));
                    }
                    else {
                        final String errorText = "Registration failed: Unknown error";
                        callback.run(null, new RuntimeException(errorText));
                    }



                }
            });
        } catch (RejectedExecutionException e) {
            // Registration failed: app is closing
            Timber.i("Registration rejected: " + e.getMessage());
        }

    }

    private AtomicReference<Boolean> mConnectionFailedPreviously = new AtomicReference<>(false);

    private void authenticate(final AuthenticateRequest request, final Callback<Void> callback) {
        if (mAuthenticating.get()) {
            Timber.d("Authentication re-attempt blocked");
            return;
        }
        mAuthenticating.set(true);

        /**
         * With 'one-time token', server authenticates client from Google Authentication service.
         * One-time token was fetched when signed on and deviceAuthId was generated.
         * DeviceAuthId is then used in server verification and later when client authenticates
         * again to get session token. Server may invalidate session token after some time
         * and then client must re-authenticate itself
         **/
        Optional<String> oneTimeToken = mStorage.readAndClearOneTimeToken();

        if (oneTimeToken.isPresent()) {
            Timber.d("Registering client with one-time token");
            if (!registerInternal(oneTimeToken.get(), request, new Callback<Void>() {
                @Override
                public void run(Void result, RuntimeException error) {
                    mAuthenticating.set(false);

                    if (error != null) {
                        Timber.e("Error in registration: " + error.getMessage());
                        // mAuthenticated.set(false);
                        callback.run(null, error);
                    }
                    else {
                        authenticate(request, callback);
                        // mAuthenticated.set(true);
                    }
                }
            })) {

                callback.run(null, new RuntimeException("Register attempt without signed in detected"));
                mAuthenticating.set(false);
            }
        } else {
            try {
                mApi.authenticate(request, new retrofit.Callback<AuthenticateResponse>() {
                    @Override
                    public void success(AuthenticateResponse authenticateResponse, Response response) {
                        Timber.i("Authentication succeeded");
                        // mAuthenticated.set(true);
                        mSessionTokenCache.set(Optional.fromNullable(authenticateResponse.mSessionToken));
                        mStorage.writeSessionToken(authenticateResponse.mSessionToken);
                        if (mConnectionFailedPreviously.get()) {
                            mConnectionFailedPreviously.set(false);

                        }

                        notifyRestClientResults(InternalBroadcasts.KEY_SERVER_CONNECTION_SUCCEEDED);
                        notifyRestClientResults(InternalBroadcasts.KEY_AUTHENTICATION_SUCCEEDED);
                        mAuthenticating.set(false);
                        callback.run(null, null);
                    }

                    @Override
                    public void failure(RetrofitError error) {

                        final String msg = error.getMessage();
                        // mAuthenticated.set(false);

                        if (msg != null) {
                            Timber.w("Authentication failed: " + msg);

                            if (msg.startsWith("failed to connect")) {
                                // Regular routes server is unavailable

                                if (!mConnectionFailedPreviously.get()) {
                                    mConnectionFailedPreviously.set(true);
                                    notifyRestClientResults(InternalBroadcasts.KEY_SERVER_CONNECTION_FAILURE);
                                }

                                mAuthenticating.set(false);

                                callback.run(null, new RuntimeException("Connection failure: " + msg, error));
                            }
                            else {
                                mStorage.clearSessionToken();
                                mAuthenticating.set(false);
                                notifyRestClientResults(InternalBroadcasts.KEY_SERVER_CONNECTION_SUCCEEDED);
                                callback.run(null, new RuntimeException("Authentication failed", error));
                            }

                        } else { // msg==null
                            if (error == null) {
                                Timber.d("RestApi-authentication-failure with null error");
                            } else {
                                String mStackTrace = Log.getStackTraceString(error);
                                if(mStackTrace.contains("java.io.EOFException")) {
                                    // Stack-internal bug, the request was never sent -> try again
                                    Timber.i("RestApi-authentication-failure caught an EOFException - trying again");
                                    mAuthenticating.set(false);
                                    authenticate(request, callback);
                                    return;
                                }
                            }
                            mStorage.clearSessionToken();
                            mAuthenticating.set(false);
                            notifyRestClientResults(InternalBroadcasts.KEY_SERVER_CONNECTION_SUCCEEDED);
                            callback.run(null, new RuntimeException("Authentication failed with null message", error));
                        }
                        notifyRestClientResults(InternalBroadcasts.KEY_AUTHENTICATION_FAILED);
                    }
                });
            } catch (RejectedExecutionException e) {
                Timber.d("Authentication call rejected by RestApi: application is closing");
            }
        }
    }

    /**
     * Get device id with session token
     * @param callback
     */
    private void device(final Callback<Pair<String, Integer>> callback) {

        Optional<String> sessionToken = mStorage.readSessionToken();
        if (!sessionToken.isPresent()) {
            if (mAuthenticating.get()) {
                callback.run(null, new RuntimeException("Authentication ongoing"));
                notifyRestClientResults(InternalBroadcasts.KEY_CLIENT_NUMBER_FETCH_COMPLETED);
                return;
            }
            if (!authenticateInternal(new Callback<Void>() {
                @Override
                public void run(Void result, RuntimeException error) {
                    if (error != null) {
                        Timber.e(error.getMessage());
                        callback.run(null, error);
                        notifyRestClientResults(InternalBroadcasts.KEY_CLIENT_NUMBER_FETCH_COMPLETED);
                    } else {
                        device(callback);
                    }
                }
            })) {
                Timber.e("Not signed in or authentication is ongoing");
            }

            return;
        }

        mApi.device(sessionToken.get(), new retrofit.Callback<DeviceResponse>(){

            @Override
            public void success(DeviceResponse deviceResponse, Response response) {

                if (deviceResponse.mError != null) {
                    callback.run(null, new RuntimeException("Fetching device id failed: " + deviceResponse.mError));
                    notifyRestClientResults(InternalBroadcasts.KEY_CLIENT_NUMBER_FETCH_COMPLETED);
                    return;
                }

                final Integer clientNumber = Integer.parseInt(deviceResponse.DeviceId);
                if (clientNumber >= 0) {
                    mClientNumberCache.set(Optional.fromNullable(clientNumber));
                    mStorage.writeClientNumber(clientNumber);
                }
                else {
                    mClientNumberCache.set(Optional.<Integer>absent());
                }


                Pair<String, Integer> value = new Pair<>(deviceResponse.mSessionToken, clientNumber);
                callback.run(value, null);
                notifyRestClientResults(InternalBroadcasts.KEY_CLIENT_NUMBER_FETCH_COMPLETED);
            }

            @Override
            public void failure(RetrofitError error) {
                callback.run(null, new RuntimeException("Fetching device id failed", error));
                notifyRestClientResults(InternalBroadcasts.KEY_CLIENT_NUMBER_FETCH_COMPLETED);
            }
        });
    }

    private void uploadDataInternal(final DataQueue queue, final DataBody body, final Callback<Void> callback) {
        Optional<String> sessionToken = mSessionTokenCache.get();
        if (!sessionToken.isPresent()) {
            if (!authenticateInternal(new Callback<Void>() {
                @Override
                public void run(Void result, RuntimeException error) {
                    if (error != null) {
                        Timber.e(error.getMessage());
                        queue.increaseThreshold();
                        callback.run(null, error);
                    } else {
                        uploadDataInternal(queue, body, callback);
                    }
                }
            })) {
                Timber.e("Not signed in or authentication ongoing");
            }

            return;
        }

        Timber.d("Uploading data...");
        mApi.data(sessionToken.get(), body, new retrofit.Callback<JSONObject>() {
            @Override
            public void success(JSONObject s, Response response) {
                Timber.d("Data upload succeeded");
                queue.initThreshold(); // Reset queue threshold to configured level
                callback.run(null, null);
            }

            @Override
            public void failure(RetrofitError error) {
                if (error != null) {
                    // Pre 4.4 Androids throw EOFExceptions every now and then:
                    String mStackTrace = Log.getStackTraceString(error);
                    if(mStackTrace.contains("java.io.EOFException")) {
                        Timber.i("uploadDataInternal caught an EOFException - trying again");
                        uploadDataInternal(queue, body, callback);
                        return;
                    } else {
                        if (mStackTrace.contains("500 INTERNAL SERVER ERROR")) {
                            // MJR: Don't know yet what is causing this, but suspect bad data
                            // MJR: ...so starting to drop points from the beginning one by one, in case that would help.
                            queue.removeOne();
                        }
                        Timber.d("uploadDataInternal stacktrace produces:"+ mStackTrace);
                    }
                    final Response response = error.getResponse();
                    if (response != null && response.getStatus() == 403) {
                        // Re-authentication required (session token has expired)
                        mStorage.clearSessionToken();
                        uploadDataInternal(queue, body, callback);
                        return;

                    }

                }
                // Some other upload error
                queue.increaseThreshold();
                notifyRestClientResults(InternalBroadcasts.KEY_UPLOAD_FAILED);
                callback.run(null, new RuntimeException("Data upload failed", error));
            }
        });
    }

    /**
     * Helper call for authentication that creates request and then calls authenticate() -method
     **/
    private boolean authenticateInternal(final Callback<Void> callback) {

        if (mAuthenticating.get())
            return false; // authentication ongoing already

        // remove value from storage
        final Optional<String> userId = mStorage.readUserId();
        if (!userId.isPresent())
        {
            // Not signed in
            return false;
        }
        if (mRegisterFailed.get() && !mStorage.isOneTimeTokenAvailable()) {
            // no point to try to authenticate, if registration failed
            return false;
        }


        final Optional<String> installationId = mStorage.readInstallationId();
        String messagingToken = FirebaseInstanceId.getInstance().getToken();
        Timber.d("RestClient got messaging token: %s", messagingToken);
        AuthenticateRequest request = new AuthenticateRequest(userId.get(), mAndroidDeviceId, installationId.get(), EnvInfo.getClientVersionString(), messagingToken);
        Timber.d("User id: " + userId.get());
        Timber.d("mAndroidDeviceId " + mAndroidDeviceId);
        Timber.d("installationId: " + installationId.get());
        authenticate(request,new Callback<Void>() {
            @Override
            public void run(Void result, RuntimeException error) {
                // return value to storage
                if (error != null) {
                    Timber.e("Error in authentication: " + error.getMessage());
                    callback.run(null, new RuntimeException("Authentication failed: " + error.getMessage()));
                }
                else
                {
                    callback.run(null, null);
                }
            }
        });

        return true;
    }

    /**
     * Helper method that creates proper request and then calls register() -method
     **/
    private boolean registerInternal(final String oneTimeToken, final AuthenticateRequest authRequest, final Callback<Void> callback) {

        register(new RegisterRequest(authRequest, oneTimeToken, Build.MODEL, EnvInfo.getClientVersionString(), "") ,new Callback<RegisterResponse>() {
            @Override
            public void run(RegisterResponse response, RuntimeException error) {
                if (error != null) {
                    final String msg = error.getMessage();
                    Timber.e("Error in 'registerInternal': " + msg);
                    mRegisterFailed.set(true);
                    if (msg != null) {
                        if (msg.startsWith("failed to connect")) {
                            // token was not used
                            mStorage.writeOneTimeToken(oneTimeToken);

                            if (!mConnectionFailedPreviously.get()) {
                                mConnectionFailedPreviously.set(true);
                                notifyRestClientResults(InternalBroadcasts.KEY_SERVER_CONNECTION_FAILURE);
                            }
                        }
                        else {
                            // invalid/outdated token perhaps
                            mStorage.clearUserId();
                            notifyRestClientResults(InternalBroadcasts.KEY_SERVER_CONNECTION_SUCCEEDED);
                        }

                    }

                    mStorage.clearSessionToken();
                    callback.run(null, error);

                    return;
                }

                if (response != null) {
                    mStorage.writeSessionToken(response.mSessionToken);
                    mSessionTokenCache.set(Optional.fromNullable(response.mSessionToken));
                }


                if (mConnectionFailedPreviously.get()) {
                    mConnectionFailedPreviously.set(false);
                }

                notifyRestClientResults(InternalBroadcasts.KEY_SERVER_CONNECTION_SUCCEEDED);
                notifyRestClientResults(InternalBroadcasts.KEY_REGISTRATION_SUCCEEDED);
                mRegisterFailed.set(false);
                Timber.i("Register call succeeded");
                callback.run(null, null);
            }
        });

        return true;
    }

    private void setUploading(boolean isUploading) {
        synchronized (uploadingStateLock) {
            mUploading.set(isUploading);
            if (!isUploading)
                uploadingStateLock.notifyAll();

        }
    }

    private void broadcastUploadTime() {
        if (mLocalBroadcastManager != null && TrafficSenseService.isViewActive())
        {
            Intent intent = new Intent(InternalBroadcasts.KEY_UPLOAD_TIME);
            intent.putExtra(InternalBroadcasts.KEY_UPLOAD_TIME,mLatestUploadTime);
            mLocalBroadcastManager.sendBroadcast(intent);
        }
    }

    private void notifyRestClientResults(String messageType) {
        notifyRestClientResults(messageType, null);
    }

    private void notifyRestClientResults(String messageType, Bundle args) {
        if (mLocalBroadcastManager != null)
        {
            Intent intent = new Intent(messageType);
            if (args != null) {
                intent.putExtras(args);
            }
            mLocalBroadcastManager.sendBroadcast(intent);
        }
    }

}
