package fi.aalto.trafficsense.trafficsense.ui;

import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import fi.aalto.trafficsense.trafficsense.R;
import timber.log.Timber;

import java.io.IOException;
import java.io.InputStream;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_about);
        // setupActionBar(); // Not in application theme, throws an exception
        Toolbar myToolbar = (Toolbar) findViewById(R.id.about_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final View controlsView = findViewById(R.id.abt_Buttons);
        final View titleView = findViewById(R.id.abt_pageTitleField);
        final Button okButton = (Button) findViewById(R.id.abt_ok_button);

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        okButton.setOnTouchListener(mTouchListener);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // close
                finish();
            }
        });
        // Init fields
        initFields();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupActionBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Show the Up button in the action bar.
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            // TODO: If Settings has multiple levels, Up should navigate up
            // that hierarchy.
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            Timber.d("About onTouchListener touched.");
            return false;
        }
    };

    private void initFields() {
        final TextView clientVersionField = (TextView) findViewById(R.id.abt_clientVersionField);

        try {
            final String ver = getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
            clientVersionField.setText(ver);
        } catch (PackageManager.NameNotFoundException e) {
            clientVersionField.setText("<unknown>");
        }

        loadLicenseInfo();
        loadContributions();
    }

    private void loadContributions() {
        TextView contributionsField = (TextView) findViewById(R.id.abt_contributions);
        loadAndFillField(R.raw.contributions, contributionsField);
    }

    private void loadLicenseInfo() {
        TextView legalNoticesField = (TextView) findViewById(R.id.abt_legalNotice);
        loadAndFillField(R.raw.legal_notice, legalNoticesField);
    }

    private void loadAndFillField(int resId, TextView field) {
        Resources res = getResources();

        if (res == null || field == null)
            return;

        try {
            InputStream stream = res.openRawResource(resId);
            byte[] b = new byte[stream.available()];
            stream.read(b);
            field.setText(new String(b));
        } catch (IOException e) {
            field.setText("<load failed>");
        }
    }
}
