package fi.aalto.trafficsense.trafficsense.util;

import android.app.Activity;
import android.content.res.Resources;
import fi.aalto.trafficsense.trafficsense.R;
import timber.log.Timber;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Convert between server path activities and localised activity names and icons
 *
 * Created by mikko.rinne@aalto.fi on 1.2.2017.
 */
public class ActivityPathConverter {

    List<PathActivity> activityList = new ArrayList<>();

    public ActivityPathConverter() {
        // String serverName, boolean hasLineName, int localizedNameResource, boolean isEditable, int lineColor, int icon
        // Note!! non-editables have to be last in the list, as spinner selection is based on the list index!
        activityList.add(new PathActivity("ON_BICYCLE",false, R.string.bicycle, true,  R.color.colorOnBicycle, R.drawable.map_activity_bicycle));
        activityList.add(new PathActivity("WALKING",   false, R.string.walking, true,  R.color.colorWalking,   R.drawable.map_activity_walking));
        activityList.add(new PathActivity("RUNNING",   false, R.string.running, true,  R.color.colorRunning,   R.drawable.map_activity_running));
        activityList.add(new PathActivity("IN_VEHICLE",false, R.string.car,     true,  R.color.colorInVehicle, R.drawable.map_activity_vehicle));
        activityList.add(new PathActivity("TRAIN",     true,  R.string.train,   true,  R.color.colorTrain,     R.drawable.map_vehicle_train));
        activityList.add(new PathActivity("TRAM",      true,  R.string.tram,    true,  R.color.colorSubway,    R.drawable.map_vehicle_subway));
        activityList.add(new PathActivity("SUBWAY",    true,  R.string.subway,  true,  R.color.colorSubway,    R.drawable.map_vehicle_subway));
        activityList.add(new PathActivity("BUS",       true,  R.string.bus,     true,  R.color.colorBus,       R.drawable.map_vehicle_bus));
        activityList.add(new PathActivity("FERRY",     true,  R.string.ferry,   true,  R.color.colorBus,       R.drawable.md_activity_ferry_24dp));
        activityList.add(new PathActivity("STILL",     false, R.string.still,   false, R.color.colorStill,     R.drawable.map_activity_still));
        activityList.add(new PathActivity("TILTING",   false, R.string.tilting, false, R.color.colorTilting,   R.drawable.md_activity_tilting_24dp));
        activityList.add(new PathActivity("UNKNOWN",   false, R.string.unknown, false, R.color.colorUnknown,   R.drawable.md_activity_unknown_24dp));
    }

    public int getLocalizedNameRes(String sName) {
        PathActivity pa = findFromSName(sName);
        if (pa!=null) {
            return pa.getLocalizedNameRes();
        } else {
            return R.string.unknown;
        }
    }

    public int getColor(String sName) {
        PathActivity pa = findFromSName(sName);
        if (pa!=null) {
            return pa.getColor();
        } else {
            return R.color.colorUnknown;
        }
    }

    public int getIcon(String sName) {
        PathActivity pa = findFromSName(sName);
        if (pa!=null) {
            return pa.getIcon();
        } else {
            return R.drawable.md_activity_unknown_24dp;
        }
    }

    public boolean hasLineName(String sName) {
        PathActivity pa = findFromSName(sName);
        if (pa!=null) {
            return pa.hasLineName();
        } else {
            return false;
        }
    }

    public String getServerNameFromIndex(int index) {
        return activityList.get(index).getServerName();
    }

    public int getIndex(String sName) {
        Iterator i = activityList.iterator();
        int idx = 0;
        int res = 0;
        Boolean cnt = true;
        PathActivity pa;
        while (cnt) {
            pa = (PathActivity)i.next();
            if (pa.getServerName().equals(sName)) {
                cnt = false;
                res = idx;
            }
            idx++;
            if (!i.hasNext()) {
                cnt = false;
                Timber.e("ActivityPathConverter / getIndex got an unknown server activity name: %s", sName);
            }
        }
        return res;
    }

    public List<String> getEditList(Activity mActivity) {
        Resources mRes = mActivity.getResources();
        List<String> al = new ArrayList<>();
        for (PathActivity pa: activityList) {
            if (pa.isEditable()) al.add(mRes.getString(pa.getLocalizedNameRes()));
        }
        return al;
    }

    public List<String> getEditableSNames() {
        List<String> al = new ArrayList<>();
        for (PathActivity pa: activityList) {
            if (pa.isEditable()) al.add(pa.getServerName());
        }
        return al;
    }

    private PathActivity findFromSName(String sName) {
        Iterator i = activityList.iterator();
        Boolean cnt = true;
        PathActivity pa = null;
        while (cnt) {
            pa = (PathActivity)i.next();
            if (pa.getServerName().equals(sName)) {
                cnt = false;
            }
            if (!i.hasNext()) {
                cnt = false;
                pa = null;
                Timber.e("ActivityPathConverter / findFromSName got an unknown server activity name: %s", sName);
            }
        }
        return pa;
    }

    private class PathActivity {

        private String serverName;
        private boolean hasLineNameVar;
        private int localizedNameResource;
        private boolean isEditableVar;
        private int color;
        private int icon;

        public PathActivity(String sName, boolean hln, int lNameRes, boolean ie, int color, int icn) {
            this.serverName = sName;
            this.hasLineNameVar = hln;
            this.localizedNameResource = lNameRes;
            this.isEditableVar = ie;
            this.color = color;
            this.icon = icn;
        }

        public String getServerName() {
            return this.serverName;
        }

        public boolean hasLineName() {
            return this.hasLineNameVar;
        }

        public boolean isEditable() {
            return this.isEditableVar;
        }

        public int getLocalizedNameRes() {
            return this.localizedNameResource;
        }

        public int getColor() {
            return this.color;
        }

        public int getIcon() {
            return this.icon;
        }

    }

}
