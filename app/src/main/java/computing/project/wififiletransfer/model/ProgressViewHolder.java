package computing.project.wififiletransfer.model;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import computing.project.wififiletransfer.R;
import computing.project.wififiletransfer.common.CommonUtils;

public class ProgressViewHolder extends RecyclerView.ViewHolder {

    protected Context context;
    public TextView progressText;
    public ProgressBar progressBar;
    public TextView filename;
    public TextView size;
    public TextView status;
    public TextView speed;
    public Button buttonSuspend;
    public Button buttonInterrupt;
    public Button buttonOpenFile;
    public Button buttonAccept;
    public Button buttonReject;

    public ProgressViewHolder(@NonNull View itemView, Context context) {
        super(itemView);
        this.context = context;
        progressText = itemView.findViewById(R.id.percent);
        progressBar = itemView.findViewById(R.id.progress_bar);
        progressBar.setMax(100);
        filename = itemView.findViewById(R.id.filename);
        size = itemView.findViewById(R.id.size);
        status = itemView.findViewById(R.id.status);
        speed = itemView.findViewById(R.id.speed);
        buttonSuspend = itemView.findViewById(R.id.bn_toggle_suspension);
        buttonInterrupt = itemView.findViewById(R.id.bn_interrupt);
        buttonOpenFile = itemView.findViewById(R.id.bn_open_file);
        buttonAccept = itemView.findViewById(R.id.bn_accept);
        buttonReject = itemView.findViewById(R.id.bn_reject);
    }

    public void onChange(ProgressViewModel model, ProgressViewModel.Field[] fields) {
        if (fields == null) fields = ProgressViewModel.ALL_FIELDS;
        for (ProgressViewModel.Field field : fields) {
            switch (field) {
                case filename:
                    filename.setText(model.fileTransfer.getFileName());
                    break;
                case size:
                    size.setText(model.fileTransfer.getFileSizeText());
                    break;
                case status:
                    status.setText(model.status);
                    status.setTextColor(model.statusColor);
                    break;
                case speed:
                    speed.setText(CommonUtils.getSpeedText(model.speed));
                    break;
                case progress:
                    progressText.setText(String.valueOf(model.progress));
                    progressBar.setProgress(model.progress);
                    break;

                case state:
                    boolean enabled;
                    switch (model.state) {
                        case CONFIRM:
                        case CONFIRM_DISABLED:
                            buttonSuspend.setVisibility(View.GONE);
                            buttonInterrupt.setVisibility(View.GONE);
                            buttonOpenFile.setVisibility(View.GONE);
                            buttonAccept.setVisibility(View.VISIBLE);
                            buttonReject.setVisibility(View.VISIBLE);

                            enabled = model.state == ProgressViewModel.ControlButtonState.CONFIRM;
                            buttonAccept.setEnabled(enabled);
                            buttonAccept.setBackground(ResourcesCompat.getDrawable(
                                    context.getResources(),
                                    enabled ? R.drawable.ic_ok : R.drawable.ic_ok_disabled,
                                    null));
                            buttonReject.setEnabled(enabled);
                            buttonReject.setBackground(ResourcesCompat.getDrawable(
                                    context.getResources(),
                                    enabled ? R.drawable.ic_cancel : R.drawable.ic_cancel_disabled,
                                    null));
                            break;

                        case TRANSIT:
                        case TRANSIT_DISABLED:
                        case PAUSED:
                        case PAUSED_DISABLED:
                            buttonOpenFile.setVisibility(View.GONE);
                            buttonAccept.setVisibility(View.GONE);
                            buttonReject.setVisibility(View.GONE);
                            buttonSuspend.setVisibility(View.VISIBLE);
                            buttonInterrupt.setVisibility(View.VISIBLE);

                            enabled = model.state == ProgressViewModel.ControlButtonState.TRANSIT ||
                                    model.state == ProgressViewModel.ControlButtonState.PAUSED;
                            int suspendIcon = enabled
                                    ? (model.state == ProgressViewModel.ControlButtonState.TRANSIT ? R.drawable.ic_suspend : R.drawable.ic_play)
                                    : (model.state == ProgressViewModel.ControlButtonState.TRANSIT_DISABLED ? R.drawable.ic_suspend_disabled : R.drawable.ic_play_disabled);
                            buttonSuspend.setEnabled(enabled);
                            buttonSuspend.setBackground(ResourcesCompat.getDrawable(context.getResources(), suspendIcon, null));
                            buttonInterrupt.setEnabled(enabled);
                            buttonInterrupt.setBackground(ResourcesCompat.getDrawable(
                                    context.getResources(),
                                    enabled ? R.drawable.ic_delete : R.drawable.ic_delete_disabled,
                                    null));
                            break;

                        case DONE:
                            buttonAccept.setVisibility(View.GONE);
                            buttonReject.setVisibility(View.GONE);
                            buttonSuspend.setVisibility(View.GONE);
                            buttonInterrupt.setVisibility(View.GONE);
                            buttonOpenFile.setVisibility(View.VISIBLE);
                            break;
                    }
                    break;
            }
        }
    }
}
