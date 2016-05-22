package fi.aalto.trafficsense.trafficsense.util;

import android.content.res.Resources;
import fi.aalto.trafficsense.trafficsense.R;
import fi.aalto.trafficsense.trafficsense.TrafficSenseApplication;

/**
 * Created by mikko.rinne@aalto.fi on 17/04/16.
 */
public enum TSUploadState {
    SWITCHEDOFF, SIGNEDOUT, NOCLIENTNUMBER, READY, INPROGRESS, FAILED, DISABLED;

    public static String getUploadStateString(TSUploadState state) {
        Resources res = TrafficSenseApplication.getContext().getResources();
        switch (state) {
            case SWITCHEDOFF:
                return res.getString(R.string.upload_state_switchedoff);
            case SIGNEDOUT:
                return res.getString(R.string.upload_state_signedout);
            case NOCLIENTNUMBER:
                return res.getString(R.string.upload_state_noclientnumber);
            case READY:
                return res.getString(R.string.upload_state_ready);
            case INPROGRESS:
                return res.getString(R.string.upload_state_inprogress);
            case FAILED:
                return res.getString(R.string.upload_state_failed);
            case DISABLED:
                return res.getString(R.string.upload_state_disabled);
            default:
                return res.getString(R.string.upload_state_unknown);
        }
    }

}
