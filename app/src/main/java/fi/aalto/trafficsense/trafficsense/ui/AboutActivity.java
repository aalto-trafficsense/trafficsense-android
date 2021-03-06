package fi.aalto.trafficsense.trafficsense.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import fi.aalto.trafficsense.trafficsense.R;
import fi.aalto.trafficsense.trafficsense.TrafficSenseApplication;
import fi.aalto.trafficsense.trafficsense.util.EnvInfo;

import java.io.IOException;
import java.io.InputStream;

import static android.support.v7.preference.PreferenceManager.getDefaultSharedPreferences;

public class AboutActivity extends AppCompatActivity {

    private int mClickCounter;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TrafficSenseApplication.refreshStadi();
        mContext = this;

        setContentView(R.layout.activity_about);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.about_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final Button okButton = (Button) findViewById(R.id.abt_ok_button);
        final TextView legal = (TextView) findViewById(R.id.abt_legalNotice);

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        legal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mClickCounter++;
//                Timber.d("mClickCounter: %d", mClickCounter);
            }
        });

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // close
                finish();
            }
        });

        okButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mClickCounter == 5) { // Open debug mode
                    SharedPreferences.Editor mPrefEditor = getDefaultSharedPreferences(mContext).edit();
                    mPrefEditor.putBoolean(getResources().getString(R.string.debug_settings_debug_mode_key), true);
                    mPrefEditor.commit();
                    Toast.makeText(mContext, mContext.getResources().getString(R.string.debug_mode_open), Toast.LENGTH_LONG).show();
                }
                return true;
            }
        });

        // Init fields
        initFields();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

    }

    @Override
    protected void onResume() {
        super.onResume();
        mClickCounter = 0;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        TrafficSenseApplication.refreshStadi();
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onDestroy() {
        mContext = null;
        super.onDestroy();
    }

    private void initFields() {
        final TextView clientVersionField = (TextView) findViewById(R.id.abt_clientVersionField);
        clientVersionField.setText(EnvInfo.getClientVersionString());

        final TextView clientNumberField = (TextView) findViewById(R.id.abt_clientNumberField);
        clientNumberField.setText(EnvInfo.getClientNumberString());

        final TextView infoLinkField = (TextView) findViewById(R.id.abt_infoLinkField);
        infoLinkField.setText(R.string.str_aboutInfoLinks);
        infoLinkField.setMovementMethod(LinkMovementMethod.getInstance());

        loadLicenseInfo();
        loadContributions();
    }

    private void loadContributions() {
        TextView contributionsField = (TextView) findViewById(R.id.abt_contributions);
        contributionsField.setText(loadRawText(R.raw.contributions));
    }

    private void loadLicenseInfo() {
        TextView legalNoticesField = (TextView) findViewById(R.id.abt_legalNotice);
        legalNoticesField.setMovementMethod(LinkMovementMethod.getInstance()); // Support hyperlinks
        legalNoticesField.setText(Html.fromHtml(loadRawText(R.raw.legal_notice)));
    }

    private String loadRawText(int resId) {
        Resources res = getResources();

        try {
            InputStream stream = res.openRawResource(resId);
            byte[] b = new byte[stream.available()];
            stream.read(b);
            return new String(b);
        } catch (IOException e) {
            return res.getString(R.string.not_available);
        }
    }
}
