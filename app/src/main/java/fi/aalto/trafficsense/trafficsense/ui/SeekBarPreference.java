package fi.aalto.trafficsense.trafficsense.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.widget.SeekBar;
import android.widget.TextView;
import fi.aalto.trafficsense.trafficsense.R;
import timber.log.Timber;

/**
 * From: https://github.com/afarber/android-newbie/blob/master/MyPrefs/src/de/afarber/myprefs/SeekBarPreference.java
 */

public class SeekBarPreference extends Preference implements SeekBar.OnSeekBarChangeListener {
    private SeekBar mSeekBar;
    private int mCurrentValue;
    private int mMinValue;
    private int mMaxValue;
//    private int mDefaultValue;
    private String mUnit;

    private TextView mCurrentText;

    private static final String SBP_NS =
            "http://schemas.android.com/apk/lib/fi.aalto.trafficsense.trafficsense.ui.SeekBarPreference";
//    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

//    private static final String ATTR_DEFAULT_VALUE = "defaultValue";
    private static final String ATTR_MIN_VALUE = "minValue";
    private static final String ATTR_MAX_VALUE = "maxValue";
    private static final String ATTR_UNIT = "unit";

    public SeekBarPreference(Context context) {
        this(context, null, 0);
    }

    public SeekBarPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SeekBarPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setLayoutResource(R.layout.preference_seekbar);

//        // Print all values in attrs AttributeSet:
//        for (int i=0;i<attrs.getAttributeCount();i++) {
//            String name = attrs.getAttributeName(i);
//            Object value  = attrs.getAttributeValue(i);
//            Timber.d(name + " : " + value + " of type " + value.getClass().getSimpleName());
//        }

        mMinValue = attrs.getAttributeIntValue(SBP_NS, ATTR_MIN_VALUE, 0);
        mMaxValue = attrs.getAttributeIntValue(SBP_NS, ATTR_MAX_VALUE, 100);
        mUnit = attrs.getAttributeValue(SBP_NS, ATTR_UNIT);
        if (mUnit.startsWith("@string/")) { // String reference - dig out the actual value
            mUnit = context.getResources().getString(
                    context.getResources().getIdentifier(mUnit.substring(8), "string", context.getPackageName()));
        }

//        mDefaultValue = attrs.getAttributeIntValue(ANDROID_NS, ATTR_DEFAULT_VALUE, 50);
//        Timber.d("--- Seekbarpreference constructor fetched default: %d", mDefaultValue);

    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mSeekBar = (SeekBar) holder.findViewById(R.id.preference_seekbar);
        mSeekBar.setMax(mMaxValue - mMinValue);
        Timber.d("SeekBar onBindViewHolder");
        mSeekBar.setProgress(mCurrentValue - mMinValue);
        mSeekBar.setOnSeekBarChangeListener(this);

        if (mUnit!=null) {
            ((TextView) holder.findViewById(R.id.preference_seekbar_unit)).setText(mUnit);
        }
        ((TextView) holder.findViewById(R.id.preference_seekbar_min)).setText(Integer.toString(mMinValue));
        ((TextView) holder.findViewById(R.id.preference_seekbar_max)).setText(Integer.toString(mMaxValue));

        mCurrentText = (TextView) holder.findViewById(R.id.preference_seekbar_current);
        mCurrentText.setText(Integer.toString(mCurrentValue));
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (!fromUser)
            return;
        mCurrentValue = progress + mMinValue;
        mCurrentText.setText(Integer.toString(mCurrentValue));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // not used
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        setValue(mCurrentValue);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
//        Timber.d("--- SeekBar onSetInitialValue called with restore: %b default: %d", restorePersistedValue, (Integer) defaultValue);
        if (restorePersistedValue) {
            mCurrentValue = this.getPersistedInt(0);
        } else {
            mCurrentValue = (Integer) defaultValue;
            persistInt(mCurrentValue);
        }
    }

    public void setValue(int value) {
        if (shouldPersist()) {
            persistInt(value);
            Timber.d("SeekBar persistvalue: %d", value);
        }
        notifyChanged();
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
//        Timber.d("--- SeekBar onGetInitialValue called with index: %d", index);
        return a.getInt(index, 0);
    }
}