package fi.aalto.trafficsense.trafficsense.util;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.gson.annotations.SerializedName;

/**
 * Created by rinnem2 on 09/04/16.
 */

public class SensedActivity implements Parcelable {
    /* Public Members */
    @SerializedName("activityType")
    public ActivityType Type;
    @SerializedName("confidence")
    public int Confidence;

    /* Constructor(s) */
    public SensedActivity(com.google.android.gms.location.DetectedActivity detectedActivity) {
        this(detectedActivity.getType(), detectedActivity.getConfidence());
    }
    public SensedActivity(int activityType, int confidence) {

        this.Type = ActivityType.getActivityTypeByReference(activityType);
        this.Confidence = confidence;
    }

    /**
     * Return text representation based on confidence
     **/
    public String getConfidenceLevelAsString() {
        /**
         *  Doc: http://developer.android.com/reference/com/google/android/gms/location/DetectedActivity.html
         *  Documentation states that confidence [0, 100] less than equal to 50 is bad and
         *  greater or equal to 75 is good (likely to be true);
         **/
        String confidenceStr;
        if (Confidence <= 50)
            confidenceStr = "NOT GOOD";
        else if (Confidence <= 75)
            confidenceStr = "OK";
        else if (Confidence < 90)
            confidenceStr = "GOOD";
        else
            confidenceStr = "EXCELLENT";

        return confidenceStr;
    }

    public boolean equals(SensedActivity other) {
        return other != null
                && this.Type == other.Type
                && this.Confidence == other.Confidence;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SensedActivity that = (SensedActivity) o;
        return (equals(that));
    }

    @Override
    public int hashCode() {
        int result;
        long temp = (long) ActivityType.getActivityTypeAsInteger(Type);
        result = (int) (temp ^ (temp >>> 32));
        temp = (long) Confidence;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    public String getActivityString() {
        return ActivityType.getActivityString(Type);
    }

    @Override
    public String toString() {
        return getActivityString()+" "+Confidence+"%";
    }

    public static final Parcelable.Creator<SensedActivity> CREATOR =
            new Parcelable.Creator<SensedActivity>() {
                @Override
                public SensedActivity createFromParcel(Parcel in) {
                    SensedActivity sa = new SensedActivity(in.readInt(), in.readInt());
                    return sa;
                }

                @Override
                public SensedActivity[] newArray(int size) {
                    return new SensedActivity[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(ActivityType.getActivityTypeAsInteger(Type));
        parcel.writeInt(Confidence);
    }


}
