package fi.aalto.trafficsense.trafficsense.util;

import android.content.pm.PackageManager;
import android.os.Build;
import fi.aalto.trafficsense.trafficsense.R;
import fi.aalto.trafficsense.trafficsense.TrafficSenseApplication;

/**
 * Retrieve environmental variable-type information and
 * process related strings
 *
 * Created by mikko.rinne@aalto.fi on 07/11/16.
 */

public class EnvInfo {

    private static BackendStorage mStorage;

    public static String getClientNumberString() {
        String clientNumberString;
        if (mStorage == null) {
            mStorage = BackendStorage.create(TrafficSenseApplication.getContext());
        }
        if (mStorage.isClientNumberAvailable()) {
            clientNumberString = String.format("%d", mStorage.readClientNumber().get());
        } else {
            clientNumberString = TrafficSenseApplication.getContext().getString(R.string.not_available);
        }
        return clientNumberString;
    }

    public static String getClientVersionString() {
        String clientVersionString = "";
        try {
            clientVersionString = TrafficSenseApplication.getContext().getPackageManager().getPackageInfo(TrafficSenseApplication.getContext().getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            clientVersionString = TrafficSenseApplication.getContext().getString(R.string.not_available);
        }
        return clientVersionString;
    }

    public static String replaceUriFields(String uriString) {
        uriString = uriString.replace("client_number", getClientNumberString());
        uriString = uriString.replace("client_version", getClientVersionString());
        uriString = uriString.replace("phone_model", Build.MODEL);
        return uriString;
    }

}
