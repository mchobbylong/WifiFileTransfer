package computing.project.wififiletransfer.common;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;

import computing.project.wififiletransfer.BuildConfig;

public class CommonUtils {
    public static final String[] remainingTimeUnitText = new String[]{
            "hour(s)",
            "minute(s)",
            "second(s)",
    };
    public static final long[] remainingTimeUnitBound = new long[]{ 3600, 60, 0 };

    public static String getRemainingTimeText(long remainingTime) {
        for (int i = 0; i < remainingTimeUnitText.length; ++i)
            if (remainingTime >= remainingTimeUnitBound[i]) {
                if (remainingTimeUnitBound[i] > 0)
                    remainingTime /= remainingTimeUnitBound[i];
                return remainingTime + " " + remainingTimeUnitText[i];
            }
        return "Error";
    }

    @SuppressLint("DefaultLocale")
    public static String getSpeedText(double speed) {
        if (speed > 1024.0) return String.format("%dMB/s", (long) speed / 1024);
        else return String.format("%dKB/s", (long) speed);
    }

    public static void openFileByPath(Context context, String path) {
        if (context == null || path == null)
            return;
        Intent intent = new Intent();
        // 设置 Intent 的 Action 属性
        intent.setAction(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory(Intent.CATEGORY_DEFAULT);

        // 文件的类型
        String type = "";
        String extension = MimeTypeMap.getFileExtensionFromUrl(path);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        try {
            File out = new File(path);
            Uri fileURI;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                fileURI = FileProvider.getUriForFile(context,
                        BuildConfig.APPLICATION_ID + ".fileprovider",
                        out);
            } else {
                fileURI = Uri.fromFile(out);
            }
            // 设置 Intent 的 Data 和 Type 属性
            intent.setDataAndType(fileURI, type);
            // 跳转
            if (context.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                context.startActivity(intent);
            } else {
                Toast.makeText(context, "Cannot open this file due to lack of app that can open it", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) { // 当系统没有携带文件打开软件，提示
            Toast.makeText(context, "Error when trying to open this file", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}
