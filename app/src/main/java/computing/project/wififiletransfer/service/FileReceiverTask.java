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
import java.io.DataInputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;

import javax.crypto.SecretKey;

import computing.project.wififiletransfer.common.AESUtils;
import computing.project.wififiletransfer.common.Curve25519Helper;
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
    private DataInputStream dataInputStream;
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
            outputStream = client.getOutputStream();

            // 先发送自己的 DH 公钥给发送端
            Curve25519Helper dh = new Curve25519Helper();
            outputStream.write(dh.getPublicKey());
            // 然后接收发送端的 DH 公钥
            inputStream = client.getInputStream();
            dataInputStream = new DataInputStream(inputStream);
            byte[] clientPublicKey = new byte[Curve25519Helper.KEY_BYTE_SIZE];
            dataInputStream.readFully(clientPublicKey);
            // 计算 Shared secret 并作为 AES key
            SecretKey aesKey = AESUtils.generateAESKey(dh.getSharedSecret(clientPublicKey));

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
            objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeObject(fileTransfer.getProgress());

            // 恢复续传进度
            file = new File(fileTransfer.getFilePath());
            fileOutputStream = new RandomAccessFile(file, "rwd");
            fileOutputStream.seek(fileTransfer.getProgress());

            // 开始传输
            listener.onStartTransfer(fileTransfer);
            monitor = new SpeedMonitor(fileTransfer, listener);
            monitor.start();
            int size, received;
            Integer cipherTextSize = (Integer) objectInputStream.readObject();
            while (cipherTextSize > 0) {
                // Log.d(TAG, "Current cipherTextSize: " + cipherTextSize);
                byte[] buffer = new byte[cipherTextSize];
                // 接收指定量的字节作为 cipherText
                size = 0;
                while (!Thread.currentThread().isInterrupted() && size < cipherTextSize && (received = inputStream.read(buffer, size, cipherTextSize - size)) != -1) {
                    if (suspended) tryResume();
                    // Log.d(TAG, "Buffer received: " + received);
                    size += received;
                }
                if (Thread.currentThread().isInterrupted())
                    throw new InterruptedException("File transfer is cancelled");
                if (size < cipherTextSize)
                    throw new Exception("File transfer is cancelled by sender");
                byte[] plainContent = AESUtils.decrypt(buffer, cipherTextSize, aesKey);
                fileOutputStream.write(plainContent);
                fileTransfer.setProgress(fileTransfer.getProgress() + plainContent.length);
                recorder.update(fileTransfer);
                // 接收新的 cipherTextSize
                cipherTextSize = (Integer) objectInputStream.readObject();
            }
            if (fileTransfer.getProgress() < fileTransfer.getFileSize()) {
                throw new Exception("File transfer is cancelled by sender");
            }
            Log.i(TAG, "文件接收成功");
            monitor.stop();
            recorder.delete(fileTransfer.getMd5());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            exception = e;
        } catch (Exception e) {
            exception = e;
            e.printStackTrace();
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
        closeStream(dataInputStream);
        closeStream(inputStream);
        closeStream(objectOutputStream);
        closeStream(outputStream);
        closeStream(fileOutputStream);
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
