package fi.aalto.trafficsense.trafficsense.backend.backend_util;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

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
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Instructions for sending a DATA MESSAGE are here:
        // http://stackoverflow.com/questions/37711082/how-to-handle-notification-when-app-in-background-in-firebase/37845174#37845174

        // Sample expected survey message:
        /*
{
    "to" : "/topics/surveys",
    "data" : {
"NOTIFICATION_TITLE" : "TrafficSense",
"NOTIFICATION_MESSAGE" : "User Survey 1/2. Press this notification to enter your data for the 1st user survey.",
"NOTIFICATION_URI" : "https://docs.google.com/forms/d/e/1FAIpQLSeZ7bxoAu_9JZfZ9aEXjhdkWX4nHUHEm7rIuA6govdCVWSHLg/viewform?entry.1842912509&entry.355863673&entry.1724961827&entry.1449005772=client_number&entry.894508572=client_version&entry.1791597625=phone_model",
"NOTIFICATION_TITLE_FI" : "TrafficSense",
"NOTIFICATION_MESSAGE_FI" : "Käyttäjäkysely 1/2. Painamalla tätä viestiä siirryt vastaamaan 1. käyttäjäkyselyyn.",
"NOTIFICATION_URI_FI" : "https://docs.google.com/forms/d/e/1FAIpQLSct_vTN8Mz4sVvCM9N-NoE5eVGoVWlGTUf_05vWIXgmGfXrcQ/viewform?entry.1637582747&entry.355863673&entry.1724961827&entry.1031700194=client_number&entry.764908055=client_version&entry.779736130=phone_model"
    }
}         */
        // Feedback form test URI:
        // https://docs.google.com/forms/d/1GRvwgUXigE2iclSqAMqd6B2m3SDcs239a9nRrhHOgKM/viewform?entry.1453132440&entry.1290358306=client_number&entry.1714883594=client_version
        //
        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ

        if (remoteMessage.getData().size() > 0) {
            ServerNotification sn = new ServerNotification(remoteMessage);

            if (sn.shouldCreateNotification()) {
                sn.postNotification();

            }

        }

    }
}
