package fi.aalto.trafficsense.trafficsense.util;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import com.google.android.gms.location.DetectedActivity;
import fi.aalto.trafficsense.trafficsense.R;
import fi.aalto.trafficsense.trafficsense.TrafficSenseApplication;

public enum ActivityType {
    /* The following order is same as defined in DetectedActivity's constants, values: 0..9 */
    IN_VEHICLE, ON_BICYCLE, ON_FOOT, RUNNING, STILL, TILTING, UNKNOWN, WALKING;

    public static ActivityType getActivityTypeByReference(final int activityTypeReference) {
        switch (activityTypeReference) {
            case DetectedActivity.IN_VEHICLE:
                return ActivityType.IN_VEHICLE;
            case DetectedActivity.ON_BICYCLE:
                return ActivityType.ON_BICYCLE;
            case DetectedActivity.ON_FOOT:
                return ActivityType.ON_FOOT;
            case DetectedActivity.RUNNING:
                return ActivityType.RUNNING;
            case DetectedActivity.STILL:
                return ActivityType.STILL;
            case DetectedActivity.TILTING:
                return ActivityType.TILTING;
            case DetectedActivity.UNKNOWN:
                return ActivityType.UNKNOWN;
            case DetectedActivity.WALKING:
                return ActivityType.WALKING;
            default:
                return ActivityType.UNKNOWN;
        }
    }

    public static int getActivityTypeAsInteger(final ActivityType activityType) {
        switch (activityType) {
            case IN_VEHICLE:
                return DetectedActivity.IN_VEHICLE;
            case ON_BICYCLE:
                return DetectedActivity.ON_BICYCLE;
            case ON_FOOT:
                return DetectedActivity.ON_FOOT;
            case RUNNING:
                return DetectedActivity.RUNNING;
            case STILL:
                return DetectedActivity.STILL;
            case TILTING:
                return DetectedActivity.TILTING;
            case UNKNOWN:
                return DetectedActivity.UNKNOWN;
            case WALKING:
                return DetectedActivity.WALKING;
            default:
                return DetectedActivity.UNKNOWN;
        }
    }

    public static String getActivityTypeStringByReference(final int activityTypeReference) {
        return getActivityTypeByReference(activityTypeReference).name();
    }

    public static String getActivityString(ActivityType type) {
        Resources res = TrafficSenseApplication.getContext().getResources();
        switch(type) {
            case IN_VEHICLE:
                return res.getString(R.string.in_vehicle);
            case ON_BICYCLE:
                return res.getString(R.string.on_bicycle);
            case RUNNING:
                return res.getString(R.string.running);
            case STILL:
                return res.getString(R.string.still);
            case TILTING:
                return res.getString(R.string.tilting);
            case UNKNOWN:
                return res.getString(R.string.unknown);
            case WALKING:
                return res.getString(R.string.walking);
            default:
                return res.getString(R.string.unidentifiable_activity, getActivityTypeAsInteger(type));
        }
    }

    public static int getActivityIcon(ActivityType type) {
        Resources res = TrafficSenseApplication.getContext().getResources();
        switch(type) {
            case IN_VEHICLE:
                return R.drawable.md_activity_vehicle_24dp;
            case ON_BICYCLE:
                return R.drawable.md_activity_bicycle_24dp1;
            case RUNNING:
                return R.drawable.md_activity_running_24dp;
            case STILL:
                return R.drawable.md_activity_still;
            case TILTING:
                return R.drawable.md_activity_tilting_24dp;
            case UNKNOWN:
                return R.drawable.md_activity_unknown_24dp;
            case WALKING:
                return R.drawable.md_activity_walking_24dp;
            default:
                return R.drawable.md_activity_unknown_24dp;
        }
    }

}