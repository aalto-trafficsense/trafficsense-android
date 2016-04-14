package fi.aalto.trafficsense.trafficsense.util;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.location.DetectedActivity;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import timber.log.Timber;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class ActivityData implements Parcelable {
    private ArrayList<SensedActivity> Activities;
    private long activityTime = 0;
    private long elapsedTime = 0; // seconds

    public ActivityData(long inTime, long inElapsed) {
        Activities = new ArrayList<>();
        activityTime = inTime;
        elapsedTime = inElapsed;
    }

    public void add(int activityType, int confidence) {
        Activities.add(new SensedActivity(activityType, confidence));
    }

    public int numOfDataEntries() {
        if (Activities != null) return Activities.size();
        return 0;
    }

    public SensedActivity getFirst() {
        if (Activities.size() < 1)
            return null;

        return get(0);
    }

    public String getFirstString() {
        SensedActivity sa = getFirst();
        if (sa != null) return sa.getActivityString();
        else return null;
    }

    public SensedActivity get(int i) {
        return Activities.get(i);
    }

    public List<SensedActivity> getAll() {
        return Activities;
    }

    public String timeString() {
        Date actDate = new Date(activityTime);
        String res = DateFormat.getTimeInstance().format(actDate);
        if (elapsedTime>0) res.concat(" Elapsed: "+elapsedTime+"seconds");
        return res;
    }

    @Override
    public String toString() {
        StringBuilder activities = new StringBuilder();
        for (int i = 0; i < numOfDataEntries(); ++i) {
            SensedActivity a = get(i);
            if (i > 0)
                activities.append("\n");

            activities
                    .append(a.getActivityString())
                    .append(" ")
                    .append(a.Confidence)
                    .append("%");
        }

        return activities.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ActivityData that = (ActivityData) o;

        if (Activities.size() != that.Activities.size())
            return false;

        List<SensedActivity> list1  = getAll();
        List<SensedActivity> list2  = that.getAll();
        for (int i = 0; i < list1.size(); ++i)
            if (!list1.get(i).equals(list2.get(i)))
                return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = 0;
        long temp;
        List<SensedActivity> list  = getAll();
        for (int i = 0; i < list.size(); ++i) {
            temp = (long)list.get(i).hashCode();
            result = 31 * result + (int) (temp ^ (temp >>> 32));
        }

        return result;
    }

    public static final Parcelable.Creator<ActivityData> CREATOR =
            new Parcelable.Creator<ActivityData>() {
                @Override
                public ActivityData createFromParcel(Parcel in) {
                    ActivityData a = new ActivityData(in.readLong(), in.readLong());
                    a.Activities = in.createTypedArrayList(SensedActivity.CREATOR);
                    return a;
                }

                @Override
                public ActivityData[] newArray(int size) {
                    return new ActivityData[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeLong(activityTime);
        parcel.writeLong(elapsedTime);
        parcel.writeTypedList(Activities);
    }


}
