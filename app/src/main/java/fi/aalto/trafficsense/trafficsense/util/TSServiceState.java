package fi.aalto.trafficsense.trafficsense.util;

import android.content.res.Resources;
import fi.aalto.trafficsense.trafficsense.R;
import fi.aalto.trafficsense.trafficsense.TrafficSenseApplication;

public enum TSServiceState {
    STARTING, RUNNING, SLEEPING, STOPPING, STOPPED;

    public static String getServiceStateString(TSServiceState state) {
        Resources res = TrafficSenseApplication.getContext().getResources();
        switch (state) {
            case STARTING:
                return res.getString(R.string.service_state_starting);
            case RUNNING:
                return res.getString(R.string.service_state_running);
            case SLEEPING:
                return res.getString(R.string.service_state_sleeping);
            case STOPPING:
                return res.getString(R.string.service_state_stopping);
            case STOPPED:
                return res.getString(R.string.service_state_stopped);
            default:
                return res.getString(R.string.service_state_unknown);
        }
    }

}