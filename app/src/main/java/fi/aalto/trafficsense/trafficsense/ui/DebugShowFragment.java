package fi.aalto.trafficsense.trafficsense.ui;

import android.content.*;
import android.content.res.Resources;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableRow;
import android.widget.TextView;
import fi.aalto.trafficsense.trafficsense.R;
import fi.aalto.trafficsense.trafficsense.util.*;
import timber.log.Timber;

import java.text.DateFormat;
import java.util.Date;

import static android.support.v7.preference.PreferenceManager.getDefaultSharedPreferences;
import static fi.aalto.trafficsense.trafficsense.util.InternalBroadcasts.LABEL_CLIENT_NUMBER;
import static fi.aalto.trafficsense.trafficsense.util.InternalBroadcasts.LABEL_STATE_INDEX;

/**
 * Fragment to trace view client parameters
 */
public class DebugShowFragment extends Fragment {

    private BroadcastReceiver mBroadcastReceiver;
    private LocalBroadcastManager mLocalBroadcastManager;
    private SharedPreferences mSettings; // Application settings
    private Resources mRes;

    private long activityIntervalTimer;
    private long locationIntervalTimer;

    /* UI Components */
    private TableRow mServiceHeaderRow;
    private TextView mServiceLabelTextField;
    private TextView mServiceStatusTextField;
    private TextView mClientNumberTextField;

    private TableRow mActivityHeaderRow;
    private TextView mActivityLabelTextField;
    private TextView mTopActivityTextField;
    private TextView mActivityListLabelTextField;
    private TextView mLatestActivitiesTextField;
    private TextView mActivityTimeTextField;

    private TableRow mLocationHeaderRow;
    private TextView mLocationLabelTextField;
    private TextView mLocationStatusTextField;
    private TextView mLocationAccuracyLabelTextField;
    private TextView mLocationAccuracyTextField;
    private TextView mLocationTimeTextField;

