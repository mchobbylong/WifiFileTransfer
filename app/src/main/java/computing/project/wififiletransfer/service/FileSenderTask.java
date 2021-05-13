package computing.project.wififiletransfer.service;

import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.Socket;

import javax.crypto.SecretKey;

import computing.project.wififiletransfer.common.AESUtils;
import computing.project.wififiletransfer.common.Constants;
import computing.project.wififiletransfer.common.Md5Util;
import computing.project.wififiletransfer.common.OnTransferChangeListener;
import computing.project.wififiletransfer.common.SpeedMonitor;
import computing.project.wififiletransfer.model.FileTransfer;

public class FileSenderTask extends PauseableRunnable {

    static final String TAG = "FileSenderTask";

    private final FileTransfer fileTransfer;
    private final String ipAddress;
    private final OnTransferChangeListener listener;
    private Socket socket;
    private OutputStream outputStream;
    private ObjectOutputStream objectOutputStream;
    private InputStream inputStream;
    private ObjectInputStream objectInputStream;
    private RandomAccessFile fileInputStream;
    private SpeedMonitor monitor;

    public FileSenderTask(FileTransfer fileTransfer, String ipAddress, OnTransferChangeListener listener) {
        this.fileTransfer = fileTransfer;
        this.ipAddress = ipAddress;
        this.listener = listener;
    }


    @RequiresApi(Build.VERSION_CODES.O)
    @Override
    public void run() {
        // 计算 MD5
        if (TextUtils.isEmpty(fileTransfer.getMd5())) {
            Log.i(TAG, "开始计算 MD5");
            listener.onStartComputeMD5(fileTransfer);
            fileTransfer.setMd5(Md5Util.getMd5(new File(fileTransfer.getFilePath())));
            Log.i(TAG, "MD5 计算完毕");
        } else {
            Log.i(TAG, "无需计算 MD5");
        }

        try {
            socket = new Socket();
            socket.bind(null);
            socket.connect(new InetSocketAddress(ipAddress, Constants.PORT), 10000);
            outputStream = socket.getOutputStream();
            objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeObject(fileTransfer);

            // 等待接收端的回复
            listener.onReceiveFileTransfer(fileTransfer);

            // 接受来自接收端的 progress 值
            inputStream = socket.getInputStream();
            objectInputStream = new ObjectInputStream(inputStream);
            try {
                Long receivedProgress = (Long) objectInputStream.readObject();
                fileTransfer.setProgress(receivedProgress);
            } catch (EOFException e) {
                throw new Exception("Transfer is rejected by receiver");
            }

            // 使用 RandomAccessFile 类，并调用 .seek(progress) 指定续传的位置
            fileInputStream = new RandomAccessFile(new File(fileTransfer.getFilePath()), "r");
            fileInputStream.seek(fileTransfer.getProgress());

            listener.onStartTransfer(fileTransfer);
            monitor = new SpeedMonitor(fileTransfer, listener);
            monitor.start();
            byte[] buffer = new byte[Constants.TRANSFER_BUFFER_SIZE];
            int size;
            SecretKey key = AESUtils.generateAESKey("abcdefghijklmnopqrstuvwxyz012345");
            while ((!Thread.currentThread().isInterrupted()) && ((size = fileInputStream.read(buffer)) != -1)) {
                if (suspended) tryResume();
                byte[] cipherText = AESUtils.encrypt(buffer, 0, size, key);

                // 先将 cipherText 的长度发送给接收端，再发送 cipherText 的内容
                objectOutputStream.writeObject(cipherText.length);
                outputStream.write(cipherText);
                fileTransfer.setProgress(fileTransfer.getProgress() + size);
            }
            // 检查是不是传输被中断了
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
            // 发送结束信号给接收端
            objectOutputStream.writeObject((int)0);
            Log.i(TAG, "文件发送成功");
            monitor.stop();
            listener.onTransferSucceed(fileTransfer);
        } catch (InterruptedException e) {
            Log.i(TAG, "文件发送已中断");
            Thread.currentThread().interrupt();
            listener.onTransferFailed(fileTransfer, new Exception("Transfer is cancelled"));
        } catch (Exception e) {
            Log.e(TAG, "文件发送异常：" + e.getMessage());
            e.printStackTrace();
            listener.onTransferFailed(fileTransfer, e);
        } finally {
            cleanUp();
        }
    }

    private void cleanUp() {
        if (monitor != null) {
            monitor.stop();
        }
        closeStream(objectOutputStream);
        objectOutputStream = null;
        closeStream(outputStream);
        outputStream = null;
        closeStream(objectInputStream);
        objectInputStream = null;
        closeStream(inputStream);
        inputStream = null;
        closeStream(fileInputStream);
        fileInputStream = null;
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
                socket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
