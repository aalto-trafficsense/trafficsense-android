package fi.aalto.trafficsense.trafficsense.backend.backend_util;

import android.app.AlarmManager;
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
import fi.aalto.trafficsense.trafficsense.util.TSBootReceiver;
import timber.log.Timber;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import static android.app.AlarmManager.RTC;

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
    private int notificationType = NOTIFICATION_TYPE_UNKNOWN;
    private boolean shouldCreateNotification = true;


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

    public static final String KEY_PTP_ALERT_MESSAGE = "PTP_ALERT_MESSAGE";
    public static final String KEY_PTP_ALERT_LAT = "PTP_ALERT_LAT";
    public static final String KEY_PTP_ALERT_LNG = "PTP_ALERT_LNG";

    public static final int SERVER_NOTIFICATION_ID = 1213;

    public static final String PTP_ALERT_END_ACTION = "fi.aalto.trafficsense.action.PTP_ALERT_END";

    public static final int NOTIFICATION_TYPE_UNKNOWN = 0;
    public static final int NOTIFICATION_TYPE_SURVEY = 1;
    public static final int NOTIFICATION_TYPE_BROADCASTMESSAGE = 2;
    public static final int NOTIFICATION_TYPE_FERRY = 3;
    public static final int NOTIFICATION_TYPE_SUBWAY = 4;
    public static final int NOTIFICATION_TYPE_TRAIN = 5;
    public static final int NOTIFICATION_TYPE_TRAM = 6;
    public static final int NOTIFICATION_TYPE_BUS = 7;
    public static final int NOTIFICATION_TYPE_DIGITRAFFIC = 8;

    private static final String PTP_ALERT_PUBTRANS = "PTP_ALERT_PUBTRANS";
    private static final String PTP_ALERT_TRAFFIC = "PTP_ALERT_TRAFFIC";
    private static final String PTP_ALERT_END = "PTP_ALERT_END";
    private static final String PTP_ALERT_TYPE = "PTP_ALERT_TYPE";
    private static final String PTP_NOTIFICATION = "PTP_NOTIFICATION";

    public static final String NOTIFICATION_PREFS_FILE_NAME = "SurveyPrefs";

    private SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");


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

            // All messages currently may have a title, message and uri.
            if (msgPayload.containsKey(KEY_NOTIFICATION_TITLE)) {
                messageTitle = msgPayload.get(KEY_NOTIFICATION_TITLE);
            } else {
                shouldCreateNotification = false;
            }
            if (msgPayload.containsKey(KEY_NOTIFICATION_MESSAGE)) {
                messageBody = msgPayload.get(KEY_NOTIFICATION_MESSAGE);
            } else {
                shouldCreateNotification = false;
            }
            if (msgPayload.containsKey(KEY_NOTIFICATION_URI)) {
                uriString = msgPayload.get(KEY_NOTIFICATION_URI);
            } else {
                uriString = "";
            }

            // If terminal locale is set to Finnish and there is Finnish content => replace
            String loc = Locale.getDefault().getLanguage();
            // Timber.d("ServerNotification got language as: %s", loc);
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

            if (from.startsWith(FB_TOPIC_PREFIX)) { // Broadcast topic message

                if (from.endsWith(FB_TOPIC_SURVEY)) {
                    Timber.d("Survey received");
                    notificationType = NOTIFICATION_TYPE_SURVEY;

                } else if (from.endsWith(FB_TOPIC_BROADCAST)) {
                    Timber.d("Generic Broadcast received");
                    notificationType = NOTIFICATION_TYPE_BROADCASTMESSAGE;
                }

                // Store without field replacements -> Survey should get the latest numbers, when launched
                SharedPreferences mPref = TrafficSenseApplication.getContext().getSharedPreferences(NOTIFICATION_PREFS_FILE_NAME, Context.MODE_PRIVATE);
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

            } else { // No topic - point-to-point message
                Timber.d("ServerNotification received a point-to-point");
                // Only Traffic Alerts currently contain a location:
                if (msgPayload.containsKey(PTP_ALERT_TRAFFIC)) {
                    SharedPreferences mPref = TrafficSenseApplication.getContext().getSharedPreferences(NOTIFICATION_PREFS_FILE_NAME, Context.MODE_PRIVATE);
                    SharedPreferences.Editor mPrefEditor = mPref.edit();
                    if (msgPayload.containsKey(KEY_PTP_ALERT_LAT) && msgPayload.containsKey(KEY_PTP_ALERT_LNG)) {
                        mPrefEditor.putFloat(KEY_PTP_ALERT_LAT, Float.parseFloat(msgPayload.get(KEY_PTP_ALERT_LAT)));
                        mPrefEditor.putFloat(KEY_PTP_ALERT_LNG, Float.parseFloat(msgPayload.get(KEY_PTP_ALERT_LNG)));
                        mPrefEditor.putString(KEY_PTP_ALERT_MESSAGE, messageBody);
                    } else {
                        mPrefEditor.remove(KEY_PTP_ALERT_LAT);
                        mPrefEditor.remove(KEY_PTP_ALERT_LNG);
                        mPrefEditor.remove(KEY_PTP_ALERT_MESSAGE);
                    }
                    mPrefEditor.apply();
                }
                // Common processing for public transport and traffic alerts
                if (msgPayload.containsKey(PTP_ALERT_PUBTRANS) || msgPayload.containsKey(PTP_ALERT_TRAFFIC)) {
                    long now = System.currentTimeMillis();
                    long alertEndMillis = now + (15 * 60 * 1000); // Default 15 minutes
                    if (msgPayload.containsKey(PTP_ALERT_END)) { // Alert expiration time specified
                        Date alertEndDate = new Date();
                        Timber.d("PTP Alert End is: %s", msgPayload.get(PTP_ALERT_END));
                        try {
                            alertEndDate = dateTimeFormat.parse(msgPayload.get(PTP_ALERT_END));
                        } catch (ParseException e) {
                            Timber.d("ServerNotification failed to parse PTP_ALERT_END: %s", e.getMessage());
                            e.printStackTrace();
                        }
                        Timber.d("Now is: %d Alert End is: %d", now, alertEndMillis);
                        if (alertEndDate.getTime() > now) {
                            alertEndMillis = alertEndDate.getTime();
                        }
                    }
                    Timber.d("ServerNotification set alert duration as %d ms.", alertEndMillis - now);
                    // Set up an alarm for the alert end time
                    Context ctx = TrafficSenseApplication.getContext();
                    AlarmManager alarmManager = (AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);
                    Intent intent = new Intent(ctx, TSBootReceiver.class);
                    intent.setAction(PTP_ALERT_END_ACTION);
                    intent.putExtra("notification_id", SERVER_NOTIFICATION_ID);
                    PendingIntent pi = PendingIntent.getBroadcast(ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                    alarmManager.set(RTC, alertEndMillis, pi);
                    if (msgPayload.containsKey(PTP_ALERT_TYPE)) {
                        switch (msgPayload.get(PTP_ALERT_TYPE)) {
                            case "TRAIN":
                                notificationType = NOTIFICATION_TYPE_TRAIN;
                                break;
                            case "TRAM":
                                notificationType = NOTIFICATION_TYPE_TRAM;
                                break;
                            case "SUBWAY":
                                notificationType = NOTIFICATION_TYPE_SUBWAY;
                                break;
                            case "BUS":
                                notificationType = NOTIFICATION_TYPE_BUS;
                                break;
                            case "FERRY":
                                notificationType = NOTIFICATION_TYPE_FERRY;
                                break;
                            case "DIGITRAFFIC":
                                notificationType = NOTIFICATION_TYPE_DIGITRAFFIC;
                                break;
                            default:
                                notificationType = NOTIFICATION_TYPE_UNKNOWN;
                        }
                    }

                } else if (msgPayload.containsKey(PTP_NOTIFICATION)) {
                    notificationType = NOTIFICATION_TYPE_BROADCASTMESSAGE;
                } else {
                    // Unknown PTP - do not post
                    shouldCreateNotification = false;
                }

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
            case NOTIFICATION_TYPE_FERRY:
                notificationIcon = R.drawable.md_activity_ferry_24dp;
                break;
            case NOTIFICATION_TYPE_SUBWAY:
                notificationIcon = R.drawable.md_activity_subway_24dp;
                break;
            case NOTIFICATION_TYPE_TRAIN:
                notificationIcon = R.drawable.md_activity_train_24dp;
                break;
            case NOTIFICATION_TYPE_TRAM:
                notificationIcon = R.drawable.md_activity_subway_24dp;
                break;
            case NOTIFICATION_TYPE_BUS:
                notificationIcon = R.drawable.md_activity_bus_24dp;
                break;
            case NOTIFICATION_TYPE_DIGITRAFFIC:
                notificationIcon = R.drawable.md_activity_vehicle_24dp;
                break;
            default:
                notificationIcon = R.mipmap.ic_launcher;
        }

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(TrafficSenseApplication.getContext())
                .setSmallIcon(notificationIcon)
                .setColor(ContextCompat.getColor(TrafficSenseApplication.getContext(),R.color.colorTilting))
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

    public boolean shouldCreateNotification() { return shouldCreateNotification; }

}
