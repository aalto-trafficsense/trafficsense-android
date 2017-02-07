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

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Instructions for sending a DATA MESSAGE are here:
        // http://stackoverflow.com/questions/37711082/how-to-handle-notification-when-app-in-background-in-firebase/37845174#37845174

        if (remoteMessage.getData().size() > 0) {
            ServerNotification sn = new ServerNotification(remoteMessage);

            if (sn.shouldCreateNotification()) {
                sn.postNotification();

            }

        }

    }
}
