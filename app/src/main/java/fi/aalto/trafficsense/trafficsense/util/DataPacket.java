package fi.aalto.trafficsense.trafficsense.util;

import fi.aalto.trafficsense.trafficsense.util.ActivityData;
import fi.aalto.trafficsense.trafficsense.util.LocationData;

/**
 * Media data object to bind activity data with location data
 **/

public class DataPacket {
    final LocationData mLocationData;
    final ActivityData mActivityData;

    public DataPacket(LocationData locationData, ActivityData activityData) {
        this.mLocationData = locationData;
        this.mActivityData = activityData;
    }

    public LocationData getLocationData() {
        return mLocationData;
    }

    public ActivityData getActivityData() {
        return mActivityData;
    }
}
