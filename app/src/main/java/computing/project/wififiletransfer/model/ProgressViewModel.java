package computing.project.wififiletransfer.model;


import android.content.Context;

import androidx.annotation.NonNull;

import java.util.concurrent.Future;

import computing.project.wififiletransfer.service.PauseableRunnable;

public class ProgressViewModel {

    public enum ControlButtonState {
        CONFIRM, CONFIRM_DISABLED, TRANSIT, TRANSIT_DISABLED, PAUSED, PAUSED_DISABLED, DONE
    }

    public PauseableRunnable task = null;
    public Future taskFuture = null;

    public FileTransfer fileTransfer = new FileTransfer();
    public int progress = 0;
    public double speed = 0;
    public String status = "";
    public int statusColor;
    public ControlButtonState state = ControlButtonState.TRANSIT;

    public enum Field {
        filename, size, status, speed, progress, state
    }

    public static Field[] ALL_FIELDS = new Field[]{ Field.filename, Field.size, Field.status, Field.speed, Field.progress, Field.state };

    public ProgressViewModel(@NonNull Context context) {
        statusColor = context.getResources().getColor(android.R.color.tab_indicator_text);
    }

}
