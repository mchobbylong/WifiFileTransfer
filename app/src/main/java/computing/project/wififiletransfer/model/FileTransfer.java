package computing.project.wififiletransfer.model;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.Serializable;

public class FileTransfer implements Serializable {

    // 文件名
    private String fileName = "";

    // 文件路径
    private String filePath = "";

    // 文件大小
    private long fileSize = 0;

    // MD5码
    private String md5 = "";

    // 当前传输进度
    private long progress = 0;

    // 发送方用户名
    private String senderName = "";

    public FileTransfer() {}

    public FileTransfer(File file) {
        this.fileName = file.getName();
        this.filePath = file.getPath();
        this.fileSize = file.length();
        this.progress = 0;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getFileSize() {
        return fileSize;
    }

    @SuppressLint("DefaultLocale")
    public String getFileSizeText() {
        if (fileSize >= 1024 * 1024 * 1024)
            return String.format("%.1fGB", fileSize / 1024.0 / 1024 / 1024);
        if (fileSize >= 1024 * 1024)
            return String.format("%.1fMB", fileSize / 1024.0 / 1024);
        if (fileSize >= 1024)
            return String.format("%.1fKB", fileSize / 1024.0);
        return fileSize + "B";
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public long getProgress() { return progress; }

    public void setProgress(long progress) { this.progress = progress; }

    public String getSenderName() { return senderName; }

    public void setSenderName(String name) { this.senderName = name; }

    @NonNull
    @Override
    public String toString() {
        return "FileTransfer{" +
                "fileName='" + fileName + '\'' +
                ", filePath='" + filePath + '\'' +
                ", fileSize=" + fileSize +
                ", md5='" + md5 + '\'' +
                '}';
    }

}
