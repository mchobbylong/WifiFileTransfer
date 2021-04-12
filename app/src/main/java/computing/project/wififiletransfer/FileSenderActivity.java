package computing.project.wififiletransfer;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.res.ResourcesCompat;

import me.rosuh.filepicker.config.FilePickerManager;

import java.io.File;
import java.util.List;
import java.util.concurrent.Future;

import computing.project.wififiletransfer.manager.WifiLManager;
import computing.project.wififiletransfer.model.FileTransfer;
import computing.project.wififiletransfer.common.OnTransferChangeListener;
import computing.project.wififiletransfer.common.CommonUtils;
import computing.project.wififiletransfer.service.FileSenderTask;


public class FileSenderActivity extends BaseActivity {

    public static final String TAG = "FileSenderActivity";

    private static final int CODE_CHOOSE_FILE = 100;

    private FileSenderTask task;

    private Future taskFuture;

    private OnTransferChangeListener onTransferChangeListener = new OnTransferChangeListener() {

        @Override
        public void onStartComputeMD5(final FileTransfer fileTransfer) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isCreated()) {
                        filename.setText(fileTransfer.getFileName());
                        progressText.setText("0");
                        progressBar.setProgress(0);
                        size.setText(fileTransfer.getFileSizeText());
                        status.setText("Calculating MD5");
                        status.setTextColor(getResources().getColor(android.R.color.tab_indicator_text));
                        speed.setText("0KB/s");

                        // 禁用两个按钮
                        buttonSuspend.setEnabled(false);
                        buttonSuspend.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_suspend_disabled, null));
                        buttonInterrupt.setEnabled(false);
                        buttonInterrupt.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_delete_disabled, null));

                        progressView.setVisibility(View.VISIBLE);
                    }
                }
            });
        }

        @Override
        public void onStartTransfer(final FileTransfer fileTransfer) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    filename.setText(fileTransfer.getFileName());
                    progressText.setText("0");
                    progressBar.setProgress(0);
                    size.setText(fileTransfer.getFileSizeText());
                    speed.setText("0KB/s");
                    status.setTextColor(getResources().getColor(android.R.color.tab_indicator_text));

                    // 启用两个按钮
                    buttonSuspend.setEnabled(true);
                    buttonSuspend.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_suspend, null));
                    buttonInterrupt.setEnabled(true);
                    buttonInterrupt.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_delete, null));

                    progressView.setVisibility(View.VISIBLE);
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
        public void onTransferSucceed(final FileTransfer fileTransfer) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isCreated()) {
                        progressText.setText("100");
                        progressBar.setProgress(100);
                        status.setText("Transfer succeed");
                        status.setTextColor(getResources().getColor(R.color.colorSuccess));
                        Toast.makeText(FileSenderActivity.this, "Transfer succeed", Toast.LENGTH_SHORT).show();

                        // 启用选择文件的按钮
                        buttonSelectFile.setEnabled(true);

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
        public void onTransferFailed(FileTransfer fileTransfer, final Exception e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isCreated()) {
                        String statusText = "Transfer failed: " + e.getMessage();
                        status.setText(statusText);
                        status.setTextColor(Color.RED);
                        Toast.makeText(FileSenderActivity.this, statusText, Toast.LENGTH_SHORT).show();

                        // 启用选择文件的按钮
                        buttonSelectFile.setEnabled(true);

                        // 禁用两个按钮
                        buttonSuspend.setEnabled(false);
                        buttonSuspend.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_suspend_disabled, null));
                        buttonInterrupt.setEnabled(false);
                        buttonInterrupt.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_delete_disabled, null));
                    }
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_sender);
        initView();
    }

    private ViewGroup progressView;
    private TextView serverIp;
    private TextView progressText;
    private TextView filename;
    private ProgressBar progressBar;
    private TextView size;
    private TextView status;
    private TextView speed;
    private Button buttonSuspend;
    private Button buttonInterrupt;
    private Button buttonSelectFile;

    private void initView() {
        setTitle("Send Files");
        TextView tv_hint = findViewById(R.id.tv_hint);
        tv_hint.setText("Connect to the same WiFi with the receiver, then input the receiver's IP address.");
        // 初始化接收端 IP 地址为当前连接 Wifi 的网关（假设连上了接收端开的热点）
        serverIp = findViewById(R.id.et_serverIp);
        serverIp.setText(WifiLManager.getGatewayIpAddress(this));
        buttonSelectFile = findViewById(R.id.bn_select);

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
        buttonSuspend = progressView.findViewById(R.id.bn_toggle_suspension);
        buttonSuspend.setVisibility(View.VISIBLE);
        buttonInterrupt = progressView.findViewById(R.id.bn_interrupt);
        buttonInterrupt.setVisibility(View.VISIBLE);
        Button buttonOpenFile = progressView.findViewById(R.id.bn_open_file);
        buttonOpenFile.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
        if (taskFuture != null) {
            taskFuture.cancel(true);
        }
    }

    public void navToChose(View view) {
        FilePickerManager.INSTANCE
                .from(this)
                .enableSingleChoice()
                .forResult(CODE_CHOOSE_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CODE_CHOOSE_FILE && resultCode == RESULT_OK) {
            List<String> paths = FilePickerManager.INSTANCE.obtainData();
            if (!paths.isEmpty()) {
                String path = paths.get(0);
                File file = new File(path);
                if (file.exists()) {
                    buttonSelectFile.setEnabled(false);

                    FileTransfer fileTransfer = new FileTransfer(file);
                    Log.i(TAG, "Files to be sent：" + fileTransfer);
                    task = new FileSenderTask(fileTransfer, serverIp.getText().toString(), onTransferChangeListener);
                    taskFuture = ((CoreApplication) this.getApplication()).threadPool.submit(task);
                }
            }
        }
    }

    public void toggleTaskSuspension(View view) {
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

    public void interruptTask(View view) {
        Log.d(TAG, "Interrupt received");
        if (task == null || taskFuture == null) return;
        taskFuture.cancel(true);
        task = null;
        taskFuture = null;
    }
}
