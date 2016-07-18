package fi.aalto.trafficsense.trafficsense.util;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import timber.log.Timber;

/**
 * Manage the viewport window bounds for a Google Maps view
 *
 * Created by mikko.rinne@aalto.fi on 18/07/16.
 */
public class MapBounds {

    private LatLngBounds.Builder mBuilder;
    private int numPoints;

    // Public constructor
    public MapBounds() {
        numPoints = 0;
        mBuilder = new LatLngBounds.Builder();
    }

    public void include(LatLng pos) {
        mBuilder.include(pos);
        numPoints++;
    }

    public void update(GoogleMap map) {
        if ((numPoints > 1) && (map != null)) {
            Timber.d("MapBounds update moving camera with: %d points",numPoints);
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(mBuilder.build(), 20));
        }
    }
}
