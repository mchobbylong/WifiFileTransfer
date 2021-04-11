package computing.project.wififiletransfer;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import me.rosuh.filepicker.config.FilePickerManager;

import java.io.File;
import java.util.List;
import java.util.concurrent.Future;

import computing.project.wififiletransfer.manager.WifiLManager;
import computing.project.wififiletransfer.model.FileTransfer;
import computing.project.wififiletransfer.common.OnTransferChangeListener;
import computing.project.wififiletransfer.service.FileSenderTask;

public class FileSenderActivity extends BaseActivity {

    public static final String TAG = "FileSenderActivity";

    private static final int CODE_CHOOSE_FILE = 100;

    private ProgressDialog progressDialog;

    private FileSenderTask task;

    private Future taskFuture;

    private OnTransferChangeListener onTransferChangeListener = new OnTransferChangeListener() {

        @Override
        public void onStartComputeMD5() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isCreated()) {
                        progressDialog.setTitle("send files");
                        progressDialog.setMessage("Calculating MD5 code of file");
                        progressDialog.setCancelable(false);
                        progressDialog.show();
                        progressDialog.setProgress(0);
                        progressDialog.getButton(DialogInterface.BUTTON_POSITIVE).setVisibility(View.INVISIBLE);
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
                        progressDialog.setTitle("Sending file： " + fileTransfer.getFileName());
                        if (progress != 100) {
                            progressDialog.setMessage("MD5 code of file：" + fileTransfer.getMd5()
                                    + "\n\n" + "Total transmission time：" + totalTime + " seconds"
                                    + "\n\n" + "Instantaneous transmission rate：" + (int) instantSpeed + " Kb/s"
                                    + "\n" + "Instantaneous - estimated remaining completion time：" + instantRemainingTime + " seconds"
                                    + "\n\n" + "Average transmission rate：" + (int) averageSpeed + " Kb/s"
                                    + "\n" + "Average - estimated remaining completion time：" + averageRemainingTime + " seconds"
                            );
                        }
                        progressDialog.setCancelable(true);
                        progressDialog.show();
                        progressDialog.setProgress(progress);
                        progressDialog.getButton(DialogInterface.BUTTON_POSITIVE).setVisibility(View.VISIBLE);
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
                        progressDialog.setTitle("File sent successfully");
                        progressDialog.setMessage("File sent successfully：" + fileTransfer.getFileName());
                        progressDialog.setCancelable(true);
                        progressDialog.show();
                        progressDialog.setProgress(100);
                        progressDialog.getButton(DialogInterface.BUTTON_POSITIVE).setVisibility(View.INVISIBLE);
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
                        progressDialog.setTitle("Failed to send file");
                        progressDialog.setMessage("Abnormal information： " + e.getMessage());
                        progressDialog.setCancelable(true);
                        progressDialog.show();
                        progressDialog.getButton(DialogInterface.BUTTON_POSITIVE).setVisibility(View.INVISIBLE);
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

    private void initView() {
        setTitle("send files");
        TextView tv_hint = findViewById(R.id.tv_hint);
        // 初始化接收端 IP 地址为当前连接 Wifi 的网关（假设连上了接收端开的热点）
        TextView serverIp = findViewById(R.id.et_serverIp);
        serverIp.setText(WifiLManager.getHotspotIpAddress(this));

        // TODO: 废弃 ProgressDialog
        // 初始化 ProgressDialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setTitle("send files");
        progressDialog.setMax(100);
        progressDialog.setIndeterminate(false);
        // 添加取消按钮
        progressDialog.setButton(DialogInterface.BUTTON_POSITIVE, "cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (taskFuture != null)
                    taskFuture.cancel(true);
                taskFuture = null;
                task = null;
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
        if (taskFuture != null) {
            taskFuture.cancel(true);
        }
        progressDialog.dismiss();
    }

    public void sendFile(View view) {
        // if (!Constants.AP_SSID.equals(WifiLManager.getConnectedSSID(this))) {
        //     showToast("当前连接的Wifi并非文件接收端开启的Wifi热点，请重试");
        //     return;
        // }
        navToChose();
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
                    FileTransfer fileTransfer = new FileTransfer(file);
                    Log.e(TAG, "Files to be sent：" + fileTransfer);
                    TextView tv = findViewById(R.id.et_serverIp);
                    task = new FileSenderTask(fileTransfer, tv.getText().toString(), onTransferChangeListener);
                    taskFuture = ((CoreApplication) this.getApplication()).threadPool.submit(task);
                }
            }
        }
    }

    private void navToChose() {
        FilePickerManager.INSTANCE
                .from(this)
                .enableSingleChoice()
                .forResult(CODE_CHOOSE_FILE);
    }

}
