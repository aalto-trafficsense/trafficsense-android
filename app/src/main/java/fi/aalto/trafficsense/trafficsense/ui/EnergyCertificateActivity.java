package fi.aalto.trafficsense.trafficsense.ui;

import android.app.ActionBar;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.*;
import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGImageView;
import com.caverock.androidsvg.SVGParseException;
import com.google.common.base.Optional;
import fi.aalto.trafficsense.trafficsense.R;
import fi.aalto.trafficsense.trafficsense.TrafficSenseApplication;
import fi.aalto.trafficsense.trafficsense.util.BackendStorage;
import timber.log.Timber;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class EnergyCertificateActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener {

    private SVGImageView svgImageView;
    private BackendStorage mStorage;
    private static Calendar startDate;
    private static Calendar endDate;
    private static Calendar currentDate;

    private Button mStartDateButton;
    private Button mEndDateButton;

    private static int dateDialogId = -1;

    static final int START_DATE_DIALOG = 0;
    static final int END_DATE_DIALOG = 1;
    private final SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TrafficSenseApplication.refreshStadi();
        setContentView(R.layout.activity_energy_certificate);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.energy_certificate_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        FrameLayout container = (FrameLayout) findViewById(R.id.energy_certificate);
        svgImageView = new SVGImageView(this);
        container.addView(svgImageView, ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT);

        mStartDateButton = (Button) findViewById(R.id.energy_button_start);
        mEndDateButton = (Button) findViewById(R.id.energy_button_end);

        mStorage = BackendStorage.create(this);

        // Initialize current date and default start and end to one week
        currentDate = Calendar.getInstance();
        setDateOffsets(-7, -1);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // fetchCertificate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchCertificate(startDate, endDate);
    }

    private void setDateOffsets(int startOffset, int endOffset) {
        resetStartEnd();
        startDate.add(Calendar.DATE, startOffset);
        endDate.add(Calendar.DATE, endOffset);
        updateButtons();
    }

    private void updateButtons() {
        mStartDateButton.setText(DateFormat.getDateInstance().format(startDate.getTime()));
        mEndDateButton.setText(DateFormat.getDateInstance().format(endDate.getTime()));
    }

    private void resetStartEnd() {
        startDate = (Calendar)currentDate.clone();
        endDate = (Calendar)currentDate.clone();
    }

    public void selectWeek(View view) { // Select the last week
        Timber.d("Week selected");
        setDateOffsets(-7, -1);
        fetchCertificate(startDate, endDate);
    }

    public void selectMonth(View view) { // Select the last month
        Timber.d("Month selected");
        resetStartEnd();
        startDate.add(Calendar.MONTH, -1);
        endDate.add(Calendar.DATE, -1);
        updateButtons();
        fetchCertificate(startDate, endDate);
    }

    public void selectYear(View view) { // Select the last year
        Timber.d("Year selected");
        resetStartEnd();
        startDate.add(Calendar.YEAR, -1);
        endDate.add(Calendar.DATE, -1);
        updateButtons();
        fetchCertificate(startDate, endDate);
    }

    public void startDateClick(View view) { // Select start date
        Timber.d("Start date selection selected");
        DialogFragment newFragment = new StartDatePicker();
        newFragment.show(getSupportFragmentManager(), "startPicker");
    }

    public void endDateClick(View view) { // Select end date
        Timber.d("End date selection selected");
        DialogFragment newFragment = new EndDatePicker();
        newFragment.show(getSupportFragmentManager(), "endPicker");
    }

    public static class StartDatePicker extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            dateDialogId = START_DATE_DIALOG;
            // Use startDate as the default date in the picker
            int year = startDate.get(Calendar.YEAR);
            int month = startDate.get(Calendar.MONTH);
            int day = startDate.get(Calendar.DAY_OF_MONTH);

            // Create a new instance of DatePickerDialog
            DatePickerDialog dpd = new DatePickerDialog(getActivity(), (DatePickerDialog.OnDateSetListener) getActivity(), year, month, day);
            // Set maximum to current date
            DatePicker dp = dpd.getDatePicker();
            dp.setMaxDate(currentDate.getTimeInMillis());
            return dpd;
        }
    }

    public static class EndDatePicker extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            dateDialogId = END_DATE_DIALOG;
            // Use endDate as the default date in the picker
            int year = endDate.get(Calendar.YEAR);
            int month = endDate.get(Calendar.MONTH);
            int day = endDate.get(Calendar.DAY_OF_MONTH);

            // Create a new instance of DatePickerDialog
            DatePickerDialog dpd = new DatePickerDialog(getActivity(), (DatePickerDialog.OnDateSetListener) getActivity(), year, month, day);
            // Set maximum to current date, minimum to start date
            DatePicker dp = dpd.getDatePicker();
            dp.setMaxDate(currentDate.getTimeInMillis());
            dp.setMinDate(startDate.getTimeInMillis());
            return dpd;
        }
    }

    @Override
    public void onDateSet (DatePicker view, int year, int month, int day) {
        switch (dateDialogId) {
            case START_DATE_DIALOG:
                startDate.set(Calendar.YEAR, year);
                startDate.set(Calendar.MONTH, month);
                startDate.set(Calendar.DAY_OF_MONTH, day);
                if (startDate.after(endDate)) {
                    // Move endDate default equal to startDate
                    endDate = (Calendar)startDate.clone();
                    mEndDateButton.setText(DateFormat.getDateInstance().format(endDate.getTime()));
                }
                mStartDateButton.setText(DateFormat.getDateInstance().format(startDate.getTime()));
                break;
            case END_DATE_DIALOG:
                endDate.set(Calendar.YEAR, year);
                endDate.set(Calendar.MONTH, month);
                endDate.set(Calendar.DAY_OF_MONTH, day);
                if (startDate.after(endDate)) {
                    // Should not happen, but fix here in case
                    endDate = (Calendar)startDate.clone();
                }
                mEndDateButton.setText(DateFormat.getDateInstance().format(endDate.getTime()));
                break;
            default:
                // Huh??
        }
        fetchCertificate(startDate, endDate);

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        TrafficSenseApplication.refreshStadi();
        super.onConfigurationChanged(newConfig);
    }

    private void fetchCertificate(Calendar start, Calendar end) {
        Optional<String> token = mStorage.readSessionToken();
        if (!token.isPresent()) {
            Toast.makeText(this, R.string.not_signed_in, Toast.LENGTH_SHORT).show();
        } else {
            try {
                URL url = new URL(mStorage.getServerName() + "/svg/" + token.get() + "?firstday=" + mDateFormat.format(start.getTime()) + "&lastday=" + mDateFormat.format(end.getTime()));
                DownloadDataTask downloader = new DownloadDataTask();
                downloader.execute(url);
            } catch (MalformedURLException e) {
                Toast.makeText(this, R.string.certificate_url_broken, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class DownloadDataTask extends AsyncTask<URL, Void, String> {
        protected String doInBackground(URL... urls) {
            String returnVal = null;
            if (urls.length != 1) {
                return "Certificate downloader attempted to get more or less than one URL";
            }


            HttpURLConnection urlConnection = null;
            try {
                urlConnection = (HttpURLConnection) urls[0].openConnection();
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                java.util.Scanner s = new java.util.Scanner(in).useDelimiter("\\A");
                if (s.hasNext()) {
                    returnVal = s.next();
                }
                else {
                    returnVal = "No data!";
                }
                in.close();

            }
            catch (IOException e) {
                return "error connecting to URL";
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

            return returnVal;
        }

        protected void onPostExecute(String info) {
            if (info.length() > 20) {
                SVG svgImage;
                try {
                    svgImage = SVG.getFromString(info);
                } catch(SVGParseException e) {
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }
                svgImageView.setSVG(svgImage);
            } else {
                Toast.makeText(getApplicationContext(), R.string.energy_load_error, Toast.LENGTH_LONG).show();
            }
        }
    }
}
