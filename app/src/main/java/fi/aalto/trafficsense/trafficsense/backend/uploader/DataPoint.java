package fi.aalto.trafficsense.trafficsense.backend.uploader;

import com.google.gson.annotations.SerializedName;
import fi.aalto.trafficsense.trafficsense.util.ActivityData;
import fi.aalto.trafficsense.trafficsense.util.LocationData;

public class DataPoint {
    @SerializedName("time")
    public final long mTime;
    @SerializedName("location")
    public final LocationData mLocation;
    @SerializedName("activityData")
    public final ActivityData mActivityData;
    public transient final long mSequence;

    public DataPoint(long time, long sequence, LocationData location, ActivityData mActivities) {
        this.mTime = time;
        this.mSequence = sequence;
        this.mLocation = location;
        this.mActivityData = mActivities;
    }

    @Override
    public String toString() {
        final String activities = mActivityData != null ? mActivityData.toString() : "[]";
        return String.format("DataPoint{mTime=%d, mSequence=%d, mLocation=%s, mActivities=%s}", mTime, mSequence, mLocation, activities);
    }
}
