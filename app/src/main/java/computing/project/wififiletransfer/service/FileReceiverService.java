package computing.project.wififiletransfer.service;

import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import computing.project.wififiletransfer.common.Constants;
import computing.project.wififiletransfer.common.OnNewTransferListener;

public class FileReceiverService extends PauseableRunnable {

    static final String TAG = "FileReceiverService";

    private final OnNewTransferListener listener;
    private ServerSocket serverSocket;
    private Socket client;

    public FileReceiverService(OnNewTransferListener listener) {
        this.listener = listener;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.setSoTimeout(500);
            serverSocket.bind(new InetSocketAddress(Constants.PORT));
            Log.i(TAG, "正在监听端口：" + Constants.PORT);
            while (true) {
                try {
                    client = null;
                    while (!Thread.currentThread().isInterrupted() && client == null) {
                        try {
                            client = serverSocket.accept();
                        } catch (SocketTimeoutException ignored) {
                        }
                    }
                    if (Thread.currentThread().isInterrupted())
                        throw new InterruptedException();
                    Log.i(TAG, "发送者 IP 地址：" + client.getInetAddress().getHostAddress());

                    // 通知主线程有新的发送者
                    listener.onNewTransfer(client);
                } catch (InterruptedException e) {
                    // 主线程正常推出
                    Log.i(TAG, "停止监听");
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "网络异常：" + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanUp();
        }
    }

    private void cleanUp() {
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
