package fi.aalto.trafficsense.trafficsense.backend.uploader;

import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.content.LocalBroadcastManager;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import fi.aalto.trafficsense.trafficsense.TrafficSenseApplication;
import fi.aalto.trafficsense.trafficsense.backend.TrafficSenseService;
import fi.aalto.trafficsense.trafficsense.backend.rest.RestClient;
import fi.aalto.trafficsense.trafficsense.util.BackendStorage;
import fi.aalto.trafficsense.trafficsense.util.Callback;
import fi.aalto.trafficsense.trafficsense.util.DataPacket;
import fi.aalto.trafficsense.trafficsense.util.ThreadGlue;
import timber.log.Timber;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * All pipeline operations should be executed in a single PipelineThread. The thread uses a
 * Looper, so work can be pushed to the thread with a Handler connected to the Looper.
 * <p/>
 * All work outside Runnables pushed to the Handler must be thread-safe.
 */
public class PipelineThread {
    private final HandlerThread mHandlerThread;
    private final Handler mHandler;
    private final DataQueue mDataQueue;
    private final RestClient mRestClient;
    private final ThreadGlue mThreadGlue = new ThreadGlue();

    private static LocalBroadcastManager mLocalBroadcastManager;


    /**
     * This factory method creates the PipelineThread by using the handler which will be used
     * by the PipelineThread itself. This guarantees that the constructor runs in the same thread
     * as all the important PipelineThread operations.
     */
    public static ListenableFuture<PipelineThread> create(final HandlerThread handlerThread) throws InterruptedException {
        final Handler handler = new Handler(handlerThread.getLooper());
        final SettableFuture<PipelineThread> future = SettableFuture.create();
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    future.set(new PipelineThread(handlerThread, handler));
                } catch (Exception e) {
                    future.setException(e);
                }
            }
        });
        return future;
    }

    private PipelineThread(HandlerThread handlerThread, Handler handler) {
        this.mHandlerThread = handlerThread;
        this.mHandler = handler;
        // TODO: This bs is completely weird at the moment - redo parameter storage ASAP
        BackendStorage bs=BackendStorage.create(TrafficSenseApplication.getContext());
        this.mDataQueue = new DataQueue();
        this.mRestClient = new RestClient(TrafficSenseApplication.getContext(), Uri.parse(bs.getServerName()), bs, handler);
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(TrafficSenseService.getContext());
    }

    public void sendData (final DataPacket p) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mThreadGlue.verify();
                Timber.d("onDataReceived with isUploadEnabled: "+mRestClient.isUploadEnabled()+" and isUploading: "+mRestClient.isUploading());
                mDataQueue.onDataReady(p);
                if (mDataQueue.shouldBeFlushed()) {
                    if (!mRestClient.isUploadEnabled()) {
                        mDataQueue.increaseThreshold();
                    } else { // Upload is enabled
                        if (!mRestClient.isUploading()) {
                            mRestClient.uploadData(mDataQueue);
                        }
                    }
                }
            }
        });
    }

    public void requestUpload() {
        mHandler.post(new Runnable() {
                          @Override
                          public void run() {
                              mThreadGlue.verify();
                              if (!mRestClient.isUploading()) {
                                  mRestClient.uploadData(mDataQueue);
                              }
                          }

                      }
        );
    }

    /**
     * Try sending all data in data queue to server and wait for it to finish.
     *
     * @return false if failed to trigger data transfer or if worker thread was interrupted
     */
    public boolean forceFlushDataToServer() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Boolean> interruptedState = new AtomicReference<>(Boolean.valueOf(false));
        final Runnable task = new Runnable() {
            @Override
            public void run() {
                mThreadGlue.verify();
                try {
                    /**
                     * Only one data completed operation (or force flush operation) access
                     * rest client at a time.
                     *
                     * This procedure waits until previous data upload operations are completed
                     * and then triggers the upload
                     **/
                    if (mRestClient.isUploadEnabled()) {
                        Timber.d("force flushing data to server: " + mDataQueue.size()
                                + " items queued");
                        mRestClient.waitAndUploadData(mDataQueue);
                    } else {
                        Timber.d("upload data to server is disabled: " + mDataQueue.size()
                                + " items in queue were not uploaded");
                    }
                } catch (InterruptedException intEx) {
                    interruptedState.set(true);
                } finally {
                    latch.countDown();
                }
            }
        };
        if (!mHandler.postAtFrontOfQueue(task))
            return false;

        latch.await();
        mRestClient.waitTillNotUploading();

        if (mRestClient.isUploadEnabled())
            Timber.d("forceFlushDataToServer: completed");
        else
            Timber.d("forceFlushDataToServer: aborted (uploading disabled)");
        return !interruptedState.get();


    }

    /**
     * Trigger fetching client number from server
     */
    public void fetchClientNumber(final Callback<Optional<Integer>> callback) {
        mRestClient.fetchClientNumber(new Callback<Optional<Integer>>() {
            @Override
            public void run(Optional<Integer> result, RuntimeException error) {
                callback.run(result, error);
            }
        });
    }

    public boolean destroy() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final Runnable task = new Runnable() {
            @Override
            public void run() {
                destroyInternal();
                latch.countDown();
            }
        };
        if (!mHandler.postAtFrontOfQueue(task))
            return false;
        latch.await();
        return true;
    }

    public void setUploadEnabledState(boolean enabled) {
        mRestClient.setUploadEnabledState(enabled);
    }

    public boolean getUploadEnabledState() {
        return mRestClient.isUploadEnabled();
    }

//    public boolean getUploadingState() {
//        return mRestClient.isUploading();
//    }

    private void destroyInternal() {
        mThreadGlue.verify();
        mRestClient.destroy();
        mHandlerThread.quit();
    }

}