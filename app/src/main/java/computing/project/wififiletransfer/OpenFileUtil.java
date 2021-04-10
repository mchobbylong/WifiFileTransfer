package computing.project.wififiletransfer;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.StrictMode;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import java.io.File;

public class OpenFileUtil {
    /**
     * 根据路径打开文件
     * @param context 上下文
     * @param path 文件路径
     */
    public static void openFileByPath(Context context, String path) {
        if(context==null||path==null)
            return;
        Intent intent = new Intent();
        //设置intent的Action属性
        intent.setAction(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory(Intent.CATEGORY_DEFAULT);

        //文件的类型
        String type = "";
        String extension = MimeTypeMap.getFileExtensionFromUrl(path);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        try {
            File out = new File(path);
            Uri fileURI;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // 由于7.0以后文件访问权限，可以通过定义xml在androidmanifest中申请，也可以直接跳过权限
                // 通过定义xml在androidmanifest中申请
//                fileURI = FileProvider.getUriForFile(context,
//                        "com.lonelypluto.zyw_test.provider",
//                        out);
                // 直接跳过权限
                StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                StrictMode.setVmPolicy(builder.build());
                fileURI = Uri.fromFile(out);
            }else{
                fileURI = Uri.fromFile(out);
            }
            //设置intent的data和Type属性
            intent.setDataAndType(fileURI, type);
            //跳转
            if (context.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                context.startActivity(intent);
            } else {
                Toast.makeText(context, "没有找到对应的程序", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) { //当系统没有携带文件打开软件，提示
            Toast.makeText(context, "无法打开该格式文件", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}

