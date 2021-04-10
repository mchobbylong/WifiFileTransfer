package computing.project.wififiletransfer;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Locale;

import computing.project.wififiletransfer.common.Constants;
import computing.project.wififiletransfer.model.FileTransfer;
import computing.project.wififiletransfer.service.FileReceiverService;

/**
 * 作者：chenZY
 * 时间：2018/4/3 14:53
 * 描述：https://www.jianshu.com/u/9df45b87cfdf
 * https://github.com/leavesC
 */
public class FileReceiverActivity extends BaseActivity {

    private FileReceiverService fileReceiverService;

    private ProgressDialog progressDialog;

    private static final String TAG = "ReceiverActivity";

    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            FileReceiverService.MyBinder binder = (FileReceiverService.MyBinder) service;
            fileReceiverService = binder.getService();
            fileReceiverService.setProgressChangListener(progressChangListener);
            if (!fileReceiverService.isRunning()) {
                FileReceiverService.startActionTransfer(FileReceiverActivity.this);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            fileReceiverService = null;
            bindService(FileReceiverService.class, serviceConnection);
        }
    };


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

    private FileReceiverService.OnReceiveProgressChangeListener progressChangListener = new FileReceiverService.OnReceiveProgressChangeListener() {

        private FileTransfer originFileTransfer;

        @Override
        public void onProgressChanged(final FileTransfer fileTransfer, final long totalTime, final int progress, final double instantSpeed, final long instantRemainingTime, final double averageSpeed, final long averageRemainingTime) {
            this.originFileTransfer = fileTransfer;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isCreated()) {
                        progressDialog.setTitle("正在接收的文件： " + originFileTransfer.getFileName());
                        if (progress != 100) {
                            progressDialog.setMessage("原始文件的MD5码是：" + originFileTransfer.getMd5()
                                    + "\n\n" + "总的传输时间：" + totalTime + " 秒"
                                    + "\n\n" + "瞬时-传输速率：" + (int) instantSpeed + " Kb/s"
                                    + "\n" + "瞬时-预估的剩余完成时间：" + instantRemainingTime + " 秒"
                                    + "\n\n" + "平均-传输速率：" + (int) averageSpeed + " Kb/s"
                                    + "\n" + "平均-预估的剩余完成时间：" + averageRemainingTime + " 秒"
                            );
                        }
                        progressDialog.setProgress(progress);
                        progressDialog.setCancelable(true);
                        progressDialog.setButton(DialogInterface.BUTTON_POSITIVE, "继续", new DialogInterface.OnClickListener() {

                            @Override

                            public void onClick(DialogInterface dialog, int which) {

                                // TODO: Resume the transmission
                                fileReceiverService.setResume();
                            }

                        });

                        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "暂停", new DialogInterface.OnClickListener() {

                            @Override

                            public void onClick(DialogInterface dialog, int which) {

                                // TODO: Pause the transmission
                                fileReceiverService.setPause();
                            }

                        });
                        progressDialog.show();
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
                        progressDialog.setTitle("传输结束，正在计算本地文件的MD5码以校验文件完整性");
                        progressDialog.setMessage("原始文件的MD5码是：" + originFileTransfer.getMd5());
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
                        progressDialog.setTitle("传输成功");
                        progressDialog.setMessage("原始文件的MD5码是：" + originFileTransfer.getMd5()
                                + "\n" + "本地文件的MD5码是：" + fileTransfer.getMd5()
                                + "\n" + "文件位置：" + fileTransfer.getFilePath());
                        progressDialog.setCancelable(true);
                        progressDialog.show();

                        // 如果这是图片，则调用 Gilde 显示图片
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
                        progressDialog.setTitle("传输失败");
                        progressDialog.setMessage("原始文件的MD5码是：" + originFileTransfer.getMd5()
                                + "\n" + "本地文件的MD5码是：" + fileTransfer.getMd5()
                                + "\n" + "文件位置：" + fileTransfer.getFilePath()
                                + "\n" + "异常信息：" + e.getMessage());
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
        bindService(FileReceiverService.class, serviceConnection);
    }

    private void initView() {
        setTitle("接收文件");
        iv_image = findViewById(R.id.iv_image);
        TextView tv_hint = findViewById(R.id.tv_hint);
        tv_hint.setText(MessageFormat.format("本机IP地址：{0}", getIpAddressString()));
        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setTitle("正在接收文件");
        progressDialog.setMax(100);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fileReceiverService != null) {
            fileReceiverService.setProgressChangListener(null);
            unbindService(serviceConnection);
        }
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void openFile(String filePath) {
        String ext = filePath.substring(filePath.lastIndexOf('.')).toLowerCase(Locale.US);
        try {
            MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
            String mime = mimeTypeMap.getMimeTypeFromExtension(ext.substring(1));
            mime = TextUtils.isEmpty(mime) ? "" : mime;
            Intent intent = new Intent();
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction(android.content.Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(new File(filePath)), mime);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "文件打开异常：" + e.getMessage());
            showToast("文件打开异常：" + e.getMessage());
        }
    }

}