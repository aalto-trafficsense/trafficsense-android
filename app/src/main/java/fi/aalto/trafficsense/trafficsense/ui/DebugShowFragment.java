package fi.aalto.trafficsense.trafficsense.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;
import fi.aalto.trafficsense.trafficsense.R;
import fi.aalto.trafficsense.trafficsense.util.*;

import java.text.DateFormat;
import java.util.Date;

import static fi.aalto.trafficsense.trafficsense.util.InternalBroadcasts.LABEL_STATE_INDEX;
import static fi.aalto.trafficsense.trafficsense.util.TSServiceState.SLEEPING;
import static java.text.DateFormat.getTimeInstance;

/**
 * Fragment to trace view client parameters
 */
public class DebugShowFragment extends Fragment {

    private BroadcastReceiver mBroadcastReceiver;
    private LocalBroadcastManager mLocalBroadcastManager;
    private Resources mRes;

    private long activityIntervalTimer;
    private long locationIntervalTimer;

    /* UI Components */
    private TextView mServiceLabelTextField;
    private TextView mServiceStatusTextField;

    private TextView mActivityLabelTextField;
    private TextView mTopActivityTextField;
    private TextView mActivityListLabelTextField;
    private TextView mLatestActivitiesTextField;
    private TextView mActivityTimeTextField;

