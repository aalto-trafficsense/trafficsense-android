package fi.aalto.trafficsense.trafficsense.util;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;

import java.util.*;

public class ActivityData {
    public final ImmutableCollection<SensedActivity> Activities;

    public ActivityData() {
        Activities = ImmutableList.of();
    }

    public ActivityData(int activityType, int confidence) {
        SensedActivity[] activities = new SensedActivity[]{new SensedActivity(activityType, confidence)};
        Activities = ImmutableList.copyOf(activities);
    }

    public ActivityData(Map<Integer, Integer> activityConfidenceMap) {
        ArrayList<SensedActivity> activities = new ArrayList<>();
        for (Integer key : activityConfidenceMap.keySet())
            activities.add(new SensedActivity(key, activityConfidenceMap.get(key)));

        // Map is not in any guaranteed order -> sort results descending by conf. before using them
        Collections.sort(activities, new ActivityDataComparator(true));
        Activities = ImmutableList.copyOf(activities);
    }

    public int numOfDataEntries() {
        return Activities.size();
    }

    public SensedActivity getFirst() {
        if (Activities.size() < 1)
            return null;

        return get(0);
    }

    public SensedActivity get(int i) {
        return Activities.asList().get(i);
    }

    public boolean equals(ActivityData other) {
        if (other == null || numOfDataEntries() != other.numOfDataEntries())
            return false;


        for(int i = 0; i < numOfDataEntries(); ++i) {
            if (!get(i).equals(other.get(i)))
                return false;
        }

        return true;
    }

    public List<SensedActivity> getAll() {
        return Activities.asList();
    }

    @Override
    public String toString() {
        StringBuilder activities = new StringBuilder();
        List<SensedActivity> list  = getAll();
        for (int i = 0; i < list.size(); ++i) {
            SensedActivity a = list.get(i);
            if (i >= 0)
                activities.append(", ");

            activities
                    .append("{mActivityType=")
                    .append(a.Type)
                    .append(", mConfidence=")
                    .append(a.Confidence)
                    .append("}");
        }

        return String.format("[%s]", activities.toString());
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

    public class ActivityDataComparator implements Comparator<SensedActivity> {

        private final boolean _descendingOrder;
        public ActivityDataComparator(boolean descendingOrder) {
            _descendingOrder = descendingOrder;
        }

        @Override
        public int compare(SensedActivity lhs, SensedActivity rhs) {
            final SensedActivity first;
            final SensedActivity second;

            if (_descendingOrder) {
                first = rhs;
                second = lhs;
            }
            else {
                first = lhs;
                second = rhs;
            }

            if (first == null && second == null)
                return 0;
            else if (second == null)
                return -1;

            else if (first == null)
                return 1;

            final Integer firstConf = first.Confidence;
            final Integer secondConf = second.Confidence;

            return firstConf.compareTo(secondConf);

        }
    }

}
