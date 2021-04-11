package computing.project.wififiletransfer.common;

import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import computing.project.wififiletransfer.model.FileTransfer;

public class SpeedMonitor {
    // 计算瞬时传输速率的间隔时间
    private static final int MONITOR_PERIOD = 500;

    private final FileTransfer fileTransfer;
    private final OnTransferChangeListener listener;
    private ScheduledExecutorService scheduledExecutor;
    private Date startTime;
    private long lastProgress;

    public SpeedMonitor(FileTransfer fileTransfer, OnTransferChangeListener listener) {
        this.fileTransfer = fileTransfer;
        this.listener = listener;
    }

    Runnable speedMonitor = new Runnable() {
        @Override
        public void run() {
            long progress = fileTransfer.getProgress();
            long fileSize = fileTransfer.getFileSize();
            long transferred = progress - lastProgress;
            lastProgress = progress;
            double progressPercent = progress * 100.0 / fileSize;
            double instantSpeed = 0;
            long instantRemainingTime = 0;
            if (transferred > 0) {
                instantSpeed = transferred / 1024.0 / (MONITOR_PERIOD / 1000.0);
                instantRemainingTime = (long) ((fileSize - progress) / 1024.0 / instantSpeed);
            }
            long totalTime = (new Date().getTime() - startTime.getTime()) / 1000;
            double averageSpeed = progress / 1024.0 / totalTime;
            long averageRemainingTime = fileSize;
            if (progress > 0) {
                averageRemainingTime = (long) ((fileSize - progress) / 1024.0 / averageSpeed);
            }

            listener.onProgressChanged(
                    fileTransfer,
                    totalTime,
                    (int)progressPercent,
                    instantSpeed,
                    instantRemainingTime,
                    averageSpeed,
                    averageRemainingTime);
        }
    };

    public void start() {
        stop();
        lastProgress = fileTransfer.getProgress();
        startTime = new Date();
        scheduledExecutor = Executors.newScheduledThreadPool(1);
        scheduledExecutor.scheduleAtFixedRate(speedMonitor, MONITOR_PERIOD, MONITOR_PERIOD, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        if (scheduledExecutor != null && !scheduledExecutor.isShutdown())
            scheduledExecutor.shutdownNow();
        scheduledExecutor = null;
    }
}
