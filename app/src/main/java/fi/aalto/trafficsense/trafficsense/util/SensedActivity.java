package fi.aalto.trafficsense.trafficsense.util;

import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.location.DetectedActivity;
import fi.aalto.trafficsense.trafficsense.R;
import fi.aalto.trafficsense.trafficsense.TrafficSenseApplication;

/**
 * Created by rinnem2 on 09/04/16.
 */

public class SensedActivity implements Parcelable {
    /* Public Members */
    public int Type;
    public int Confidence;

    /* Constructor(s) */
    public SensedActivity(com.google.android.gms.location.DetectedActivity detectedActivity) {
        this(detectedActivity.getType(), detectedActivity.getConfidence());
    }
    public SensedActivity(int activityType, int confidence) {

        this.Type = activityType;
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
        long temp = (long) Type;
        result = (int) (temp ^ (temp >>> 32));
        temp = (long) Confidence;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    public String getActivityString() {
        Resources res = TrafficSenseApplication.getContext().getResources();
        switch(Type) {
            case DetectedActivity.IN_VEHICLE:
                return res.getString(R.string.in_vehicle);
            case DetectedActivity.ON_BICYCLE:
                return res.getString(R.string.on_bicycle);
            case DetectedActivity.RUNNING:
                return res.getString(R.string.running);
            case DetectedActivity.STILL:
                return res.getString(R.string.still);
            case DetectedActivity.TILTING:
                return res.getString(R.string.tilting);
            case DetectedActivity.UNKNOWN:
                return res.getString(R.string.unknown);
            case DetectedActivity.WALKING:
                return res.getString(R.string.walking);
            default:
                return res.getString(R.string.unidentifiable_activity, Type);
        }
    }

    @Override
    public String toString() {
        return getActivityString();
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
        parcel.writeInt(Type);
        parcel.writeInt(Confidence);
    }


}
