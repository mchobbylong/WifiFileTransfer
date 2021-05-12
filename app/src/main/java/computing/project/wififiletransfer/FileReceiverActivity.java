package computing.project.wififiletransfer;

import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.net.Socket;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Future;

import computing.project.wififiletransfer.common.CommonUtils;
import computing.project.wififiletransfer.manager.WifiLManager;
import computing.project.wififiletransfer.model.FileTransfer;
import computing.project.wififiletransfer.model.FileTransferRecorder;
import computing.project.wififiletransfer.model.ProgressViewAdapter;
import computing.project.wififiletransfer.model.ProgressViewModel;
import computing.project.wififiletransfer.service.FileReceiverService;
import computing.project.wififiletransfer.service.FileReceiverTask;


public class FileReceiverActivity extends BaseActivity {

    private static final String TAG = "ReceiverActivity";

    private Future serviceFuture;

    private final List<ProgressViewModel> receivers = new Vector<>();

    private class OnNewTransferListener implements computing.project.wififiletransfer.common.OnNewTransferListener {
        @Override
        public void onNewTransfer(Socket client) {
            runOnUiThread(() -> {
                ProgressViewModel model = new ProgressViewModel(FileReceiverActivity.this);
                receivers.add(0, model);
                adapter.notifyItemInserted(0);
                recyclerView.smoothScrollToPosition(0);
                model.task = new FileReceiverTask(client, new OnTransferChangeListener(model), recorder);
                model.taskFuture = ((CoreApplication) getApplication()).threadPool.submit(model.task);
            });
        }
    }

    private class OnTransferChangeListener implements computing.project.wififiletransfer.common.OnTransferChangeListener {
        ProgressViewModel model;

        public OnTransferChangeListener(ProgressViewModel model) {
            super();
            this.model = model;
        }

        @Override
        public void onReceiveFileTransfer(final FileTransfer fileTransfer) {
            runOnUiThread(() -> {
                if (isCreated()) {
                    model.fileTransfer = fileTransfer;
                    model.status = String.format("From \"%s\", accept?", fileTransfer.getSenderName());
                    model.statusColor = getResources().getColor(R.color.colorOrange);
                    model.state = ProgressViewModel.ControlButtonState.CONFIRM;

                    int position = receivers.indexOf(model);
                    adapter.notifyItemChanged(position, new ProgressViewModel.Field[]{
                            ProgressViewModel.Field.filename,
                            ProgressViewModel.Field.size,
                            ProgressViewModel.Field.status,
                            ProgressViewModel.Field.state,
                    });
                }
            });
        }

        @Override
        public void onStartTransfer(final FileTransfer fileTransfer) {
            runOnUiThread(() -> {
                if (isCreated()) {
                    model.status = "";
                    model.statusColor = getResources().getColor(android.R.color.tab_indicator_text);
                    model.state = ProgressViewModel.ControlButtonState.TRANSIT;
                    int position = receivers.indexOf(model);
                    adapter.notifyItemChanged(position, new ProgressViewModel.Field[]{
                            ProgressViewModel.Field.status,
                            ProgressViewModel.Field.state,
                    });
                }
            });
        }

        @Override
        public void onProgressChanged(final FileTransfer fileTransfer, final long totalTime, final int progress, final double instantSpeed, final long instantRemainingTime, final double averageSpeed, final long averageRemainingTime) {
            runOnUiThread(() -> {
                if (isCreated()) {
                    model.progress = progress;
                    model.status = CommonUtils.getRemainingTimeText(averageRemainingTime);
                    model.speed = instantSpeed;
                    int position = receivers.indexOf(model);
                    adapter.notifyItemChanged(position, new ProgressViewModel.Field[]{
                            ProgressViewModel.Field.progress,
                            ProgressViewModel.Field.status,
                            ProgressViewModel.Field.speed,
                    });
                }
            });
        }

        @Override
        public void onStartComputeMD5(final FileTransfer fileTransfer) {
            runOnUiThread(() -> {
                if (isCreated()) {
                    model.progress = 100;
                    model.status = "Validating MD5";
                    model.state = ProgressViewModel.ControlButtonState.TRANSIT_DISABLED;
                    int position = receivers.indexOf(model);
                    adapter.notifyItemChanged(position, new ProgressViewModel.Field[]{
                            ProgressViewModel.Field.progress,
                            ProgressViewModel.Field.status,
                            ProgressViewModel.Field.state,
                    });
                }
            });
        }

        @Override
        public void onTransferSucceed(final FileTransfer fileTransfer) {
            runOnUiThread(() -> {
                if (isCreated()) {
                    showToast("Transfer succeed");

                    model.fileTransfer = fileTransfer;
                    model.status = "Transfer succeed";
                    model.statusColor = getResources().getColor(R.color.colorSuccess);
                    model.state = ProgressViewModel.ControlButtonState.DONE;
                    int position = receivers.indexOf(model);
                    adapter.notifyItemChanged(position, new ProgressViewModel.Field[]{
                            ProgressViewModel.Field.status,
                            ProgressViewModel.Field.state,
                    });

                    // 如果这是图片，则调用 Glide 显示图片
                    BitmapFactory.Options bitmapOpts = new BitmapFactory.Options();
                    bitmapOpts.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(fileTransfer.getFilePath(), bitmapOpts);
                    if (bitmapOpts.outHeight != -1 && bitmapOpts.outWidth != -1)
                        Glide.with(FileReceiverActivity.this).load(fileTransfer.getFilePath()).into(iv_image);
                    else
                        CommonUtils.openFileByPath(FileReceiverActivity.this, fileTransfer.getFilePath());
                }
            });
        }

        @Override
        public void onTransferFailed(final FileTransfer fileTransfer, final Exception e) {
            runOnUiThread(() -> {
                if (isCreated()) {
                    String statusText = "Transfer failed: " + e.getMessage();
                    showToast(statusText);
                    model.status = statusText;
                    model.statusColor = Color.RED;
                    model.state = model.state == ProgressViewModel.ControlButtonState.CONFIRM
                            ? ProgressViewModel.ControlButtonState.CONFIRM_DISABLED
                            : (model.state == ProgressViewModel.ControlButtonState.TRANSIT
                                ? ProgressViewModel.ControlButtonState.TRANSIT_DISABLED
                                : ProgressViewModel.ControlButtonState.PAUSED_DISABLED);
                    int position = receivers.indexOf(model);
                    adapter.notifyItemChanged(position, new ProgressViewModel.Field[]{
                            ProgressViewModel.Field.status,
                            ProgressViewModel.Field.state,
                    });
                }
            });
        }
    }

    private FileTransferRecorder recorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_receiver);
        initView();
        recorder = new FileTransferRecorder(this);
        FileReceiverService service = new FileReceiverService(new OnNewTransferListener());
        serviceFuture = ((CoreApplication) getApplication()).threadPool.submit(service);
    }

    private ImageView iv_image;
    private RecyclerView recyclerView;
    private ProgressViewAdapter adapter;

    private void initView() {
        setTitle("Receive Files");
        iv_image = findViewById(R.id.iv_image);
        TextView localIp = findViewById(R.id.tv_local_ip);
        localIp.setText(WifiLManager.getLocalIpAddress(this));

        // 初始化 RecyclerView
        recyclerView = findViewById(R.id.recycler);
        adapter = new ProgressViewAdapter(this, receivers, recyclerView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (ProgressViewModel model : receivers) {
            model.taskFuture.cancel(true);
        }
        if (serviceFuture != null) {
            Log.i(TAG, "正在取消接收端线程");
            serviceFuture.cancel(true);
        }
        recorder.close();
    }

}
