package fi.aalto.trafficsense.trafficsense.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;
import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGImageView;
import com.caverock.androidsvg.SVGParseException;
import com.google.common.base.Optional;
import fi.aalto.trafficsense.trafficsense.R;
import fi.aalto.trafficsense.trafficsense.TrafficSenseApplication;
import fi.aalto.trafficsense.trafficsense.util.BackendStorage;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class EnergyCertificateActivity extends AppCompatActivity {

    private SVGImageView svgImageView;
    private BackendStorage mStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_energy_certificate);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.energy_certificate_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        FrameLayout container = (FrameLayout) findViewById(R.id.energy_certificate);
        svgImageView = new SVGImageView(this);
        container.addView(svgImageView, ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT);

        mStorage = BackendStorage.create(this);

    }

    @Override
    protected void onStart() {
        super.onStart();
        // fetchCertificate();
    }

    @Override
    protected void onResume() {
        super.onResume();
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
                URL url = new URL(mStorage.getServerName().toString() + "/svg/" + token.get());
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
            SVG svgImage;
            try {
                svgImage = SVG.getFromString(info);
            } catch(SVGParseException e) {
                Context context = getApplicationContext();
                Toast toast = Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT);
                toast.show();
                return;
            }
            svgImageView.setSVG(svgImage);
        }
    }
}
