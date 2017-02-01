package fi.aalto.trafficsense.trafficsense.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import com.google.maps.android.geojson.GeoJsonFeature;
import fi.aalto.trafficsense.trafficsense.R;
import fi.aalto.trafficsense.trafficsense.util.ActivityPathConverter;
import fi.aalto.trafficsense.trafficsense.util.InternalBroadcasts;
import fi.aalto.trafficsense.trafficsense.util.SharedPrefs;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import timber.log.Timber;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Created by mikko.rinne@aalto.fi on 1.2.2017.
 */
public class PathEditDialog extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private Activity mActivity;
    private ActivityPathConverter mActivityPathConverter;
    private GeoJsonFeature currentActivity;
    private String mSessionToken = "";
    private String mServerName = "";

    private String defaultActString = "";
    private String defaultLineName = "";
    private int defaultActivityIndex = -1;

    private String currentActString = "";
    private int currentActivityIndex = -1;
    private String currentLineName = "";

    private EditText mLineNameEdit;
    private LinearLayout mLineNameLayout;

    public PathEditDialog(Activity context, String st, String sn, ActivityPathConverter apc, GeoJsonFeature ca) {
        mActivity = context;
        mSessionToken = st;
        mServerName = sn;
        mActivityPathConverter = apc;
        currentActivity = ca;
    }

    public void show() {

        String title = mActivity.getString(R.string.path_edit_dialog_title);

        LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.dialog_path_edit, null);

        if (currentActivity.hasProperty("activity")) {
            defaultActString = currentActivity.getProperty("activity");
            currentActString = defaultActString;
            defaultActivityIndex = mActivityPathConverter.getIndex(defaultActString);
            currentActivityIndex = defaultActivityIndex;
        }
        if (currentActivity.hasProperty("line_name")) {
            defaultLineName = currentActivity.getProperty("line_name");
            currentLineName = defaultLineName;
        }

        Spinner activityListView = (Spinner) layout.findViewById(R.id.path_edit_activity_list);
        activityListView.setOnItemSelectedListener(this);
        List<String> activities = mActivityPathConverter.getEditList(mActivity);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                mActivity,
                android.R.layout.simple_spinner_item,
                activities
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        activityListView.setAdapter(adapter);
        activityListView.setSelection(defaultActivityIndex);

        mLineNameEdit = (EditText) layout.findViewById(R.id.path_edit_line_name_input);
        mLineNameLayout = (LinearLayout) layout.findViewById(R.id.path_edit_line_name_layout);
        updateLineName();

        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity)
                .setTitle(title)
                .setView(layout)
                .setPositiveButton(R.string.path_edit_confirm_choice, new Dialog.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Timber.d("Confirmed");
                        // Find dialog answers
                        currentLineName = mLineNameEdit.getText().toString();
                        Timber.d("Server activity: %s Line name: %s", currentActString, currentLineName);
                        // Send to server
                        if (!(defaultActivityIndex==currentActivityIndex && defaultLineName.equals(currentLineName))) {
                            sendLegEdit();
                        }
                        // Reload path data
                        dialogInterface.dismiss();
                    }
                })
                .setNegativeButton(R.string.path_edit_cancel_choice, new Dialog.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Timber.d("Edits cancelled, do nothing.");
                    }

                });
        builder.create().show();
    }


    /*
        Spinner callbacks
     */

    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        Timber.d("Selected item at position: %d", pos);
        if (pos!=currentActivityIndex) { // Activity changed
            currentActivityIndex = pos;
            currentActString = mActivityPathConverter.getServerNameFromIndex(currentActivityIndex);
            if (currentActivityIndex == defaultActivityIndex) { // Restore default
                currentLineName = defaultLineName;
            } else { // No default - set blank
                currentLineName = "";
            }
            updateLineName();
        }
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
        Timber.d("Nothing selected");
    }

    /*
        Private helpers
     */

    private void updateLineName() {
        mLineNameEdit.setText(currentLineName);
        if (mActivityPathConverter.hasLineName(currentActString)) {
            mLineNameLayout.setVisibility(View.VISIBLE);  // Others INVISIBLE, GONE
        } else {
            mLineNameLayout.setVisibility(View.INVISIBLE);
        }
    }

    /*
        Send update to server
     */

    private void sendLegEdit() {
        try {
            URL url = new URL(mServerName + "/setlegmode");
            LegEditSendTask downloader = new LegEditSendTask();
            downloader.execute(url);
        } catch (MalformedURLException e) {
            Toast.makeText(mActivity, R.string.path_edit_url_broken, Toast.LENGTH_SHORT).show();
        }
    }

    private class LegEditSendTask extends AsyncTask<URL, Void, Integer> {
        protected Integer doInBackground(URL... urls) {
            Integer responseCode = 0;
            if (urls.length != 1) {
                Timber.e("Leg edit attempted to get more or less than one URL");
                return null;
            }

            HttpURLConnection urlConnection = null;
            try {
                Timber.d("Opening with URL: " + urls[0].toString());
                urlConnection = (HttpURLConnection) urls[0].openConnection();
                urlConnection.setDoOutput(true);
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Content-Type", "application/json");

                JSONObject legUpdate = new JSONObject();
                legUpdate.put("sessionToken", mSessionToken);
                legUpdate.put("id", Integer.parseInt(currentActivity.getProperty("id")));
                legUpdate.put("activity", currentActString);
                if (mActivityPathConverter.hasLineName(currentActString)) {
                    legUpdate.put("line_name", currentLineName);
                }

                DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
                wr.writeBytes(legUpdate.toString());
                wr.flush();
                wr.close();

                responseCode = urlConnection.getResponseCode();
            } catch (IOException e) {
                Timber.e("LegEditSendTask error connecting to URL: %s", e.getMessage());
            } catch (JSONException e) {
                Timber.e("LegEditSendTask JSON exception: %s", e.getMessage());
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

            return responseCode; // responseString;
        }

        protected void onPostExecute(Integer responseCode) {
//            Timber.d("DownloadDestTask Received a string of length: " + info.length());
//            Timber.d("First 200 characters: \n" + info.substring(0,min(200,info.length())));
            Timber.d("LegEditSend response: %d", responseCode);
            if (responseCode == 200) {
                // Success - refresh the paths on the screen.
                LocalBroadcastManager mLB = LocalBroadcastManager.getInstance(mActivity);
                mLB.sendBroadcast(new Intent(InternalBroadcasts.KEY_REQUEST_PATH_UPDATE));
            } else {
                Toast.makeText(mActivity, R.string.path_edit_update_failed, Toast.LENGTH_SHORT).show();
            }
        }
    }


}
