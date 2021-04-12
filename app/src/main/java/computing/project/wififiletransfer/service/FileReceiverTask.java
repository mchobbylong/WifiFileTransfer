package computing.project.wififiletransfer.service;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import computing.project.wififiletransfer.common.Constants;
import computing.project.wififiletransfer.common.Md5Util;
import computing.project.wififiletransfer.common.OnTransferChangeListener;
import computing.project.wififiletransfer.common.SpeedMonitor;
import computing.project.wififiletransfer.model.FileTransfer;
import computing.project.wififiletransfer.model.FileTransferRecorder;

public class FileReceiverTask implements Runnable {
    private static final String TAG = "FileReceiverTask";

    // 暂停功能相关
    private final Object state = new Object();
    private volatile boolean suspended = false;

    private final OnTransferChangeListener listener;
    private final Context context;
    private FileTransfer fileTransfer;
    private ServerSocket serverSocket;
    private Socket client;
    private InputStream inputStream;
    private OutputStream outputStream;
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;
    private RandomAccessFile fileOutputStream;
    private SpeedMonitor monitor;
    private FileTransferRecorder recorder;

    public FileReceiverTask(Context context, OnTransferChangeListener listener) {
        this.listener = listener;
        this.context = context;
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

    public boolean isSuspended() { return suspended; }

    @Override
    public void run() {
        File file = null;
        Exception exception = null;
        try {
            recorder = new FileTransferRecorder(context);
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
                    if (fileTransfer == null)
                        throw new Exception("发送端传来的信息有误");

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
                        if (!suspended) {
                            fileOutputStream.write(buffer, 0, size);
                            fileTransfer.setProgress(fileTransfer.getProgress() + size);
                            recorder.update(fileTransfer);
                        } else {
                            synchronized (state) {
                                while (suspended) state.wait();
                            }
                        }
                    }
                    if (Thread.currentThread().isInterrupted())
                        throw new InterruptedException("文件传输被中断");
                    if (fileTransfer.getProgress() < fileTransfer.getFileSize())
                        throw new Exception("传输被发送端中断");
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
                        if (client == null) {
                            Log.i(TAG, "接收端正常退出");
                            break;
                        }
                        if (fileTransfer != null)
                            fileTransfer.setMd5("None");
                        listener.onTransferFailed(fileTransfer, exception);
                        exception = null;
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
                            listener.onTransferFailed(fileTransfer, new Exception("MD5 不一致"));
                    }
                    cleanUpClient();
                    file = null;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "绑定端口错误：" + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanUpAll();
        }
    }

    private void closeStream(Closeable s) {
        if (s != null) {
            try {
                s.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void cleanUpClient() {
        fileTransfer = null;
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
        if (recorder != null) {
            recorder.close();
        }
    }
}