    private TableRow mUploadHeaderRow;
    private TextView mUploadLabelTextField;
    private TextView mUploadStatusTextField;
    private TextView mUploadQueueLengthTextField;
    private TextView mUploadTimeTextField;

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
        mRes = this.getResources();
        mSettings = getDefaultSharedPreferences(getActivity());
    }

    @Override
    public void onResume()
    {
        super.onResume();
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
        initBroadcastReceiver();
        BroadcastHelper.simpleBroadcast(mLocalBroadcastManager, InternalBroadcasts.KEY_DEBUG_SHOW_REQ);
        activityIntervalTimer = 0;
        locationIntervalTimer = 0;
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
        mServiceHeaderRow = (TableRow) getActivity().findViewById(R.id.debug_show_service_header_row);
        mServiceLabelTextField = (TextView) getActivity().findViewById(R.id.debug_show_service_label);
        mServiceStatusTextField = (TextView) getActivity().findViewById(R.id.debug_show_service_state);
        mClientNumberTextField = (TextView) getActivity().findViewById(R.id.debug_show_client_number);

        mActivityHeaderRow = (TableRow) getActivity().findViewById(R.id.debug_show_activity_header_row);
        mActivityLabelTextField = (TextView) getActivity().findViewById(R.id.debug_show_activity_label);
        mTopActivityTextField = (TextView) getActivity().findViewById(R.id.debug_show_activity_top);
        mActivityListLabelTextField = (TextView) getActivity().findViewById(R.id.debug_show_activity_list_label);
        mLatestActivitiesTextField = (TextView) getActivity().findViewById(R.id.debug_show_activities);
        mActivityTimeTextField = (TextView) getActivity().findViewById(R.id.debug_show_activity_time);

        mLocationHeaderRow = (TableRow) getActivity().findViewById(R.id.debug_show_location_header_row);
        mLocationLabelTextField = (TextView) getActivity().findViewById(R.id.debug_show_location_label);
        mLocationStatusTextField = (TextView) getActivity().findViewById(R.id.debug_show_location_provider);
        mLocationTimeTextField = (TextView) getActivity().findViewById(R.id.debug_show_location_time);
        mLocationAccuracyLabelTextField = (TextView) getActivity().findViewById(R.id.debug_show_location_accuracy_label);
        mLocationAccuracyTextField = (TextView) getActivity().findViewById(R.id.debug_show_location_accuracy);

        mUploadHeaderRow = (TableRow) getActivity().findViewById(R.id.debug_show_upload_header_row);
        mUploadLabelTextField = (TextView) getActivity().findViewById(R.id.debug_show_upload_label);
        mUploadStatusTextField = (TextView) getActivity().findViewById(R.id.debug_show_upload_state);
        mUploadQueueLengthTextField = (TextView) getActivity().findViewById(R.id.debug_show_queue_length);
        mUploadTimeTextField = (TextView) getActivity().findViewById(R.id.debug_show_latest_upload);

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
                        updateServiceState (intent);
                        break;
                    case InternalBroadcasts.KEY_UPLOAD_STATE_UPDATE:
                        updateUploadState(intent);
                        break;
                    case InternalBroadcasts.KEY_LOCATION_UPDATE:
                        updateLocation(intent);
                        break;
                    case InternalBroadcasts.KEY_ACTIVITY_UPDATE:
                        updateActivity(intent);
                        break;
                    case InternalBroadcasts.KEY_SENSORS_UPDATE:
                        updateLocation(intent);
                        updateActivity(intent);
                        break;
                    case InternalBroadcasts.KEY_QUEUE_LENGTH_UPDATE:
                        updateQueueLength(intent);
                        break;
                    case InternalBroadcasts.KEY_UPLOAD_TIME:
                        updateLatestUpload(intent);
                        break;
                }
            }
        };

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(InternalBroadcasts.KEY_SERVICE_STATE_UPDATE);
        intentFilter.addAction(InternalBroadcasts.KEY_UPLOAD_STATE_UPDATE);
        intentFilter.addAction(InternalBroadcasts.KEY_LOCATION_UPDATE);
        intentFilter.addAction(InternalBroadcasts.KEY_ACTIVITY_UPDATE);
        intentFilter.addAction(InternalBroadcasts.KEY_SENSORS_UPDATE);
        intentFilter.addAction(InternalBroadcasts.KEY_QUEUE_LENGTH_UPDATE);
        intentFilter.addAction(InternalBroadcasts.KEY_UPLOAD_TIME);
        intentFilter.addAction(InternalBroadcasts.KEY_DEBUG_SHOW);

        mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, intentFilter);
    }

    private void updateServiceState (Intent i) {
        TSServiceState newState = TSServiceState.values()[i.getIntExtra(LABEL_STATE_INDEX,0)];
        mServiceStatusTextField.setText(TSServiceState.getServiceStateString(newState));
        int clientNumber = i.getIntExtra(LABEL_CLIENT_NUMBER,-1);
        if (clientNumber == -1) mClientNumberTextField.setText(mRes.getString(R.string.not_available));
        else mClientNumberTextField.setText(String.format("%d", clientNumber));
        int headerTextColor;
        int headerBackgroundColor;
        switch (newState) {
            case STOPPED:
                headerTextColor = R.color.grayText;
                headerBackgroundColor = R.color.colorStill;
                mLocationStatusTextField.setText(R.string.location_state_off);
                break;
            case SLEEPING:
                headerTextColor = R.color.colorSubtitleText;
                headerBackgroundColor = R.color.colorTilting;
                mLocationStatusTextField.setText(R.string.location_state_nopower);
                break;
            default:
                headerTextColor = R.color.normalText;
                headerBackgroundColor = R.color.colorRunning;
                mLocationStatusTextField.setText(R.string.location_state_high);
                break;
        }

        mServiceLabelTextField.setTextColor(ContextCompat.getColor(this.getContext(), headerTextColor));
        mServiceStatusTextField.setTextColor(ContextCompat.getColor(this.getContext(), headerTextColor));
        mServiceHeaderRow.setBackgroundColor(ContextCompat.getColor(this.getContext(), headerBackgroundColor));

//        mServiceStatusTextField.setGravity(Gravity.END);
//        mClientNumberTextField.setGravity(Gravity.END);

    }

    private void updateUploadState (Intent i) {
        TSUploadState newState = TSUploadState.values()[i.getIntExtra(LABEL_STATE_INDEX,0)];
        mUploadStatusTextField.setText(TSUploadState.getUploadStateString(newState));
        int headerTextColor;
        int headerBackgroundColor;
        switch (newState) {
            case SWITCHEDOFF:
                headerTextColor = R.color.grayText;
                headerBackgroundColor = R.color.colorStill;
                break;
            case SIGNEDOUT:
                headerTextColor = R.color.normalText;
                headerBackgroundColor = R.color.colorBus;
                break;
            case NOCLIENTNUMBER:
            case FAILED:
                headerTextColor = R.color.colorSubtitleText;
                headerBackgroundColor = R.color.colorInVehicle;
                break;
            case INPROGRESS:
                headerTextColor = R.color.normalText;
                headerBackgroundColor = R.color.colorRunning;
                break;
            case DISABLED:
                headerTextColor = R.color.colorSubtitleText;
                headerBackgroundColor = R.color.colorTilting;
                break;
            default:
                headerTextColor = R.color.colorSubtitleText;
                headerBackgroundColor = R.color.colorWalking;
                break;
        }

        mUploadLabelTextField.setTextColor(ContextCompat.getColor(this.getContext(), headerTextColor));
        mUploadStatusTextField.setTextColor(ContextCompat.getColor(this.getContext(), headerTextColor));
        mUploadHeaderRow.setBackgroundColor(ContextCompat.getColor(this.getContext(), headerBackgroundColor));

//        mUploadStatusTextField.setGravity(Gravity.END);

    }

    private void updateLocation (Intent i) {
        if (i.hasExtra(InternalBroadcasts.KEY_LOCATION_UPDATE)) {
            Location l = i.getParcelableExtra(InternalBroadcasts.KEY_LOCATION_UPDATE);

            Date locationDate = new Date(l.getTime());
            StringBuilder locTime = new StringBuilder(DateFormat.getTimeInstance().format(locationDate));
            if (locationIntervalTimer > 0) {
                locTime.append(" ").append(mRes.getString(R.string.interval))
                        .append(" ").append(((System.currentTimeMillis()-locationIntervalTimer)/1000))
                        .append(" ").append(mRes.getString(R.string.seconds));
            }
            locationIntervalTimer = System.currentTimeMillis();
            mLocationTimeTextField.setText(locTime.toString());

            float acc = l.getAccuracy();
            mLocationAccuracyTextField.setText(String.format("%.0fm", acc));
            if (acc >= (float) mSettings.getInt(mRes.getString(R.string.debug_settings_location_accuracy_key), 50)) {
                mLocationLabelTextField.setTextColor(ContextCompat.getColor(this.getContext(), R.color.colorSubtitleText));
                mLocationStatusTextField.setTextColor(ContextCompat.getColor(this.getContext(), R.color.colorSubtitleText));
                mLocationHeaderRow.setBackgroundColor(ContextCompat.getColor(this.getContext(), R.color.colorInVehicle));
            } else {
                mLocationLabelTextField.setTextColor(ContextCompat.getColor(this.getContext(), R.color.colorSubtitleText));
                mLocationStatusTextField.setTextColor(ContextCompat.getColor(this.getContext(), R.color.colorSubtitleText));
                mLocationHeaderRow.setBackgroundColor(ContextCompat.getColor(this.getContext(), R.color.colorWalking));
            }
        }

//        mLocationStatusTextField.setGravity(Gravity.END);
//        mLocationAccuracyTextField.setGravity(Gravity.END);
    }

    private void updateActivity (Intent i) {
        if (i.hasExtra(InternalBroadcasts.KEY_ACTIVITY_UPDATE)) {
            ActivityData a = i.getParcelableExtra(InternalBroadcasts.KEY_ACTIVITY_UPDATE);
            mTopActivityTextField.setText(a.getFirstString());
            ActivityType topActivity=a.getFirst().Type;
            mLatestActivitiesTextField.setText(a.toString());

            StringBuilder actTime = new StringBuilder(a.timeString());
            if (activityIntervalTimer > 0) {
                actTime.append(" ").append(mRes.getString(R.string.interval))
                        .append(" ").append((System.currentTimeMillis()-activityIntervalTimer)/1000)
                        .append(" ").append(mRes.getString(R.string.seconds));
            }
            activityIntervalTimer = System.currentTimeMillis();
            mActivityTimeTextField.setText(actTime.toString());
            int headerTextColor;
            int headerBackgroundColor;
            switch (topActivity) {
                case IN_VEHICLE:
                    headerTextColor = R.color.colorSubtitleText;
                    headerBackgroundColor = R.color.colorInVehicle;
                    break;
                case ON_BICYCLE:
                    headerTextColor = R.color.colorSubtitleText;
                    headerBackgroundColor = R.color.colorOnBicycle;
                    break;
                case RUNNING:
                    headerTextColor = R.color.normalText;
                    headerBackgroundColor = R.color.colorRunning;
                    break;
                case STILL:
                    headerTextColor = R.color.normalText;
                    headerBackgroundColor = R.color.colorStill;
                    break;
                case TILTING:
                    headerTextColor = R.color.colorSubtitleText;
                    headerBackgroundColor = R.color.colorTilting;
                    break;
                case UNKNOWN:
                    headerTextColor = R.color.colorSubtitleText;
                    headerBackgroundColor = R.color.colorUnknown;
                    break;
                case WALKING:
                    headerTextColor = R.color.colorSubtitleText;
                    headerBackgroundColor = R.color.colorWalking;
                    break;
                default:
                    headerTextColor = R.color.colorSubtitleText;
                    headerBackgroundColor = R.color.colorUnknown;
                    break;
            }
            mActivityLabelTextField.setTextColor(ContextCompat.getColor(this.getContext(), headerTextColor));
            mTopActivityTextField.setTextColor(ContextCompat.getColor(this.getContext(), headerTextColor));
            mActivityHeaderRow.setBackgroundColor(ContextCompat.getColor(this.getContext(), headerBackgroundColor));

//            mTopActivityTextField.setGravity(Gravity.END);
//            mLatestActivitiesTextField.setGravity(Gravity.END);

        }
    }

    private void updateQueueLength (Intent i) {
        int queueLength = i.getIntExtra(InternalBroadcasts.LABEL_QUEUE_LENGTH,0);
        int activeThreshold = i.getIntExtra(InternalBroadcasts.LABEL_QUEUE_THRESHOLD,0);
        mUploadQueueLengthTextField.setText(queueLength + " / " + activeThreshold);
//        mUploadQueueLengthTextField.setGravity(Gravity.END);
    }

    private void updateLatestUpload (Intent i) {
        long latestUploadMillis = i.getLongExtra(InternalBroadcasts.KEY_UPLOAD_TIME,0);
        if (latestUploadMillis == 0) {
            mUploadTimeTextField.setText(mRes.getString(R.string.not_available));
        } else {
            Date uploadDate = new Date(latestUploadMillis);
            String uploadFormatted = DateFormat.getDateTimeInstance().format(uploadDate);
            mUploadTimeTextField.setText(uploadFormatted);
        }

//        mUploadTimeTextField.setGravity(Gravity.END);
    }

}
