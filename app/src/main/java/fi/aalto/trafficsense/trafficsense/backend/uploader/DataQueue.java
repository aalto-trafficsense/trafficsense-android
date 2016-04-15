package fi.aalto.trafficsense.trafficsense.backend.uploader;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.ImmutableList;
import fi.aalto.trafficsense.trafficsense.util.DataPacket;
import timber.log.Timber;

import java.util.Iterator;
import java.util.Queue;

public class DataQueue {
    private final Queue<DataPoint> mDeque;
    private final int flushThreshold;
    private final int mMaxSize;
    private int activeThreshold;

    private long mNextSequence;

    public DataQueue(int maxSize, int flushThreshold) {
        mMaxSize = maxSize;
        this.mDeque = EvictingQueue.create(mMaxSize);
        this.flushThreshold = flushThreshold;
        activeThreshold = this.flushThreshold;
        Timber.d("DataQueue: constructor called with maxSize: "+maxSize+" flushThreshold: "+flushThreshold);
    }

    public void onDataReady(DataPacket data) {

        DataPoint dataPoint = new DataPoint(System.currentTimeMillis(), mNextSequence++, data.getLocationData(), data.getActivityData());
        this.mDeque.add(dataPoint);
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
    }

    // MJR: Auxiliary procedure to help cope with mysterious "500 INTERNAL SERVER ERROR"s during upload.
    // TODO: Remove method, when root cause for errors has been cleared
    public void removeOne() {
        Iterator<DataPoint> iter = this.mDeque.iterator();
        if (iter.hasNext()) iter.remove();
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
        if (activeThreshold + flushThreshold < mMaxSize) {
            activeThreshold += flushThreshold;
            return true;
        } else {
            return false;
        }
    }

    public boolean shouldBeFlushed() {
        Timber.d("DataQueue:shouldBeFlushed (test) called with size:"+mDeque.size()+" threshold "+flushThreshold);
        return mDeque.size() >= activeThreshold;
    }
}
