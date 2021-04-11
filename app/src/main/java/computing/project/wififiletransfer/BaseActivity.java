package computing.project.wififiletransfer;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

@SuppressLint("Registered")
public class BaseActivity extends AppCompatActivity {

    protected void setTitle(String title) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(title);
        }
    }

    protected void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    protected void bindService(Class<? extends Service> service, ServiceConnection serviceConnection) {
        bindService(new Intent(this, service), serviceConnection, Context.BIND_AUTO_CREATE);
    }

    protected void startActivity(Class c) {
        startActivity(new Intent(this, c));
    }

    protected boolean isCreated() {
        return !isFinishing() && !isDestroyed();
    }

}