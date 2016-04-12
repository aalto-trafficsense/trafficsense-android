package fi.aalto.trafficsense.trafficsense.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Switch;
import android.widget.TextView;
import fi.aalto.trafficsense.trafficsense.R;
import fi.aalto.trafficsense.trafficsense.util.ActivityData;
import fi.aalto.trafficsense.trafficsense.util.InternalBroadcasts;
import fi.aalto.trafficsense.trafficsense.util.TSServiceState;
import timber.log.Timber;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import static fi.aalto.trafficsense.trafficsense.util.InternalBroadcasts.KEY_DEBUG_SETTINGS_REQ;
import static fi.aalto.trafficsense.trafficsense.util.InternalBroadcasts.LABEL_SERVICE_STATE_INDEX;

/**
 * Fragment to trace view client parameters
 */
public class DebugShowFragment extends Fragment {

    private BroadcastReceiver mBroadcastReceiver;
    private LocalBroadcastManager mLocalBroadcastManager;

    /* UI Components */
    private TextView mServiceStatusTextField;
    private TextView mLatestActivitiesTextField;
    private TextView mLocationProviderTextField;
    private TextView mLocationTimeTextField;
    private TextView mLocationAccuracyTextField;


    /* Display values */
    private TSServiceState DS_ServiceState;

    public DebugShowFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_debug_show, container, false);
    }

    @Override
    public void onActivityCreated(Bundle bundle)
    {
        super.onActivityCreated(bundle);
        initFields();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
        initBroadcastReceiver();
        simpleBroadcast(InternalBroadcasts.KEY_DEBUG_SHOW_REQ);
    }

    @Override
    public void onPause() {
        super.onPause();
        mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private void initFields() {
        Timber.d("initFields called");
        mServiceStatusTextField = (TextView) getActivity().findViewById(R.id.debug_show_service_state);
        mLatestActivitiesTextField = (TextView) getActivity().findViewById(R.id.debug_show_activities);
        mLocationProviderTextField = (TextView) getActivity().findViewById(R.id.debug_show_location_provider);
        mLocationTimeTextField = (TextView) getActivity().findViewById(R.id.debug_show_location_time);
        mLocationAccuracyTextField = (TextView) getActivity().findViewById(R.id.debug_show_location_accuracy);

    }

    // Broadcaster
    private void simpleBroadcast(String messageType) {
        if (mLocalBroadcastManager != null)
        {
            Intent intent = new Intent(messageType);
            mLocalBroadcastManager.sendBroadcast(intent);
        }
    }

    /* Local broadcast receiver */
    private void initBroadcastReceiver() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                switch (action) {
                    case InternalBroadcasts.KEY_DEBUG_SHOW:
                        // Add other stuff here later
                        break;
                    case InternalBroadcasts.KEY_SERVICE_STATE_UPDATE:
                        TSServiceState newState = TSServiceState.values()[intent.getIntExtra(LABEL_SERVICE_STATE_INDEX,0)];
                        mServiceStatusTextField.setText(TSServiceState.getServiceStateString(newState));
                        break;
                    case InternalBroadcasts.KEY_LOCATION_UPDATE:
                        Location l = intent.getParcelableExtra(InternalBroadcasts.KEY_LOCATION_UPDATE);
                        mLocationProviderTextField.setText(l.getProvider());
                        Date date = new Date(l.getTime());
                        DateFormat formatter = new SimpleDateFormat("HH:mm:ss");
                        mLocationTimeTextField.setText(formatter.format(date));
                        mLocationAccuracyTextField.setText(String.format("%.0fm", l.getAccuracy()));
                        break;
                    case InternalBroadcasts.KEY_ACTIVITY_UPDATE:
                        ActivityData a = intent.getParcelableExtra(InternalBroadcasts.KEY_ACTIVITY_UPDATE);
                        mLatestActivitiesTextField.setText(a.toString());
                        break;
                }
            }
        };

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(InternalBroadcasts.KEY_SERVICE_STATE_UPDATE);
        intentFilter.addAction(InternalBroadcasts.KEY_LOCATION_UPDATE);
        intentFilter.addAction(InternalBroadcasts.KEY_ACTIVITY_UPDATE);
        intentFilter.addAction(InternalBroadcasts.KEY_DEBUG_SHOW);

        mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, intentFilter);
    }


}
