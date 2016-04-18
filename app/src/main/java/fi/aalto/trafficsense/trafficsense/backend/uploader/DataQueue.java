package fi.aalto.trafficsense.trafficsense.backend.uploader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import com.google.common.collect.EvictingQueue;
import com.google.common.collect.ImmutableList;
import fi.aalto.trafficsense.trafficsense.backend.TrafficSenseService;
import fi.aalto.trafficsense.trafficsense.util.DataPacket;
import fi.aalto.trafficsense.trafficsense.util.InternalBroadcasts;
import timber.log.Timber;

import java.util.Iterator;
import java.util.Queue;

public class DataQueue {
    private final Queue<DataPoint> mDeque;
    private final int flushThreshold;
    private final int mMaxSize;
    private int activeThreshold;

    private LocalBroadcastManager mLocalBroadcastManager;
    private BroadcastReceiver mBroadcastReceiver;

    private long mNextSequence;

    public DataQueue(int maxSize, int flushThreshold) {
        mMaxSize = maxSize;
        this.mDeque = EvictingQueue.create(mMaxSize);
        this.flushThreshold = flushThreshold;
        initThreshold();
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(TrafficSenseService.getContext());
        initBroadcastReceiver();
        Timber.d("DataQueue: constructor called with maxSize: "+maxSize+" flushThreshold: "+flushThreshold);
    }

    public void onDataReady(DataPacket data) {

        DataPoint dataPoint = new DataPoint(System.currentTimeMillis(), mNextSequence++, data.getLocationData(), data.getActivityData());
        this.mDeque.add(dataPoint);
        broadcastQueueStatus();
    }

    public void removeUntilSequence(long sequence) {
        Iterator<DataPoint> iter = this.mDeque.iterator();
        while (iter.hasNext()) {
            DataPoint dataPoint = iter.next();
            if (dataPoint.mSequence > sequence) {
                break;
            }
            iter.remove();
        }
        broadcastQueueStatus();
    }

    // MJR: Auxiliary procedure to help cope with mysterious "500 INTERNAL SERVER ERROR"s during upload.
    // TODO: Remove method, when root cause for errors has been cleared
    // In TrafficSense Client threw an exception - maybe the upload was completed and the queue was empty.
    public void removeOne() {
//        Iterator<DataPoint> iter = this.mDeque.iterator();
//        if (iter.hasNext()) iter.remove();
    }

    public ImmutableList<DataPoint> getSnapshot() {
        return ImmutableList.copyOf(this.mDeque);
    }

    public boolean isEmpty() {
        return mDeque.isEmpty();
    }

    public int size() {
        return mDeque.size();
    }

    public boolean increaseThreshold() {
        if (activeThreshold * 2 < mMaxSize) {
            activeThreshold *= 2;
            return true;
        } else {
            return false;
        }
    }

    public void initThreshold() {
        activeThreshold = this.flushThreshold;
    }

    public boolean shouldBeFlushed() {
        Timber.d("DataQueue:shouldBeFlushed (test) called with size:"+mDeque.size()+" threshold "+flushThreshold);
        return mDeque.size() >= activeThreshold;
    }

    // Give Broadcast capability for updating queue status
    private void initBroadcastReceiver() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                switch (action) {
                    case InternalBroadcasts.KEY_DEBUG_SHOW_REQ:
                        broadcastQueueStatus();
                        break;
                }
            }
        };

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(InternalBroadcasts.KEY_DEBUG_SHOW_REQ);

        if (mLocalBroadcastManager != null) {
            mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, intentFilter);
        }
    }

    private void broadcastQueueStatus() {
        if (TrafficSenseService.isViewActive()) {
            Intent i = new Intent(InternalBroadcasts.KEY_QUEUE_LENGTH_UPDATE);
            i.putExtra(InternalBroadcasts.LABEL_QUEUE_LENGTH, size());
            i.putExtra(InternalBroadcasts.LABEL_QUEUE_THRESHOLD, activeThreshold);
            mLocalBroadcastManager.sendBroadcast(i);
        }
    }
}
