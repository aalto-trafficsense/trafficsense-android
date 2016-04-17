package fi.aalto.trafficsense.trafficsense.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.*;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.*;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import fi.aalto.trafficsense.trafficsense.R;
import fi.aalto.trafficsense.trafficsense.util.InternalBroadcasts;
import fi.aalto.trafficsense.trafficsense.util.TSServiceState;
import timber.log.Timber;

import static fi.aalto.trafficsense.trafficsense.util.InternalBroadcasts.LABEL_SERVICE_STATE_INDEX;

public class DebugActivity extends AppCompatActivity {
    static final int NUM_TABS = 2;

    DebugPager mAdapter;
    ViewPager mViewPager;

    private LocalBroadcastManager mLocalBroadcastManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

        // ViewPager and its adapters use support library
        // fragments, so use getSupportFragmentManager.
        mAdapter = new DebugPager(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.debug_pager);
        mViewPager.setAdapter(mAdapter);

        // Give the TabLayout the ViewPager
        TabLayout tabLayout = (TabLayout) findViewById(R.id.debug_tabs);
        tabLayout.setupWithViewPager(mViewPager);

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        broadcastViewResumed(true);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        broadcastViewResumed(false);
    }


    public class DebugPager extends FragmentPagerAdapter {
        public DebugPager(FragmentManager fm) {
            super(fm);
        }


        @Override
        public int getCount() {
            return NUM_TABS;
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment = null;
            switch (position) {
                case 0:
                    fragment = new DebugShowFragment();
                    break;
                case 1:
                    fragment = new DebugSettingsFragment();
                    break;
            }
            if (fragment == null) {
                Timber.e("Debugpager getitem failed to initialize a fragment.");
                return null;
            } else {
                return fragment;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            CharSequence title;
            switch (position) {
                case 0:
                    title = getString(R.string.debug_show_page_title);
                    break;
                case 1:
                    title = getString(R.string.debug_settings_page_title);
                    break;
                default:
                    title = "Error";
            }
            return title;
        }

    }

    // Update (viewing) activity status to service
    public void broadcastViewResumed(boolean resumed) {
        if (mLocalBroadcastManager != null)
        {
            String key;
            if (resumed) key = InternalBroadcasts.KEY_VIEW_RESUMED;
            else key = InternalBroadcasts.KEY_VIEW_PAUSED;
            Intent intent = new Intent(key);
            mLocalBroadcastManager.sendBroadcast(intent);
        }
    }

}

