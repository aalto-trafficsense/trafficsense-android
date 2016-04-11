package fi.aalto.trafficsense.trafficsense.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.ListFragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.*;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import fi.aalto.trafficsense.trafficsense.R;
import timber.log.Timber;

public class DebugActivity extends AppCompatActivity {
    static final int NUM_TABS = 2;


    DebugPager mAdapter;
    ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

        // ViewPager and its adapters use support library
        // fragments, so use getSupportFragmentManager.
        mAdapter = new DebugPager(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.debug_pager);
        mViewPager.setAdapter(mAdapter);
    }

    @Override
    public void onResume()
    {
        super.onResume();
    }

    @Override
    public void onPause()
    {
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        if (id == R.id.action_exit) {
            Timber.d("Exit pressed.");
            // TODO MJR: Think of something more meaningful, finish doesn't really finish: http://stackoverflow.com/questions/3226495/how-to-exit-from-the-application-and-show-the-home-screen
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
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

    }

//    @Override
//    public CharSequence getPageTitle(int position) {
//        String title = "";
//        switch (position) {
//            case 0:
//                title = getString(R.string.debug_show_page_title);
//            case 1:
//                title = getString(R.string.debug_settings_page_title);
//        }
//        return title;
//    }



}