    private TextView mLocationLabelTextField;
    private TextView mLocationProviderTextField;
    private TextView mLocationAccuracyLabelTextField;
    private TextView mLocationAccuracyTextField;
    private TextView mLocationTimeTextField;

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

    }

    @Override
    public void onResume()
    {
        super.onResume();
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
        initBroadcastReceiver();
        simpleBroadcast(InternalBroadcasts.KEY_DEBUG_SHOW_REQ);
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
        mServiceLabelTextField = (TextView) getActivity().findViewById(R.id.debug_show_service_label);
        mServiceStatusTextField = (TextView) getActivity().findViewById(R.id.debug_show_service_state);

        mActivityLabelTextField = (TextView) getActivity().findViewById(R.id.debug_show_activity_label);
        mTopActivityTextField = (TextView) getActivity().findViewById(R.id.debug_show_activity_top);
        mActivityListLabelTextField = (TextView) getActivity().findViewById(R.id.debug_show_activity_list_label);
        mLatestActivitiesTextField = (TextView) getActivity().findViewById(R.id.debug_show_activities);
        mActivityTimeTextField = (TextView) getActivity().findViewById(R.id.debug_show_activity_time);

        mLocationLabelTextField = (TextView) getActivity().findViewById(R.id.debug_show_location_label);
        mLocationProviderTextField = (TextView) getActivity().findViewById(R.id.debug_show_location_provider);
        mLocationTimeTextField = (TextView) getActivity().findViewById(R.id.debug_show_location_time);
        mLocationAccuracyLabelTextField = (TextView) getActivity().findViewById(R.id.debug_show_location_accuracy_label);
        mLocationAccuracyTextField = (TextView) getActivity().findViewById(R.id.debug_show_location_accuracy);

        mUploadLabelTextField = (TextView) getActivity().findViewById(R.id.debug_show_upload_label);
        mUploadStatusTextField = (TextView) getActivity().findViewById(R.id.debug_show_upload_state);
        mUploadQueueLengthTextField = (TextView) getActivity().findViewById(R.id.debug_show_queue_length);
        mUploadTimeTextField = (TextView) getActivity().findViewById(R.id.debug_show_latest_upload);

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
        switch (newState) {
            case STOPPED:
                setTextColors(mServiceLabelTextField, R.color.grayText, R.color.white);
                setTextColors(mServiceStatusTextField, R.color.grayText, R.color.white);
                break;
            case SLEEPING:
                setTextColors(mServiceLabelTextField, R.color.colorSubtitleText, R.color.colorTilting);
                setTextColors(mServiceStatusTextField, R.color.colorSubtitleText, R.color.colorTilting);
                break;
            default:
                setTextColors(mServiceLabelTextField, R.color.normalText, R.color.colorRunning);
                setTextColors(mServiceStatusTextField, R.color.normalText, R.color.colorRunning);
            break;
        }

    }

    private void updateUploadState (Intent i) {
        TSUploadState newState = TSUploadState.values()[i.getIntExtra(LABEL_STATE_INDEX,0)];
        mUploadStatusTextField.setText(TSUploadState.getUploadStateString(newState));
        switch (newState) {
            case SWITCHEDOFF:
                setTextColors(mServiceLabelTextField, R.color.grayText, R.color.white);
                setTextColors(mServiceStatusTextField, R.color.grayText, R.color.white);
                break;
            case SIGNEDOUT:
                setTextColors(mServiceLabelTextField, R.color.normalText, R.color.colorBus);
                setTextColors(mServiceStatusTextField, R.color.normalText, R.color.colorBus);
                break;
            case NOCLIENTNUMBER:
                setTextColors(mServiceLabelTextField, R.color.colorSubtitleText, R.color.colorInVehicle);
                setTextColors(mServiceStatusTextField, R.color.colorSubtitleText, R.color.colorInVehicle);
                break;
            case INPROGRESS:
                setTextColors(mServiceLabelTextField, R.color.normalText, R.color.colorRunning);
                setTextColors(mServiceStatusTextField, R.color.normalText, R.color.colorRunning);
                break;
            default:
                setTextColors(mServiceLabelTextField, R.color.colorSubtitleText, R.color.colorWalking);
                setTextColors(mServiceStatusTextField, R.color.colorSubtitleText, R.color.colorWalking);
                break;
        }

    }


    private void setTextColors(TextView view, int text, int background) {
        view.setTextColor(mRes.getColor(text));
        view.setBackgroundResource(background);
    }

    private void updateLocation (Intent i) {
        if (i.hasExtra(InternalBroadcasts.KEY_LOCATION_UPDATE)) {
            Location l = i.getParcelableExtra(InternalBroadcasts.KEY_LOCATION_UPDATE);
            mLocationProviderTextField.setText(l.getProvider());

            Date locationDate = new Date(l.getTime());
            String locTime = DateFormat.getTimeInstance().format(locationDate);
            if (locationIntervalTimer > 0) {
                locTime = locTime.concat(" "+mRes.getString(R.string.interval)+" "+((System.currentTimeMillis()-locationIntervalTimer)/1000)+mRes.getString(R.string.seconds));
            }
            locationIntervalTimer = System.currentTimeMillis();
            mLocationTimeTextField.setText(locTime);

            mLocationAccuracyTextField.setText(String.format("%.0fm", l.getAccuracy()));
        }
    }

    private void updateActivity (Intent i) {
        if (i.hasExtra(InternalBroadcasts.KEY_ACTIVITY_UPDATE)) {
            ActivityData a = i.getParcelableExtra(InternalBroadcasts.KEY_ACTIVITY_UPDATE);
            mTopActivityTextField.setText(a.getFirstString());
            ActivityType topActivity=a.getFirst().Type;
            mLatestActivitiesTextField.setText(a.toString());

            String actTime = a.timeString();
            if (activityIntervalTimer > 0) {
                actTime = actTime.concat(" "+mRes.getString(R.string.interval)+" "+((System.currentTimeMillis()-activityIntervalTimer)/1000)+mRes.getString(R.string.seconds));
            }
            activityIntervalTimer = System.currentTimeMillis();
            mActivityTimeTextField.setText(actTime);

            switch (topActivity) {
                case IN_VEHICLE:
                    setTextColors(mActivityLabelTextField, R.color.colorSubtitleText, R.color.colorInVehicle);
                    setTextColors(mTopActivityTextField, R.color.colorSubtitleText, R.color.colorInVehicle);
                    break;
                case ON_BICYCLE:
                    setTextColors(mActivityLabelTextField, R.color.colorSubtitleText, R.color.colorOnBicycle);
                    setTextColors(mTopActivityTextField, R.color.colorSubtitleText, R.color.colorOnBicycle);
                    break;
                case RUNNING:
                    setTextColors(mActivityLabelTextField, R.color.normalText, R.color.colorRunning);
                    setTextColors(mTopActivityTextField, R.color.normalText, R.color.colorRunning);
                    break;
                case STILL:
                    setTextColors(mActivityLabelTextField, R.color.normalText, R.color.colorStill);
                    setTextColors(mTopActivityTextField, R.color.normalText, R.color.colorStill);
                    break;
                case TILTING:
                    setTextColors(mActivityLabelTextField, R.color.colorSubtitleText, R.color.colorTilting);
                    setTextColors(mTopActivityTextField, R.color.colorSubtitleText, R.color.colorTilting);
                    break;
                case UNKNOWN:
                    setTextColors(mActivityLabelTextField, R.color.colorSubtitleText, R.color.colorUnknown);
                    setTextColors(mTopActivityTextField, R.color.colorSubtitleText, R.color.colorUnknown);
                    break;
                case WALKING:
                    setTextColors(mActivityLabelTextField, R.color.colorSubtitleText, R.color.colorWalking);
                    setTextColors(mTopActivityTextField, R.color.colorSubtitleText, R.color.colorWalking);
                    break;
                default:
                    setTextColors(mActivityLabelTextField, R.color.colorSubtitleText, R.color.colorUnknown);
                    setTextColors(mTopActivityTextField, R.color.colorSubtitleText, R.color.colorUnknown);
                    break;
            }

        }
    }

    private void updateQueueLength (Intent i) {
        int queueLength = i.getIntExtra(InternalBroadcasts.LABEL_QUEUE_LENGTH,0);
        int activeThreshold = i.getIntExtra(InternalBroadcasts.LABEL_QUEUE_THRESHOLD,0);
        mUploadQueueLengthTextField.setText(queueLength + " / " + activeThreshold);
    }

    private void updateLatestUpload (Intent i) {
        long latestUploadMillis = i.getIntExtra(InternalBroadcasts.KEY_UPLOAD_TIME,0);
        if (latestUploadMillis == 0) {
            mUploadTimeTextField.setText(mRes.getString(R.string.not_available));
        } else {
            Date uploadDate = new Date(latestUploadMillis);
            String uploadFormatted = DateFormat.getDateTimeInstance().format(uploadDate);
            mUploadTimeTextField.setText(uploadFormatted);
        }
    }

}
