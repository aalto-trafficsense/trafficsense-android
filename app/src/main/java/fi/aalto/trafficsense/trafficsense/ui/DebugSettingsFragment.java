package fi.aalto.trafficsense.trafficsense.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import fi.aalto.trafficsense.trafficsense.R;
import fi.aalto.trafficsense.trafficsense.backend.uploader.RegularRoutesPipeline;
import fi.aalto.trafficsense.trafficsense.util.InternalBroadcasts;

/**
 * Created by mikko.rinne@aalto.fi on 17/05/16.
 */
public class DebugSettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    private SharedPreferences mSettings;
    private LocalBroadcastManager mLocalBroadcastManager;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        //add xml
        addPreferencesFromResource(R.xml.debug_settings);
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this.getContext());
        mSettings = PreferenceManager.getDefaultSharedPreferences(getActivity());

        setRestoreButton();
    }

    private void setRestoreButton() {
        Preference restoreButton = findPreference(getString(R.string.debug_settings_restore_defaults_key));
        restoreButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                SharedPreferences.Editor editor = mSettings.edit();
                editor.clear();
                editor.commit();
                setPreferenceScreen(null);
                addPreferencesFromResource(R.xml.debug_settings);
                setRestoreButton(); // re-initialize the button
                return true;
            }
        });
    }


    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        // Service ON/OFF toggle
        if (key.equals(getString(R.string.debug_settings_service_running_key))) {
            if (mSettings.getBoolean(key, true)) {
                mLocalBroadcastManager.sendBroadcast(new Intent(InternalBroadcasts.KEY_SERVICE_START));
            } else {
                mLocalBroadcastManager.sendBroadcast(new Intent(InternalBroadcasts.KEY_SERVICE_STOP));
            }
        }

        // Location interval change
        if (key.equals(getString(R.string.debug_settings_location_interval_key))) {
            mLocalBroadcastManager.sendBroadcast(new Intent(InternalBroadcasts.KEY_SETTINGS_LOCATION_INTERVAL));
        }

        // Activity interval change
        if (key.equals(getString(R.string.debug_settings_activity_interval_key))) {
//            Timber.d("Activity interval changed!!");
            mLocalBroadcastManager.sendBroadcast(new Intent(InternalBroadcasts.KEY_SETTINGS_ACTIVITY_INTERVAL));
        }

        // Upload enabled change
        if (key.equals(getString(R.string.debug_settings_upload_enabled_key))) {
            Boolean enabled = mSettings.getBoolean(key, true);
            RegularRoutesPipeline.setUploadEnabledState(enabled);

            Button mUploadButton = (Button) getActivity().findViewById(R.id.debug_show_upload_button);

            // Don't show the button on the dash
            if (enabled) mUploadButton.setVisibility(View.VISIBLE);
            else mUploadButton.setVisibility(View.INVISIBLE);

            // Request view update
            mLocalBroadcastManager.sendBroadcast(new Intent(InternalBroadcasts.KEY_DEBUG_SHOW_REQ));
        }

        // Upload threshold change
        if (key.equals(getString(R.string.debug_settings_upload_threshold_key))) {
            Toast.makeText(getActivity(), R.string.upload_threshold_change, Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        //unregister the preference change listener
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
