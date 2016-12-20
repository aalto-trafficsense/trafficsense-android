package fi.aalto.trafficsense.trafficsense.backend.backend_util;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import com.google.firebase.messaging.RemoteMessage;
import fi.aalto.trafficsense.trafficsense.R;
import fi.aalto.trafficsense.trafficsense.TrafficSenseApplication;
import fi.aalto.trafficsense.trafficsense.ui.MainActivity;
import fi.aalto.trafficsense.trafficsense.util.EnvInfo;
import timber.log.Timber;

import java.util.Locale;
import java.util.Map;

/**
 * Created by mikko.rinne@aalto.fi on 16/12/16.
 *
 * Store notifications coming from the server
 * (Currently implemented with Firebase messaging)
 */
public class ServerNotification {
    private String uriString;
    private String messageTitle;
    private String messageBody;
    private int notificationType;
    private boolean messageOk = true;


    public static final String FB_TOPIC_SURVEY = "surveys";
    public static final String FB_TOPIC_BROADCAST = "broadcast";
    private static final String FB_TOPIC_PREFIX = "/topics/";

    public static final String KEY_NOTIFICATION_TITLE = "NOTIFICATION_TITLE";
    public static final String KEY_NOTIFICATION_MESSAGE = "NOTIFICATION_MESSAGE";
    public static final String KEY_NOTIFICATION_URI = "NOTIFICATION_URI";
    public static final String KEY_NOTIFICATION_TYPE = "NOTIFICATION_TYPE";
    public static final String KEY_NOTIFICATION_TITLE_FI = "NOTIFICATION_TITLE_FI";
    public static final String KEY_NOTIFICATION_MESSAGE_FI = "NOTIFICATION_MESSAGE_FI";
    public static final String KEY_NOTIFICATION_URI_FI = "NOTIFICATION_URI_FI";

    private static final int SERVER_NOTIFICATION_ID = 1213;

    public static final int NOTIFICATION_TYPE_SURVEY = 1;
    public static final int NOTIFICATION_TYPE_BROADCASTMESSAGE = 2;

    public static final String SURVEY_PREFS_FILE_NAME = "SurveyPrefs";

    public ServerNotification(String mUri, String mTitle, String mBody) {
        uriString = mUri;
        messageTitle = mTitle;
        messageBody = mBody;
    }

    public ServerNotification(RemoteMessage remoteMessage) {

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            String from = remoteMessage.getFrom();
            Timber.d("From: %s", from);
            Map<String, String> msgPayload = remoteMessage.getData();

            if (from.startsWith(FB_TOPIC_PREFIX)) { // Broadcast topic message

                // All broadcast notifications currently have a title, message and uri.
                if (msgPayload.containsKey(KEY_NOTIFICATION_TITLE)) {
                    messageTitle = msgPayload.get(KEY_NOTIFICATION_TITLE);
                } else {
                    messageOk = false;
                }
                if (msgPayload.containsKey(KEY_NOTIFICATION_MESSAGE)) {
                    messageBody = msgPayload.get(KEY_NOTIFICATION_MESSAGE);
                } else {
                    messageOk = false;
                }
                if (msgPayload.containsKey(KEY_NOTIFICATION_URI)) {
                    uriString = msgPayload.get(KEY_NOTIFICATION_URI);
                } else {
                    uriString = "";
                }

                // If terminal locale is set to Finnish and there is Finnish content => replace
                String loc = Locale.getDefault().getLanguage();
                Timber.d("ServerNotification got language as: %s", loc);
                if (loc.equalsIgnoreCase("fi")) {
                    if (msgPayload.containsKey(KEY_NOTIFICATION_TITLE_FI)) {
                        messageTitle = msgPayload.get(KEY_NOTIFICATION_TITLE_FI);
                    }
                    if (msgPayload.containsKey(KEY_NOTIFICATION_MESSAGE_FI)) {
                        messageBody = msgPayload.get(KEY_NOTIFICATION_MESSAGE_FI);
                    }
                    if (msgPayload.containsKey(KEY_NOTIFICATION_URI_FI)) {
                        uriString = msgPayload.get(KEY_NOTIFICATION_URI_FI);
                    } else {
                        uriString = "";
                    }

                }

                if (from.endsWith(FB_TOPIC_SURVEY)) {
                    Timber.d("Survey received");
                    notificationType = NOTIFICATION_TYPE_SURVEY;

                } else if (from.endsWith(FB_TOPIC_BROADCAST)) {
                    Timber.d("Generic Broadcast received");
                    notificationType = NOTIFICATION_TYPE_BROADCASTMESSAGE;
                }

                // Store without field replacements -> Survey should get the latest numbers, when launched
                SharedPreferences mPref = TrafficSenseApplication.getContext().getSharedPreferences(SURVEY_PREFS_FILE_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor mPrefEditor = mPref.edit();
                if (uriString.length()>5) {
                    mPrefEditor.putString(KEY_NOTIFICATION_URI, uriString);
                    Timber.d("Survey URI String stored as: %s", uriString);
                    mPrefEditor.putString(KEY_NOTIFICATION_MESSAGE, messageBody);
                    Timber.d("Message Notification Body: %s", messageBody);
                    mPrefEditor.putInt(KEY_NOTIFICATION_TYPE, notificationType);
                } else {
                    mPrefEditor.remove(KEY_NOTIFICATION_URI);
                    mPrefEditor.remove(KEY_NOTIFICATION_MESSAGE);
                    mPrefEditor.remove(KEY_NOTIFICATION_TYPE);
                }
                mPrefEditor.apply();

            } else { // point-to-point message
                // TODO: Add point-to-point message processing
                Timber.d("ServerNotification received a point-to-point");
            }

        }

    }

    /**
     * Post the server notification
     */
    public void postNotification() {
        int notificationIcon;
        switch (notificationType) {
            case NOTIFICATION_TYPE_SURVEY:
                notificationIcon = R.drawable.md_survey;
                break;
            case NOTIFICATION_TYPE_BROADCASTMESSAGE:
                notificationIcon = R.drawable.ic_info_outline_24dp;
                break;
            default:
                notificationIcon = R.mipmap.ic_launcher;
        }

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(TrafficSenseApplication.getContext())
                .setSmallIcon(notificationIcon)
                .setColor(ContextCompat.getColor(TrafficSenseApplication.getContext(),R.color.colorTilting))
                .setWhen(System.currentTimeMillis())
                .setContentTitle(messageTitle)
                .setContentText(messageBody)
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                // Notifications from 4.1 onwards
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(messageBody));

        // Set notification click behavior
        Intent notificationIntent;
        if (uriString.length()>5) { // If we have a URI, clicking will open it
            notificationIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(EnvInfo.replaceUriFields(uriString)));
        } else { // Otherwise click will open MainActivity
            notificationIntent = new Intent(TrafficSenseApplication.getContext(), MainActivity.class);
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(TrafficSenseApplication.getContext(), 0 /* Request code */, notificationIntent,
                PendingIntent.FLAG_ONE_SHOT);
        notificationBuilder.setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) TrafficSenseApplication.getContext().getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(SERVER_NOTIFICATION_ID /* ID of notification */, notificationBuilder.build());
    }

    public boolean messageOk() { return messageOk; }

}
