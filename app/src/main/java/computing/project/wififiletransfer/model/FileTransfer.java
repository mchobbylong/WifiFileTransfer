package computing.project.wififiletransfer.model;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.Serializable;

public class FileTransfer implements Serializable, Cloneable {

    //文件名
    private String fileName;

    //文件路径
    private String filePath;

    //文件大小
    private long fileSize;

    //MD5码
    private String md5;

    //当前传输进度
    private long progress;

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

    @NonNull
    @Override
    public FileTransfer clone() {
        FileTransfer ret = new FileTransfer();
        ret.setFileName(fileName);
        ret.setFileSize(fileSize);
        ret.setFilePath(filePath);
        ret.setMd5(md5);
        ret.setProgress(progress);
        return ret;
    }
}
