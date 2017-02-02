package fi.aalto.trafficsense.trafficsense.ui;

import android.Manifest;
import android.app.ActionBar;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
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

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.support.v7.preference.PreferenceManager.getDefaultSharedPreferences;

public class EnergyCertificateActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener {

    private SVGImageView svgImageView;
    private BackendStorage mStorage;
    private android.support.v7.widget.ShareActionProvider mShareActionProvider;
    private Intent mShareIntent;
    private static Calendar startDate;
    private static Calendar endDate;
    private static Calendar currentDate;

    private Button mStartDateButton;
    private Button mEndDateButton;
    private MenuItem mEnergyShareItem;
    private RadioButton mWeekButton;
    private RadioButton mSelectButton;

    private boolean externalStoragePermission = false;

    private static int dateDialogId = -1;

    private static final int START_DATE_DIALOG = 0;
    private static final int END_DATE_DIALOG = 1;
    private final SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private static final int LANG_EN = 0;
    private static final int LANG_FI = 1;
    private static final int LANG_STADI = 2;
    private static int currentLanguage = LANG_EN;

    private final int MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 1;

    private static final String svgExtractViewBox = "viewBox=\"\\d+,\\d+,(\\d+),(\\d+)\"";
    private static final int svgYExtension = 110;
    private static final String certTitlePre = "<text fill=\"rgb(0,0,0)\" font-family=\"Helvetica\" font-size=\"58\" stroke=\"rgb(0,0,0)\" stroke-width=\"1\" x=\"30\" y=\"%d\">";
    private static final int certTitleYOffset = 30;
    private static final String uriTitlePre = "<text fill=\"rgb(0,0,0)\" font-family=\"Helvetica\" font-size=\"34\" stroke=\"rgb(0,0,0)\" stroke-width=\"1\" x=\"35\" y=\"%d\">";
    private static final int certUriYOffset = 90;
    private static final String titlePost = "</text>";
    private static String shareSvgTitle;

    private static final String[][] enFiDictionary = new String[][]{
            { "Ranking", "Sijoitus" },
            { "Bicycle", "Pyörä" },
            { "Walking", "Kävely" },
            { "Running", "Juoksu" },
            { "Train", "Juna" },
            { "Subway", "Metro" },
            { "Tram", "raitiovaunu" },
            { "Bus", "Linja-auto"},
            { "Ferry", "lautta" },
            { "Car", "Auto" },
            { "Average", "Keskiarvo" }
    };

    private static final String[][] enStadiDictionary = new String[][]{
            { "Ranking", "Sija" },
            { "Bicycle", "Fillari" },
            { "Walking", "Dallaus" },
            { "Running", "Sprintit" },
            { "Train", "Stoge" },
            { "Subway", "Tuubi" },
            { "Tram", "spåra" },
            { "Bus", "Dösä"},
            { "Ferry", "flotta" },
            { "Car", "Fiude" },
            { "Average", "Keskiarvo" }
    };


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
        mWeekButton = (RadioButton) findViewById(R.id.energy_button_week);
        mSelectButton = (RadioButton) findViewById(R.id.energy_button_select);

        mStorage = BackendStorage.create(this);

        // Initialize current date and default start and end to one week
        currentDate = Calendar.getInstance();
        setDateOffsets(-7, -1);
        mWeekButton.setChecked(true);

        // Check the current language setting
        if (Locale.getDefault().getLanguage().equalsIgnoreCase("fi")) {
            currentLanguage = LANG_FI;
        }
        if (getDefaultSharedPreferences(this).getBoolean(getString(R.string.settings_locale_stadi_key), false)) {
            currentLanguage = LANG_STADI;
        }
        shareSvgTitle = certTitlePre + getString(R.string.app_name) + " " + getString(R.string.navigation_energy_certificate) + titlePost
                        + uriTitlePre + getString(R.string.energy_uri) + titlePost;

