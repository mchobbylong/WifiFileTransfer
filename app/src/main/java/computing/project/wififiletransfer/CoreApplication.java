package computing.project.wififiletransfer;

import android.app.Application;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CoreApplication extends Application {
    public final ExecutorService threadPool = Executors.newFixedThreadPool(4);
}
