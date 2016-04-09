package fi.aalto.trafficsense.trafficsense.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import fi.aalto.trafficsense.trafficsense.R;
import fi.aalto.trafficsense.trafficsense.util.InternalBroadcasts;
import fi.aalto.trafficsense.trafficsense.util.TSServiceState;
import timber.log.Timber;

import static fi.aalto.trafficsense.trafficsense.util.InternalBroadcasts.LABEL_SERVICE_STATE_INDEX;
import static fi.aalto.trafficsense.trafficsense.util.TSServiceState.RUNNING;
import static fi.aalto.trafficsense.trafficsense.util.TSServiceState.SLEEPING;

public class DebugSettings extends AppCompatActivity {

    private final String TAG = this.getClass().getName();

    private BroadcastReceiver mBroadcastReceiver;

    /* UI Components */
    private Switch mToggleServiceSwitch;
    private TextView mServiceStatusTextField;


    /* Display values */
    private TSServiceState DS_ServiceState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug_settings);

        initFields();
        initButtonHandlers();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        initBroadcastReceiver();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
    }


    private void initFields() {
        Timber.d("initFields called");
        mToggleServiceSwitch = (Switch) findViewById(R.id.debug_setting_ToggleServiceSwitch);
        mServiceStatusTextField = (TextView) findViewById(R.id.debug_setting_ServiceStateIndicator);
    }

/*
    private void updateServiceSwitchState() {
        // Init upload toggle state
        final boolean enabledState = getUploadEnabledState();
        if (mToggleServiceSwitch != null) {

            if (enabledState != mToggleServiceSwitch.isChecked())
                mToggleServiceSwitch.setChecked(getUploadEnabledState());
        }

    }
*/

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
            simpleBroadcast(InternalBroadcasts.KEY_SERVICE_START);
        } else {
            Timber.d("Service start switch off.");
            simpleBroadcast(InternalBroadcasts.KEY_SERVICE_STOP);
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        if (id == R.id.action_exit) {
            Timber.d("Exit pressed.");
            // TODO MJR: Think of something more meaningful, finish doesn't really finish: http://stackoverflow.com/questions/3226495/how-to-exit-from-the-application-and-show-the-home-screen
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Broadcaster
    private void simpleBroadcast(String messageType) {
        LocalBroadcastManager mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
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
                    case InternalBroadcasts.KEY_SERVICE_STATE_UPDATE:
                        DS_ServiceState = TSServiceState.values()[intent.getIntExtra(LABEL_SERVICE_STATE_INDEX,0)];
                        mServiceStatusTextField.setText(TSServiceState.getServiceStateString(DS_ServiceState));
                        break;
                }
            }
        };

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(InternalBroadcasts.KEY_SERVICE_STATE_UPDATE);

        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, intentFilter);
    }


}