        // Initialize all the constant parts of mShareIntent
        mShareIntent = new Intent();
        mShareIntent.setAction(Intent.ACTION_SEND);
        mShareIntent.setType("image/jpeg");
        mShareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        checkExternalWritePermission(); // Permission to share?
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchCertificate();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate menu resource file.
        getMenuInflater().inflate(R.menu.activity_energy_toolbar, menu);

        // Locate MenuItem with ShareActionProvider
        mEnergyShareItem = menu.findItem(R.id.energy_toolbar_share);

        // Fetch and store ShareActionProvider
        mShareActionProvider = (android.support.v7.widget.ShareActionProvider) MenuItemCompat.getActionProvider(mEnergyShareItem);

        // Hide until we have the permission to share
        mEnergyShareItem.setVisible(false);

        // Return true to display menu
        return true;
    }

    // Call to update the share intent
    private void setShareIntent(Uri uriToImage) {
        if (mShareActionProvider != null) {
            mShareIntent.putExtra(Intent.EXTRA_STREAM, uriToImage);
            mShareActionProvider.setShareIntent(mShareIntent);
            // Timber.d("Share intent set with url: %s", uriToImage.toString());
        }
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
        setDateOffsets(-7, -1);
        fetchCertificate();
    }

    public void selectMonth(View view) { // Select the last month
        resetStartEnd();
        startDate.add(Calendar.MONTH, -1);
        endDate.add(Calendar.DATE, -1);
        updateButtons();
        fetchCertificate();
    }

    public void selectYear(View view) { // Select the last year
        resetStartEnd();
        startDate.add(Calendar.YEAR, -1);
        endDate.add(Calendar.DATE, -1);
        updateButtons();
        fetchCertificate();
    }

    public void startDateClick(View view) { // Select start date
        mSelectButton.setChecked(true);
        DialogFragment newFragment = new StartDatePicker();
        newFragment.show(getSupportFragmentManager(), "startPicker");
    }

    public void endDateClick(View view) { // Select end date
        mSelectButton.setChecked(true);
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
            // Set maximum to yesterday
            DatePicker dp = dpd.getDatePicker();
            Calendar yesterday = Calendar.getInstance();
            yesterday.add(Calendar.DATE, -1);
            dp.setMaxDate(yesterday.getTimeInMillis());
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
            // Set maximum to yesterday, minimum to start date
            DatePicker dp = dpd.getDatePicker();
            Calendar yesterday = Calendar.getInstance();
            yesterday.add(Calendar.DATE, -1);
            dp.setMaxDate(yesterday.getTimeInMillis());
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
        fetchCertificate();

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        TrafficSenseApplication.refreshStadi();
        super.onConfigurationChanged(newConfig);
    }

    private void fetchCertificate() {
        Optional<String> token = mStorage.readSessionToken();
        if (!token.isPresent()) {
            Toast.makeText(this, R.string.not_signed_in, Toast.LENGTH_SHORT).show();
        } else {
            try {
                URL url = new URL(mStorage.getServerName() + "/svg/" + token.get() + "?firstday=" + mDateFormat.format(startDate.getTime()) + "&lastday=" + mDateFormat.format(endDate.getTime()));
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
            if (info.length() > 1000) { // Current basic length should be >5900
                // Change to localised date format for the certificate range
                info = info.replace(mDateFormat.format(startDate.getTime()), DateFormat.getDateInstance().format(startDate.getTime()));
                info = info.replace(mDateFormat.format(endDate.getTime()), DateFormat.getDateInstance().format(endDate.getTime()));
                if (currentLanguage != LANG_EN) info = localiseCertificate(info);  // Localise labels
                SVG svgImage;
                try {
                    svgImage = SVG.getFromString(info);
                } catch(SVGParseException e) {
                    Toast.makeText(TrafficSenseApplication.getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }
                svgImageView.setSVG(svgImage);

                if (externalStoragePermission) { // External storage permission exists, create certificate for sharing
                    // Extend viewbox and build the title area
                    int xMaxOld = 1000;
                    int yMaxOld = 800;
                    Pattern r = Pattern.compile(svgExtractViewBox);
                    Matcher m = r.matcher(info);
                    if (m.find( )) {
                        try {
                            String viewBoxOld = m.group(0);
                            xMaxOld = Integer.parseInt(m.group(1));
                            yMaxOld = Integer.parseInt(m.group(2));
                            info = info.replace(viewBoxOld, viewBoxOld.replace("," + Integer.toString(yMaxOld) + "\"", "," + Integer.toString(yMaxOld + svgYExtension) + "\""));
                            info = info.replace("</svg>", String.format(shareSvgTitle, yMaxOld+certTitleYOffset, yMaxOld+certUriYOffset) + "</svg>");  // Append titles to end
                        } catch(Exception e) {
                            Timber.e("EnergyCertificate viewbox pattern matching error 2: %s", e.getMessage());
                            return;
                        }
                        // Render with titles
                        try {
                            svgImage = SVG.getFromString(info);
                        } catch(SVGParseException e) {
                            Toast.makeText(TrafficSenseApplication.getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } else {
                        Timber.w("EnergyCertificate viewbox pattern matching error 1: Pattern not found.");
                        return;
                    }
                    Bitmap newBM = Bitmap.createBitmap(xMaxOld, yMaxOld+svgYExtension, Bitmap.Config.ARGB_8888);
                    Canvas bmcanvas = new Canvas(newBM);
                    bmcanvas.drawRGB(255, 255, 255);  // White background
                    svgImage.renderToCanvas(bmcanvas);
                    // Draw the QR-code:
                    Drawable qr = ContextCompat.getDrawable(TrafficSenseApplication.getContext(), R.drawable.energy_uri_qr);
                    qr.setBounds(850, yMaxOld-30, 990, yMaxOld+110);
                    qr.draw(bmcanvas);
                    // Save it as a jpg
                    try {
                        String path = Environment.getExternalStorageDirectory() + File.separator + "TrafficSense";
                        File f = new File(path);
                        boolean dirExists = f.isDirectory();
                        if (!f.isDirectory() && !f.exists()) {
                            dirExists = f.mkdirs();
                        }
                        if (dirExists) {
                            File file = new File(path, getString(R.string.energy_share_filename));
                            FileOutputStream fos = new FileOutputStream(file, false);  // false = don't append, overwrite every time
                            newBM.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                            fos.flush();
                            fos.close();
                            mEnergyShareItem.setVisible(true);
                            setShareIntent(Uri.fromFile(file));
                        }
                    } catch (Exception e) {
                        Timber.e("Energy Certificate Bitmap save failed: %s", e.getMessage());
                    }
                }

            } else {
                Toast.makeText(getApplicationContext(), R.string.energy_load_error, Toast.LENGTH_LONG).show();
            }
        }
    }

    /********************************
     *
     * Runtime permission checking
     *
     ********************************/

    private void checkExternalWritePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to write to external storage is missing

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Permission persistently refused by the user
                Toast.makeText(this, getString(R.string.energy_write_permission_persistently_refused), Toast.LENGTH_LONG).show();
                externalStoragePermission = false;
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE);
            }
        } else {
            externalStoragePermission = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Timber.d("WRITE_EXTERNAL_STORAGE permission granted.");
                    externalStoragePermission = true;
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, getString(R.string.energy_write_permission_persistently_refused), Toast.LENGTH_LONG).show();
                    externalStoragePermission = false;
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    /********************************
     *
     * Certificate localisation
     *
     ********************************/


    private String localiseCertificate(String in) {
        if (currentLanguage == LANG_EN) return in;
        String out = in;
        String[][] dict = enFiDictionary;  // Making sure this is initialized
        if (currentLanguage == LANG_FI) dict = enFiDictionary;
        else if (currentLanguage == LANG_STADI) dict = enStadiDictionary;

        int dictSize = (dict.length);

        for (int i=0; i<dictSize ; i++) {
            out = out.replaceAll(dict[i][0], dict[i][1]);
        }

        return out;
    }

}
