package fi.aalto.trafficsense.trafficsense.backend.uploader;

import android.os.HandlerThread;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.Atomics;
import fi.aalto.trafficsense.trafficsense.util.Callback;
import fi.aalto.trafficsense.trafficsense.util.DataPacket;
import timber.log.Timber;

import java.util.concurrent.atomic.AtomicReference;

public class RegularRoutesPipeline {
    private final AtomicReference<PipelineThread> mThread = Atomics.newReference();
    private static final AtomicReference<PipelineThread> sPipeline = Atomics.newReference();

    public RegularRoutesPipeline () {
        Timber.d("RegularRoutesPipeline constructor called");

        if (mThread.get() == null) {
            HandlerThread handlerThread = new HandlerThread(PipelineThread.class.getSimpleName());
            handlerThread.start();

            PipelineThread thread;
            try {
                thread = PipelineThread.create(handlerThread).get();
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
            mThread.set(thread);
            sPipeline.set(thread);
        }

    }

    public void disconnect() {
        PipelineThread thread = mThread.getAndSet(null);
        if (thread != null) {
            try {
                boolean result = thread.destroy();
                if (!result)
                    Timber.w("Failed to destroy PipelineThread");
            } catch (InterruptedException e) {
                Timber.e(e, "Failed to destroy PipelineThread");
            }
        }
    }

    public boolean isEnabled() {
        return mThread.get() != null;
    }

    public PipelineThread getPipelineThread() {
        return sPipeline.get();
    }

    public boolean flushDataQueueToServer() {
        PipelineThread pipeline = sPipeline.get();
        if (pipeline == null)
            return false;

        try {
            pipeline.forceFlushDataToServer();
        } catch (Exception ex) {
            Timber.e("Failed to flush data queue to server: " + ex.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Fetch client number from the server
     *
     * @param callback callback that gets executed when the value is ready (or null in error case)
     */
    public static void fetchClientNumber(Callback<Optional<Integer>> callback) {
        PipelineThread pipeline = sPipeline.get();
        if (pipeline == null)
            callback.run(Optional.<Integer>absent(), new RuntimeException("Pipeline is not initialized"));
        else {
            pipeline.fetchClientNumber(callback);
        }
    }

    /**
     * Get state if pipeline is uploading data or not
     *
     * @return true, if uploading is ongoing
     */
    public static boolean isUploading() {
        PipelineThread pipeline = sPipeline.get();
        if (pipeline == null)
            return false;

        return pipeline.getUploadingState();
    }

    /**
     * Get enabled state of upload procedure
     *
     * @return true, if uploading is enabled
     */
    public static boolean isUploadEnabled() {
        PipelineThread pipeline = sPipeline.get();
        if (pipeline == null)
            return false;

        return pipeline.getUploadEnabledState();
    }

    /**
     * Set enabled state of upload procedure
     *
     * @return true, if state was changed successfully
     */
    public static boolean setUploadEnabledState(boolean enabled) {
        PipelineThread pipeline = sPipeline.get();
        if (pipeline == null)
            return false;

        pipeline.setUploadEnabledState(enabled);
        return true;
    }

    /*
     * A Helper method to fetch the current queue size from pipelineThread
     */
    public static int queueSize() {
        PipelineThread pipeline = sPipeline.get();
        if (pipeline == null)
            return 0;
        return pipeline.queueSize();
    }

}
