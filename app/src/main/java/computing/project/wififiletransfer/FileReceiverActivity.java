package computing.project.wififiletransfer;

import android.annotation.SuppressLint;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.bumptech.glide.Glide;

import java.util.concurrent.Future;

import computing.project.wififiletransfer.common.CommonUtils;
import computing.project.wififiletransfer.manager.WifiLManager;
import computing.project.wififiletransfer.model.FileTransfer;
import computing.project.wififiletransfer.service.FileReceiverTask;


public class FileReceiverActivity extends BaseActivity {

    private static final String TAG = "ReceiverActivity";

    private FileReceiverTask task;

    private Future taskFuture;

    private FileTransfer receivedFile;

    private class OnTransferChangeListener implements computing.project.wififiletransfer.common.OnTransferChangeListener {

        @Override
        public void onStartTransfer(final FileTransfer fileTransfer) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isCreated()) {
                        filename.setText(fileTransfer.getFileName());
                        progressText.setText("0");
                        progressBar.setProgress(0);
                        size.setText(fileTransfer.getFileSizeText());
                        status.setText("");
                        status.setTextColor(getResources().getColor(android.R.color.tab_indicator_text));
                        speed.setText("0KB/s");

                        // 隐藏打开文件的按钮，并显示两个控制按钮
                        buttonOpenFile.setVisibility(View.GONE);
                        buttonSuspend.setVisibility(View.VISIBLE);
                        buttonInterrupt.setVisibility(View.VISIBLE);

                        // 启用两个按钮
                        buttonSuspend.setEnabled(true);
                        buttonSuspend.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_suspend, null));
                        buttonInterrupt.setEnabled(true);
                        buttonInterrupt.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_delete, null));

                        progressView.setVisibility(View.VISIBLE);
                    }
                }
            });
        }

        @Override
        public void onProgressChanged(final FileTransfer fileTransfer, final long totalTime, final int progress, final double instantSpeed, final long instantRemainingTime, final double averageSpeed, final long averageRemainingTime) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isCreated()) {
                        progressText.setText(String.valueOf(progress));
                        progressBar.setProgress(progress);
                        status.setText(CommonUtils.getRemainingTimeText(averageRemainingTime));
                        speed.setText(CommonUtils.getSpeedText(instantSpeed));
                    }
                }
            });
        }

        @Override
        public void onStartComputeMD5(final FileTransfer fileTransfer) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isCreated()) {
                        progressText.setText("100");
                        progressBar.setProgress(100);
                        status.setText("Validating MD5");

                        // 禁用两个按钮
                        buttonSuspend.setEnabled(false);
                        buttonSuspend.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_suspend_disabled, null));
                        buttonInterrupt.setEnabled(false);
                        buttonInterrupt.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_delete_disabled, null));
                    }
                }
            });
        }

        @Override
        public void onTransferSucceed(final FileTransfer fileTransfer) {
            receivedFile = fileTransfer;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isCreated()) {
                        status.setText("Transfer succeed");
                        status.setTextColor(getResources().getColor(R.color.colorSuccess));
                        showToast("Transfer succeed");

                        // 禁用两个按钮
                        buttonSuspend.setEnabled(false);
                        buttonSuspend.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_suspend_disabled, null));
                        buttonInterrupt.setEnabled(false);
                        buttonInterrupt.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_delete_disabled, null));

                        // 隐藏两个进度控制按钮，并显示打开文件的按钮
                        buttonSuspend.setVisibility(View.GONE);
                        buttonInterrupt.setVisibility(View.GONE);
                        buttonOpenFile.setVisibility(View.VISIBLE);

                        // 如果这是图片，则调用 Glide 显示图片
                        BitmapFactory.Options bitmapOpts = new BitmapFactory.Options();
                        bitmapOpts.inJustDecodeBounds = true;
                        BitmapFactory.decodeFile(fileTransfer.getFilePath(), bitmapOpts);
                        if (bitmapOpts.outHeight != -1 && bitmapOpts.outWidth != -1)
                            Glide.with(FileReceiverActivity.this).load(fileTransfer.getFilePath()).into(iv_image);
                        else
                            openReceivedFile(null);
                    }
                }
            });
        }

        @Override
        public void onTransferFailed(final FileTransfer fileTransfer, final Exception e) {
            runOnUiThread(new Runnable() {
                @SuppressLint("SetTextI18n")
                @Override
                public void run() {
                    if (isCreated()) {
                        String statusText = "Transfer failed: " + e.getMessage();
                        status.setText(statusText);
                        status.setTextColor(Color.RED);
                        showToast(statusText);

                        // 禁用两个按钮
                        buttonSuspend.setEnabled(false);
                        buttonSuspend.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_suspend_disabled, null));
                        buttonInterrupt.setEnabled(false);
                        buttonInterrupt.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_delete_disabled, null));
                    }
                }
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_receiver);
        initView();
        task = new FileReceiverTask(this, new OnTransferChangeListener());
        taskFuture = ((CoreApplication) getApplication()).threadPool.submit(task);
    }

    private ViewGroup progressView;
    private ImageView iv_image;
    private TextView progressText;
    private TextView filename;
    private ProgressBar progressBar;
    private TextView size;
    private TextView status;
    private TextView speed;
    private Button buttonSuspend;
    private Button buttonInterrupt;
    private Button buttonOpenFile;

    private void initView() {
        setTitle("Receive Files");
        iv_image = findViewById(R.id.iv_image);
        TextView localIp = findViewById(R.id.tv_local_ip);
        localIp.setText(WifiLManager.getLocalIpAddress(this));

        // 初始化进度条
        progressView = findViewById(R.id.group_progress_view);
        progressText = progressView.findViewById(R.id.percent);
        progressText.setText("");
        filename = progressView.findViewById(R.id.filename);
        filename.setText("");
        size = progressView.findViewById(R.id.size);
        size.setText("");
        status = progressView.findViewById(R.id.status);
        status.setText("");
        speed = progressView.findViewById(R.id.speed);
        speed.setText("");
        progressBar = progressView.findViewById(R.id.progress_bar);
        progressBar.setMax(100);
        ViewGroup controlButtonGroup = progressView.findViewById(R.id.group_control_button);
        buttonSuspend = controlButtonGroup.findViewById(R.id.bn_toggle_suspension);
        buttonInterrupt = controlButtonGroup.findViewById(R.id.bn_interrupt);
        buttonOpenFile = progressView.findViewById(R.id.bn_open_file);

        // TODO: ServerSocket.accept() 独立使用一条线程
        // 监听线程不可被用户中断，因此中断按钮暂不可用
        buttonInterrupt.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (taskFuture != null) {
            Log.i(TAG, "正在取消接收端线程");
            taskFuture.cancel(true);
        }
    }

    public void toggleTaskSuspension(View ignored) {
        Log.d(TAG, "Toggle suspension received");
        if (task == null || taskFuture == null || taskFuture.isDone()) return;
        if (task.isSuspended()) {
            task.resume();
            buttonSuspend.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_suspend, null));
        } else {
            task.suspend();
            buttonSuspend.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_play, null));
        }
    }

    public void interruptTask(View ignored) {
        Log.d(TAG, "Interrupt received");
        if (task == null || taskFuture == null) return;
        taskFuture.cancel(true);
        task = null;
        taskFuture = null;
    }

    public void openReceivedFile(View ignored) {
        if (receivedFile == null) return;
        CommonUtils.openFileByPath(this, receivedFile.getFilePath());
    }

}
