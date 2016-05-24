package fi.aalto.trafficsense.trafficsense.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.preference.*;
import android.widget.Toast;
import fi.aalto.trafficsense.trafficsense.R;
import fi.aalto.trafficsense.trafficsense.backend.uploader.RegularRoutesPipeline;
import fi.aalto.trafficsense.trafficsense.util.InternalBroadcasts;
import timber.log.Timber;

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
            RegularRoutesPipeline.setUploadEnabledState(mSettings.getBoolean(key, true));
            // Update status for this view
            mLocalBroadcastManager.sendBroadcast(new Intent(InternalBroadcasts.KEY_DEBUG_SHOW_REQ));
        }

        // Upload threshold change
        if (key.equals(getString(R.string.debug_settings_upload_threshold_key))) {
            Toast.makeText(getActivity(), R.string.upload_threshold_change, Toast.LENGTH_LONG).show();
        }

//        Preference preference = findPreference(key);
//        if (preference instanceof CheckBoxPreference) {
//            CheckBoxPreference cb = (CheckBoxPreference) preference;
//            if (cb.isChecked()) Timber.i("Checkbox true");
//            else Timber.i("Checkbox false");
//        }
//        if (preference instanceof ListPreference) {
//            ListPreference listPreference = (ListPreference) preference;
//            int prefIndex = listPreference.findIndexOfValue(sharedPreferences.getString(key, ""));
//            if (prefIndex >= 0) {
//                preference.setSummary(listPreference.getEntries()[prefIndex]);
//            }
//        } else {
//            preference.setSummary(sharedPreferences.getString(key, ""));
//
//        }
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
