package computing.project.wififiletransfer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

public class MainActivity extends BaseActivity {

    private static final int CODE_REQ_PERMISSIONS = 665;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.ACCESS_NETWORK_STATE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, CODE_REQ_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CODE_REQ_PERMISSIONS) {
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    showToast("Permission is missing. Please grant permission first");
                    return;
                }
            }
            showToast("Permission granted");
        }
    }

    public void startFileSenderActivity(View view) {
        startActivity(FileSenderActivity.class);
    }

    public void startFileReceiverActivity(View view) {
        startActivity(FileReceiverActivity.class);
    }

}
