package computing.project.wififiletransfer.common;

import java.net.Socket;

public interface OnNewTransferListener {
    void onNewTransfer(Socket client);
}
