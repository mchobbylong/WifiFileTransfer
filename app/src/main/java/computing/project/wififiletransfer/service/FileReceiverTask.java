package computing.project.wififiletransfer.service;

import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;

import computing.project.wififiletransfer.common.AESUtils;
import computing.project.wififiletransfer.common.Constants;
import computing.project.wififiletransfer.common.Md5Util;
import computing.project.wififiletransfer.common.OnTransferChangeListener;
import computing.project.wififiletransfer.common.SpeedMonitor;
import computing.project.wififiletransfer.model.FileTransfer;
import computing.project.wififiletransfer.model.FileTransferRecorder;

public class FileReceiverTask extends PauseableRunnable {

    static final String TAG = "FileReceiverTask";

    private final OnTransferChangeListener listener;
    private FileTransfer fileTransfer;
    private Socket client;
    private InputStream inputStream;
    private OutputStream outputStream;
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;
    private RandomAccessFile fileOutputStream;
    private SpeedMonitor monitor;
    private FileTransferRecorder recorder;

    public FileReceiverTask(Socket client, OnTransferChangeListener listener, FileTransferRecorder recorder) {
        this.client = client;
        this.listener = listener;
        this.recorder = recorder;
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Override
    public void run() {
        File file = null;
        Exception exception = null;
        try {
            inputStream = client.getInputStream();
            objectInputStream = new ObjectInputStream(inputStream);
            fileTransfer = (FileTransfer) objectInputStream.readObject();
            Log.i(TAG, "待接收的文件：" + fileTransfer);
            if (fileTransfer == null)
                throw new Exception("Information received from sender is damaged");

            // 等待用户同意/拒绝，暂停自己
            listener.onReceiveFileTransfer(fileTransfer);
            suspend();
            try {
                tryResume();
            } catch (InterruptedException e) {
                client = null;
                throw new InterruptedException("Transfer is rejected");
            }

            // 判断以前有没有传过相同的文件（用fileTransfer.md5）
            //     如果以前传过，从记录里获取上次成功传输的位置（fileTransfer.progress）和路径
            //     如果没有传过，设置 fileTransfer.progress = 0 并初始化路径
            FileTransfer lastTransfer = recorder.query(fileTransfer.getMd5());
            if (lastTransfer == null) {
                fileTransfer.setProgress(0);
                String filename = fileTransfer.getFileName();
                fileTransfer.setFilePath(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + filename);
            } else {
                fileTransfer = lastTransfer;
            }

            // 将 progress 通过 socket 回传给发送端
            outputStream = client.getOutputStream();
            objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeObject(fileTransfer.getProgress());

            file = new File(fileTransfer.getFilePath());
            fileOutputStream = new RandomAccessFile(file, "rwd");
            fileOutputStream.seek(fileTransfer.getProgress());

            listener.onStartTransfer(fileTransfer);
            monitor = new SpeedMonitor(fileTransfer, listener);
            monitor.start();

            byte[] buffer = new byte[Constants.TRANSFER_BUFFER_SIZE];
            int size;
            while (!Thread.currentThread().isInterrupted() && (size = inputStream.read(buffer)) != -1) {
                if (suspended) tryResume();
                byte[] plainText = AESUtils.decrypt(buffer, AESUtils.getpublicKey(), AESUtils.generateIv());
                fileOutputStream.write(plainText, 0, plainText.length);
                fileTransfer.setProgress(fileTransfer.getProgress() + size);
                recorder.update(fileTransfer);
            }
            if (Thread.currentThread().isInterrupted())
                throw new InterruptedException("File transfer is cancelled");
            if (fileTransfer.getProgress() < fileTransfer.getFileSize())
                throw new Exception("File transfer is cancelled by sender");
            Log.i(TAG, "文件接收成功");
            monitor.stop();
            recorder.delete(fileTransfer.getMd5());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            exception = e;
        } catch (Exception e) {
            exception = e;
        } finally {
            if (exception != null) {
                if (fileTransfer != null)
                    fileTransfer.setMd5("None");
                listener.onTransferFailed(fileTransfer, exception);
            } else {
                // 正常完成传输，校验 MD5
                listener.onStartComputeMD5(fileTransfer);
                Log.i(TAG, "开始计算 MD5");
                String oldMd5 = fileTransfer.getMd5();
                fileTransfer.setMd5(Md5Util.getMd5(file));
                Log.i(TAG, "计算出来的 MD5 为：" + fileTransfer.getMd5());

                if (oldMd5.equals(fileTransfer.getMd5()))
                    listener.onTransferSucceed(fileTransfer);
                else
                    listener.onTransferFailed(fileTransfer, new Exception("MD5 is inconsistent"));
            }
            cleanUp();
        }
    }

    private void cleanUp() {
        if (monitor != null) {
            monitor.stop();
        }
        closeStream(objectInputStream);
        objectInputStream = null;
        closeStream(inputStream);
        inputStream = null;
        closeStream(objectOutputStream);
        objectOutputStream = null;
        closeStream(outputStream);
        outputStream = null;
        closeStream(fileOutputStream);
        fileOutputStream = null;
        if (client != null && !client.isClosed()) {
            try {
                client.close();
                client = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
