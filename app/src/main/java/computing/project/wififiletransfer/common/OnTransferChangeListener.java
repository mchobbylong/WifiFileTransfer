package computing.project.wififiletransfer.common;

import computing.project.wififiletransfer.model.FileTransfer;

public interface OnTransferChangeListener {

    /**
     * 如果待发送的文件还没计算MD5码，则在开始计算MD5码时回调
     */
    void onStartComputeMD5();

    /**
     * 当传输进度发生变化时回调
     *
     * @param fileTransfer         待发送的文件模型
     * @param totalTime            传输到现在所用的时间
     * @param progress             文件传输进度
     * @param instantSpeed         瞬时-文件传输速率
     * @param instantRemainingTime 瞬时-预估的剩余完成时间
     * @param averageSpeed         平均-文件传输速率
     * @param averageRemainingTime 平均-预估的剩余完成时间
     */
    void onProgressChanged(FileTransfer fileTransfer, long totalTime, int progress, double instantSpeed, long instantRemainingTime, double averageSpeed, long averageRemainingTime);

    /**
     * 当文件传输成功时回调
     *
     * @param fileTransfer FileTransfer
     */
    void onTransferSucceed(FileTransfer fileTransfer);

    /**
     * 当文件传输失败时回调
     *
     * @param fileTransfer FileTransfer
     * @param e            Exception
     */
    void onTransferFailed(FileTransfer fileTransfer, Exception e);

}
