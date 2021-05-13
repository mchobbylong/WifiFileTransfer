package computing.project.wififiletransfer;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;


public class MainActivity extends BaseActivity {

    private static final int CODE_REQ_PERMISSIONS = 665;
    private EditText username;
    private String name;
    private SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        username = findViewById(R.id.et_username);
        sp = PreferenceManager.getDefaultSharedPreferences(this);

        // 从SharedPreference中检索用户名
        name = sp.getString("username", null);
        if (name == null) {
            name = "user" + System.currentTimeMillis();
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("username", name);
            editor.apply();
        }
        username.setText(name);

        // 添加焦点监听
        username.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus){ // 失去焦点
                    if(TextUtils.isEmpty(((EditText)v).getText().toString().trim())){ // 输入框为空，恢复原用户名
                        username.setText(name);
                    }
                    else{ // 新用户名，更新SharedPreference
                        name = username.getText().toString();
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString("username", name);
                        editor.apply();
                    }
                }
            }
        });

        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] permissions = packageInfo.requestedPermissions;
            ActivityCompat.requestPermissions(this, permissions, CODE_REQ_PERMISSIONS);
        } catch (Exception ignored) {}
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (isShouldHideInput(v, ev)) {

                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
            return super.dispatchTouchEvent(ev);
        }
        // 必不可少，否则所有的组件都不会有TouchEvent了
        if (getWindow().superDispatchTouchEvent(ev)) {
            return true;
        }
        return onTouchEvent(ev);
    }

    public boolean isShouldHideInput(View v, MotionEvent event) {
        if (v != null && (v instanceof EditText)) {
            int[] leftTop = {0, 0};
            //获取输入框当前的location位置
            v.getLocationInWindow(leftTop);
            int left = leftTop[0];
            int top = leftTop[1];
            int bottom = top + v.getHeight();
            int right = left + v.getWidth();
            if (event.getX() > left && event.getX() < right
                    && event.getY() > top && event.getY() < bottom) {
                // 点击的是输入框区域，保留点击EditText的事件
                return false;
            } else {
                //使EditText触发一次失去焦点事件
                v.setFocusable(false);
//                v.setFocusable(true); //这里不需要是因为下面一句代码会同时实现这个功能
                v.setFocusableInTouchMode(true);
                return true;
            }
        }
        return false;
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CODE_REQ_PERMISSIONS) {
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    showToast("Rejected while requesting permissions. Please grant permissions first");
                    return;
                }
            }
        }
    }

    public void startFileSenderActivity(View ignored) {
        startActivity(FileSenderActivity.class);
    }

    public void startFileReceiverActivity(View ignored) {
        startActivity(FileReceiverActivity.class);
    }

}
