package fi.aalto.trafficsense.trafficsense.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import fi.aalto.trafficsense.trafficsense.R;
import fi.aalto.trafficsense.trafficsense.util.BroadcastHelper;
import fi.aalto.trafficsense.trafficsense.util.InternalBroadcasts;
import fi.aalto.trafficsense.trafficsense.util.TSServiceState;
import timber.log.Timber;

import static fi.aalto.trafficsense.trafficsense.util.InternalBroadcasts.KEY_DEBUG_SETTINGS_REQ;
import static fi.aalto.trafficsense.trafficsense.util.InternalBroadcasts.LABEL_STATE_INDEX;

/**
 * Fragment to host debug setting switches
 */
public class DebugOldSettingsFragment extends Fragment {

    private BroadcastReceiver mBroadcastReceiver;
    private LocalBroadcastManager mLocalBroadcastManager;

    /* UI Components */
    private Switch mToggleServiceSwitch;
    private TextView mServiceStatusTextField;

    /* Display values */
//    private TSServiceState DS_ServiceState;

    public DebugOldSettingsFragment() {
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
        return inflater.inflate(R.layout.fragment_debug_settings, container, false);
    }

    @Override
    public void onActivityCreated(Bundle bundle)
    {
        super.onActivityCreated(bundle);
        initFields();
        initButtonHandlers();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
        initBroadcastReceiver();
        BroadcastHelper.simpleBroadcast(mLocalBroadcastManager, KEY_DEBUG_SETTINGS_REQ);
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
        mToggleServiceSwitch = (Switch) getActivity().findViewById(R.id.debug_settings_ToggleServiceSwitch);
        mServiceStatusTextField = (TextView) getActivity().findViewById(R.id.debug_setting_ServiceStateIndicator);
    }

    private void initButtonHandlers() {
        Timber.d("initButtonHandlers called");
        // Service toggle
        if (mToggleServiceSwitch != null) {
            mToggleServiceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    toggleServiceState(isChecked);
                }
            });
        }
    }


    private void toggleServiceState(boolean start) {
        if (start) {
            Timber.d("Service start switch on.");
            BroadcastHelper.simpleBroadcast(mLocalBroadcastManager, InternalBroadcasts.KEY_SERVICE_START);
        } else {
            Timber.d("Service start switch off.");
            BroadcastHelper.simpleBroadcast(mLocalBroadcastManager, InternalBroadcasts.KEY_SERVICE_STOP);
        }

    }

    private void updateServiceSwitchState(TSServiceState state) {
        // Init upload toggle state
        if (mToggleServiceSwitch != null) {
            switch (state) {
                case STOPPED:
                case STOPPING:
                    mToggleServiceSwitch.setChecked(false);
                    break;
                case RUNNING:
                case SLEEPING:
                case STARTING:
                    mToggleServiceSwitch.setChecked(true);
                    break;
                default:
                    mToggleServiceSwitch.setChecked(false);
            }
        }

    }

//    // Broadcaster
//    private void simpleBroadcast(String messageType) {
//        if (mLocalBroadcastManager != null)
//        {
//            Intent intent = new Intent(messageType);
//            mLocalBroadcastManager.sendBroadcast(intent);
//        }
//    }

    /* Local broadcast receiver */
    private void initBroadcastReceiver() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                switch (action) {
                    case InternalBroadcasts.KEY_DEBUG_SETTINGS:
                        // Add other stuff here later
                        break;
                    case InternalBroadcasts.KEY_SERVICE_STATE_UPDATE:
                        TSServiceState newState = TSServiceState.values()[intent.getIntExtra(LABEL_STATE_INDEX,0)];
                        mServiceStatusTextField.setText(TSServiceState.getServiceStateString(newState));
                        updateServiceSwitchState(newState);
                        break;
                }
            }
        };

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(InternalBroadcasts.KEY_SERVICE_STATE_UPDATE);
        intentFilter.addAction(InternalBroadcasts.KEY_DEBUG_SETTINGS);

        mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, intentFilter);
    }

}
