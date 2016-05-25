package fi.aalto.trafficsense.trafficsense.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;
import fi.aalto.trafficsense.trafficsense.R;
import fi.aalto.trafficsense.trafficsense.util.BroadcastHelper;
import fi.aalto.trafficsense.trafficsense.util.InternalBroadcasts;

import static android.support.v7.preference.PreferenceManager.getDefaultSharedPreferences;

/**
 * Created by mikko.rinne@aalto.fi on 25/05/16.
 */
public class ConsentDialog {

    private Activity mActivity;
    private SharedPreferences mSettings;
    private LocalBroadcastManager mLocalBroadcastManager;

    public ConsentDialog(Activity context) {
        mActivity = context;
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(mActivity);
        mSettings = getDefaultSharedPreferences(mActivity);
    }

    public void show() {

        if (!mSettings.getBoolean(mActivity.getString(R.string.settings_eula_shown_key), false)) { // Not shown yet
            // Show the Eula
            String title = mActivity.getString(R.string.research_consent_title);

            //Includes the updates as well so users know what changed.
            String message = mActivity.getString(R.string.research_consent);

            AlertDialog.Builder builder = new AlertDialog.Builder(mActivity)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(R.string.research_consent_agree_button, new Dialog.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // Mark accepted
                            SharedPreferences.Editor editor = mSettings.edit();
                            editor.putBoolean(mActivity.getString(R.string.settings_eula_shown_key), true);
                            // Start the service
                            BroadcastHelper.simpleBroadcast(mLocalBroadcastManager, InternalBroadcasts.KEY_SERVICE_START);
                            // Mark the service to be on
                            editor.putBoolean(mActivity.getString(R.string.debug_settings_service_running_key), true);
                            editor.apply();
                            dialogInterface.dismiss();
                        }
                    })
                    .setNegativeButton(R.string.research_consent_disagree_button, new Dialog.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Close the main activity (service has not even started)
                            Toast.makeText(mActivity, R.string.research_consent_dismissed, Toast.LENGTH_LONG).show();
                            mActivity.finish();
                        }

                    });
            builder.create().show();


        }

    }
}
