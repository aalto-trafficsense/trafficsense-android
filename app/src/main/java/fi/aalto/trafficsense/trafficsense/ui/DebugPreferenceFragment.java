package fi.aalto.trafficsense.trafficsense.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.preference.*;
import fi.aalto.trafficsense.trafficsense.R;
import fi.aalto.trafficsense.trafficsense.backend.TrafficSenseService;
import fi.aalto.trafficsense.trafficsense.util.InternalBroadcasts;
import timber.log.Timber;

/**
 * Created by mikko.rinne@aalto.fi on 17/05/16.
 */
public class DebugPreferenceFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    private SharedPreferences sharedPreferences;
    private Resources mRes;
    private LocalBroadcastManager mLocalBroadcastManager;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        //add xml
        addPreferencesFromResource(R.xml.debug_settings);

        mRes = this.getResources();
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this.getContext());

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        // onSharedPreferenceChanged(sharedPreferences, getString(R.string.preftest_categories_key));
    }


    @Override
    public void onResume() {
        super.onResume();
        //unregister the preferenceChange listener
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(mRes.getString(R.string.debug_settings_activity_interval_key))) {
            Timber.d("Activity interval changed!!");
            mLocalBroadcastManager.sendBroadcast(new Intent(InternalBroadcasts.KEY_SETTINGS_ACTIVITY_INTERVAL));
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
