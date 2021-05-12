package computing.project.wififiletransfer.service;

import android.util.Log;

import java.io.Closeable;
import java.io.IOException;

public abstract class PauseableRunnable implements Runnable {

    static String TAG = "PausedRunnable";

    private final Object state = new Object();
    protected volatile boolean suspended = false;

    public void suspend() {
        suspended = true;
        Log.i(TAG, "Paused");
    }

    public void resume() {
        Log.i(TAG, "Resumed");
        suspended = false;
        synchronized (state) {
            state.notifyAll();
        }
    }

    protected void tryResume() throws InterruptedException {
        synchronized (state) {
            while (suspended) state.wait();
        }
    }

    public boolean isSuspended() { return suspended; }

    public void closeStream(Closeable s) {
        if (s != null) {
            try {
                s.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
