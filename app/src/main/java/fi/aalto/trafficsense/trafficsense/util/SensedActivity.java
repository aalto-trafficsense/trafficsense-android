package fi.aalto.trafficsense.trafficsense.util;

import com.google.android.gms.location.DetectedActivity;

/**
 * Created by rinnem2 on 09/04/16.
 */

public class SensedActivity {
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

    public String asString() {
        //TODO: zzho cannot be official, prepare a better method.
        return DetectedActivity.zzho(Type);
    }

    @Override
    public String toString() {
        return asString();
    }
}
