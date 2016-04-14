package fi.aalto.trafficsense.trafficsense.util;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.gson.annotations.SerializedName;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ActivityData implements Parcelable {
    @SerializedName("activities")
    private ArrayList<SensedActivity> Activities;
    private long activityTime = 0;

    public ActivityData(long inTime) {
        Activities = new ArrayList<>();
        activityTime = inTime;
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
        return DateFormat.getTimeInstance().format(actDate);
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
                    ActivityData a = new ActivityData(in.readLong());
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
        parcel.writeTypedList(Activities);
    }


}
