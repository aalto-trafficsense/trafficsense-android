package fi.aalto.trafficsense.trafficsense.backend.backend_util;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import android.support.v4.content.ContextCompat;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import fi.aalto.trafficsense.trafficsense.R;
import fi.aalto.trafficsense.trafficsense.TrafficSenseApplication;
import fi.aalto.trafficsense.trafficsense.backend.TrafficSenseService;
import fi.aalto.trafficsense.trafficsense.ui.MainActivity;
import fi.aalto.trafficsense.trafficsense.util.BackendStorage;
import fi.aalto.trafficsense.trafficsense.util.EnvInfo;
import fi.aalto.trafficsense.trafficsense.util.SharedPrefs;
import timber.log.Timber;

import java.util.Map;

/**
 * Created by mikko.rinne@aalto.fi on 04/10/16.
 *
 * Original based on the firebase messaging service sample file:
 * https://github.com/firebase/quickstart-android/blob/master/messaging/app/src/main/java/com/google/firebase/quickstart/fcm/MyFirebaseMessagingService.java
 */
public class TSFirebaseMessagingService extends FirebaseMessagingService {



    private static final int SURVEY_NOTIFICATION_ID = 1213;

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
        // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages containing both notification
        // and data payloads are treated as notification messages. The Firebase console always sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]

        // Instructions for sending a DATA MESSAGE are here:
        // http://stackoverflow.com/questions/37711082/how-to-handle-notification-when-app-in-background-in-firebase/37845174#37845174

        // Sample message:
        /*
{
    "to" : "/topics/surveys",
    "data" : {
	  "SURVEY_MESSAGE" : "Karpolla on asiaa!",
      "SURVEY_URI" : "https://docs.google.com/forms/d/1GRvwgUXigE2iclSqAMqd6B2m3SDcs239a9nRrhHOgKM/viewform?entry.1453132440&entry.1290358306=client_number&entry.1714883594=client_version"
    }
  }         */
        // Feedback form test URI:
        // https://docs.google.com/forms/d/1GRvwgUXigE2iclSqAMqd6B2m3SDcs239a9nRrhHOgKM/viewform?entry.1453132440&entry.1290358306=client_number&entry.1714883594=client_version
        //
        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ

        if (remoteMessage.getData().size() > 0) {
            ServerNotification sn = new ServerNotification(remoteMessage);

            if (sn.messageOk()) {
                sn.postNotification();

            }

        }

    }
    // [END receive_message]

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param messageBody FCM message body received.
     */
//    private void sendNotification(String messageBody, String uriString) {
//        Intent notificationIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(EnvInfo.replaceUriFields(uriString)));
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, notificationIntent,
//                PendingIntent.FLAG_ONE_SHOT);
//
//        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
//                .setSmallIcon(R.drawable.md_survey)
//                .setColor(ContextCompat.getColor(TrafficSenseApplication.getContext(),R.color.colorTilting))
//                .setWhen(System.currentTimeMillis())
//                .setContentTitle(getString(R.string.survey_notification_title))
//                .setContentText(messageBody)
//                .setWhen(System.currentTimeMillis())
//                .setAutoCancel(true)
//                .setSound(defaultSoundUri)
//                .setContentIntent(pendingIntent);
//
//        NotificationManager notificationManager =
//                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//
//        notificationManager.notify(SURVEY_NOTIFICATION_ID /* ID of notification */, notificationBuilder.build());
//    }
}
