package computing.project.wififiletransfer;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.concurrent.Future;

import computing.project.wififiletransfer.common.OnTransferChangeListener;
import computing.project.wififiletransfer.manager.WifiLManager;
import computing.project.wififiletransfer.model.FileTransfer;
import computing.project.wififiletransfer.service.FileReceiverTask;


public class FileReceiverActivity extends BaseActivity {

    private static final String TAG = "ReceiverActivity";

    private ProgressDialog progressDialog;

    private FileReceiverTask task;

    private Future taskFuture;


    public static String getIpAddressString() {
        try {
            for (Enumeration<NetworkInterface> enNetI = NetworkInterface
                    .getNetworkInterfaces(); enNetI.hasMoreElements(); ) {
                NetworkInterface netI = enNetI.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = netI
                        .getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (inetAddress instanceof Inet4Address && !inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return "0.0.0.0";
    }

    private OnTransferChangeListener onTransferChangeListener = new OnTransferChangeListener() {

        private FileTransfer originFileTransfer = null;

        @Override
        public void onProgressChanged(final FileTransfer fileTransfer, final long totalTime, final int progress, final double instantSpeed, final long instantRemainingTime, final double averageSpeed, final long averageRemainingTime) {
            this.originFileTransfer = fileTransfer.clone();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isCreated()) {
                        progressDialog.setTitle("Files being received： " + originFileTransfer.getFileName());
                        if (progress != 100) {
                            progressDialog.setMessage("The MD5 code of the original file is：" + originFileTransfer.getMd5()
                                    + "\n\n" + "Total transmission time：" + totalTime + " seconds"
                                    + "\n\n" + "Instantaneous transmission rate：" + (int) instantSpeed + " Kb/s"
                                    + "\n" + "Instantaneous - estimated remaining completion time：" + instantRemainingTime + " seconds"
                                    + "\n\n" + "Average transmission rate：" + (int) averageSpeed + " Kb/s"
                                    + "\n" + "Average - estimated remaining completion time：" + averageRemainingTime + " seconds"
                            );
                        }
                        progressDialog.setCancelable(true);
                        progressDialog.setButton(DialogInterface.BUTTON_POSITIVE, "continue", new DialogInterface.OnClickListener() {

                            @Override

                            public void onClick(DialogInterface dialog, int which) {
                                // Resume the transmission
                                task.resume();
                            }

                        });

                        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "suspend", new DialogInterface.OnClickListener() {

                            @Override

                            public void onClick(DialogInterface dialog, int which) {
                                // Pause the transmission
                                task.suspend();
                            }

                        });
                        progressDialog.show();
                        progressDialog.setProgress(progress);
                    }
                }
            });
        }

        @Override
        public void onStartComputeMD5() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isCreated()) {
                        progressDialog.setTitle("The transmission is over. Calculating MD5 code of local file to verify file integrity");
                        progressDialog.setMessage("The MD5 code of the original file is:" + originFileTransfer.getMd5());
                        progressDialog.setCancelable(false);
                        progressDialog.show();
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
                        progressDialog.setTitle("Transmission successful");
                        progressDialog.setMessage("The MD5 code of the original file is：" + originFileTransfer.getMd5()
                                + "\n" + "The MD5 code of the local file is：" + fileTransfer.getMd5()
                                + "\n" + "file location：" + fileTransfer.getFilePath());
                        progressDialog.setCancelable(true);
                        progressDialog.show();
                        progressDialog.setProgress(100);

                        // 如果这是图片，则调用 Glide 显示图片
                        BitmapFactory.Options bitmapOpts = new BitmapFactory.Options();
                        bitmapOpts.inJustDecodeBounds = true;
                        Bitmap _temp = BitmapFactory.decodeFile(fileTransfer.getFilePath(), bitmapOpts);
                        if (bitmapOpts.outHeight != -1 && bitmapOpts.outWidth != -1)
                            Glide.with(FileReceiverActivity.this).load(fileTransfer.getFilePath()).into(iv_image);
                        else
                            OpenFileUtil.openFileByPath(FileReceiverActivity.this, fileTransfer.getFilePath());
                    }
                }
            });
        }

        @Override
        public void onTransferFailed(final FileTransfer fileTransfer, final Exception e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isCreated()) {
                        progressDialog.setTitle("Transmission failed");
                        if (fileTransfer == null || originFileTransfer == null)
                            progressDialog.setMessage("Abnormal information：" + e.getMessage());
                        else
                            progressDialog.setMessage("The MD5 code of the original file is：" + originFileTransfer.getMd5()
                                    + "\n" + "The MD5 code of the local file is：" + fileTransfer.getMd5()
                                    + "\n" + "file location：" + fileTransfer.getFilePath()
                                    + "\n" + "abnormal information：" + e.getMessage());
                        progressDialog.setCancelable(true);
                        progressDialog.show();
                    }
                }
            });
        }
    };

    private ImageView iv_image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_receiver);
        initView();
        task = new FileReceiverTask(this, onTransferChangeListener);
        taskFuture = ((CoreApplication) getApplication()).threadPool.submit(task);
    }

    private void initView() {
        setTitle("Receive files");
        iv_image = findViewById(R.id.iv_image);
        TextView tv_hint = findViewById(R.id.tv_hint);
        tv_hint.setText(MessageFormat.format("Local IP address：{0}", WifiLManager.getLocalIpAddress(this)));
        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setTitle("File being received");
        progressDialog.setMax(100);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (taskFuture != null) {
            Log.i(TAG, "Canceling receiving thread");
            taskFuture.cancel(true);
        }
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
