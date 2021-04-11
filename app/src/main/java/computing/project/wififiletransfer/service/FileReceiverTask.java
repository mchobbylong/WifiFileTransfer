package computing.project.wififiletransfer.service;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import computing.project.wififiletransfer.common.Constants;
import computing.project.wififiletransfer.common.Md5Util;
import computing.project.wififiletransfer.common.OnTransferChangeListener;
import computing.project.wififiletransfer.common.SpeedMonitor;
import computing.project.wififiletransfer.model.FileTransfer;

public class FileReceiverTask implements Runnable {
    private static final String TAG = "FileReceiverTask";

    // 暂停功能相关
    Object state = new Object();
    private volatile boolean suspended = false;

    private final OnTransferChangeListener listener;
    private FileTransfer fileTransfer;
    private ServerSocket serverSocket;
    private Socket client;
    private InputStream inputStream;
    private ObjectInputStream objectInputStream;
    private FileOutputStream fileOutputStream;
    private SpeedMonitor monitor;

    public FileReceiverTask(OnTransferChangeListener listener) {
        this.listener = listener;
    }

    public void suspend() {
        suspended = true;
        Log.i(TAG, "接收文件 " + fileTransfer.getFileName() + " 已暂停");
    }

    public void resume() {
        suspended = false;
        Log.i(TAG, "接收文件 " + fileTransfer.getFileName() + " 已继续");
        synchronized (state) {
            state.notifyAll();
        }
    }

    @Override
    public void run() {
        File file = null;
        Exception exception = null;
        try {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.setSoTimeout(1000);
            serverSocket.bind(new InetSocketAddress(Constants.PORT));
            Log.i(TAG, "正在监听端口：" + Constants.PORT);
            while (true) {
                try {
                    client = null;
                    while (!Thread.currentThread().isInterrupted() && client == null) {
                        try {
                            client = serverSocket.accept();
                        } catch (SocketTimeoutException e) {
                        }
                    }
                    if (Thread.currentThread().isInterrupted())
                        throw new InterruptedException();
                    Log.i(TAG, "客户端 IP 地址：" + client.getInetAddress().getHostAddress());
                    inputStream = client.getInputStream();
                    objectInputStream = new ObjectInputStream(inputStream);
                    fileTransfer = (FileTransfer) objectInputStream.readObject();
                    Log.i(TAG, "待接收的文件：" + fileTransfer);
                    if (fileTransfer == null) {
                        exception = new Exception("发送端传来的信息有误");
                    }

                    // TODO: 判断以前有没有传过相同的文件（用fileTransfer.md5）
                    //     如果以前传过，从记录里获取上次成功传输的位置（fileTransfer.progress）
                    //     如果没有传过，设置 fileTransfer.progress = 0

                    // TODO: 将 progress 通过 socket 回传给发送端

                    String filename = fileTransfer.getFileName();
                    file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + filename);
                    fileOutputStream = new FileOutputStream(file);

                    monitor = new SpeedMonitor(fileTransfer, listener);
                    monitor.start();
                    byte[] buffer = new byte[Constants.TRANSFER_BUFFER_SIZE];
                    int size;
                    while (!Thread.currentThread().isInterrupted() && (size = inputStream.read(buffer)) != -1) {
                        if (!suspended) {
                            fileOutputStream.write(buffer, 0, size);
                            fileTransfer.setProgress(fileTransfer.getProgress() + size);
                        } else {
                            while (suspended)
                                synchronized (state) {
                                    state.wait();
                                }
                        }
                    }
                    if (Thread.currentThread().isInterrupted() || fileTransfer.getProgress() < fileTransfer.getFileSize())
                        throw new InterruptedException("文件传输被中断");
                    Log.i(TAG, "文件接收成功");
                    monitor.stop();
                    fileTransfer = null;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    exception = e;
                } catch (Exception e) {
                    exception = e;
                } finally {
                    if (fileTransfer == null) {
                        Log.i(TAG, "接收端正常退出");
                        break;
                    }
                    if (exception != null) {
                        listener.onTransferFailed(fileTransfer, exception);
                        break;
                    }

                    // 正常完成传输，校验 MD5
                    listener.onStartComputeMD5();
                    Log.i(TAG, "开始计算 MD5");
                    String md5 = Md5Util.getMd5(file);
                    Log.i(TAG, "计算出来的 MD5 为：" + md5);
                    if (md5.equals(fileTransfer.getMd5()))
                        listener.onTransferSucceed(fileTransfer);
                    else
                        listener.onTransferFailed(fileTransfer, new Exception("MD5 不一致"));
                    cleanUpClient();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "绑定端口错误：" + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanUpAll();
        }
    }

    private void cleanUpClient() {
        if (monitor != null) {
            monitor.stop();
        }
        if (objectInputStream != null) {
            try {
                objectInputStream.close();
                objectInputStream = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (inputStream != null) {
            try {
                inputStream.close();
                inputStream = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (fileOutputStream != null) {
            try {
                fileOutputStream.close();
                fileOutputStream = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (client != null && !client.isClosed()) {
            try {
                client.close();
                client = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void cleanUpAll() {
        cleanUpClient();
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                serverSocket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
