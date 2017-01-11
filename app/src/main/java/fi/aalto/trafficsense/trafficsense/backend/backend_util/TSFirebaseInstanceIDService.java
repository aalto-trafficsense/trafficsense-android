package fi.aalto.trafficsense.trafficsense.backend.backend_util;

/**
 * Created by mikko.rinne@aalto.fi on 04/10/16.
 */

import android.os.AsyncTask;
import android.widget.Toast;
import com.google.android.gms.maps.model.LatLng;
import com.google.common.base.Optional;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.google.firebase.messaging.FirebaseMessaging;
import fi.aalto.trafficsense.trafficsense.R;
import fi.aalto.trafficsense.trafficsense.TrafficSenseApplication;
import fi.aalto.trafficsense.trafficsense.ui.MainActivity;
import fi.aalto.trafficsense.trafficsense.util.BackendStorage;
import fi.aalto.trafficsense.trafficsense.util.SharedPrefs;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import timber.log.Timber;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class TSFirebaseInstanceIDService extends FirebaseInstanceIdService {

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Timber.d("Refreshed token: %s", refreshedToken);

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        sendRegistrationToServer(refreshedToken);
    }

    /**
     * Send token to regularroutes server
     * or store it to backendstorage, if session token is not yet available
     *
     * @param messaging_token The new token.
     */
    private void sendRegistrationToServer(String messaging_token) {
        BackendStorage mStorage = BackendStorage.create(TrafficSenseApplication.getContext());
        mStorage.writeMessagingToken(messaging_token);
        // TODO: If this cannot be properly tested (token never refreshes while signed in) it should be commented out!
        // If token refresh is very rare, can also rely on refreshing together with authentication.
        Optional<String> token = mStorage.readSessionToken();
        if (!token.isPresent()) {
            Timber.d("Attempting messaging token refresh, but no session token available!");
        } else {
            try {
                URL url = new URL(mStorage.getServerName() + "/msgtokenrefresh/" + token.get() + "?messaging_token=" + messaging_token);
                TSFirebaseInstanceIDService.SendMsgTokenTask downloader = new TSFirebaseInstanceIDService.SendMsgTokenTask();
                downloader.execute(url);
            } catch (MalformedURLException e) {
                Toast.makeText(this, R.string.dest_url_broken, Toast.LENGTH_SHORT).show();
            }
        }

    }

    private class SendMsgTokenTask extends AsyncTask<URL, Void, String> {
        protected String doInBackground(URL... urls) {
            String msgTokenRefreshResponse = null;
            if (urls.length != 1) {
                Timber.e("Path downloader attempted to get more or less than one URL");
                return null;
            }

            HttpURLConnection urlConnection = null;
            try {
                Timber.d("Opening with URL: %s", urls[0].toString());
                urlConnection = (HttpURLConnection) urls[0].openConnection();

                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                java.util.Scanner s = new java.util.Scanner(in).useDelimiter("\\A");
                if (s.hasNext()) {
                    msgTokenRefreshResponse = s.next();
                }
                else {
                    Timber.e("SendMsgTokenTask received no data!");
                }
                in.close();
            }
            catch (IOException e) {
                Timber.e("SendMsgTokenTask error connecting to URL.");
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

            return msgTokenRefreshResponse;

        }

        protected void onPostExecute(String response) {
            Timber.d("SendMsgTokenTask received: %s", response);
        }
    }

}
