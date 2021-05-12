package computing.project.wififiletransfer.model;

import android.content.Context;
import android.graphics.Rect;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import computing.project.wififiletransfer.R;
import computing.project.wififiletransfer.common.CommonUtils;
import computing.project.wififiletransfer.common.DividerItemDecoration;

public class ProgressViewAdapter extends RecyclerView.Adapter<ProgressViewHolder> {

    private static final String TAG = "ProgressViewAdapter";

    protected Context context;
    protected RecyclerView recyclerView;
    public List<ProgressViewModel> models;

    public ProgressViewAdapter(Context context, List<ProgressViewModel> models, RecyclerView recyclerView) {
        super();
        this.context = context;
        this.models = models;
        this.recyclerView = recyclerView;
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(this);

        int marginVertical = CommonUtils.dpToPixel(context, 8);
        Rect dividerMargin = new Rect(0, marginVertical, 0, marginVertical);
        DividerItemDecoration divider = new DividerItemDecoration(context, layoutManager.getOrientation(), false, dividerMargin);
        recyclerView.addItemDecoration(divider);
    }

    @NonNull
    @Override
    public ProgressViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View progressView = LayoutInflater.from(context).inflate(R.layout.progress_view, parent, false);
        ProgressViewHolder holder = new ProgressViewHolder(progressView, context);
        holder.buttonAccept.setOnClickListener(new OnButtonAcceptClickListener(holder));
        holder.buttonReject.setOnClickListener(new OnButtonInterruptClickListener(holder));
        holder.buttonSuspend.setOnClickListener(new OnButtonSuspendClickListener(holder));
        holder.buttonInterrupt.setOnClickListener(new OnButtonInterruptClickListener(holder));
        holder.buttonOpenFile.setOnClickListener(new OnButtonOpenFileClickListener(holder));
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ProgressViewHolder holder, int position) {
        ProgressViewModel model = models.get(position);
        holder.onChange(model, null);
    }

    @Override
    public void onBindViewHolder(@NonNull ProgressViewHolder holder, int position, @NonNull List<Object> payloads) {
        ProgressViewModel model = models.get(position);
        if (payloads.size() == 0) {
            holder.onChange(model, null);
        } else {
            for (Object payload : payloads) {
                holder.onChange(model, (ProgressViewModel.Field[]) payload);
            }
        }
    }

    @Override
    public int getItemCount() { return models.size(); }

    protected class OnButtonAcceptClickListener implements View.OnClickListener {
        ProgressViewHolder holder;

        public OnButtonAcceptClickListener(ProgressViewHolder holder) {
            this.holder = holder;
        }

        @Override
        public void onClick(View view) {
            int position = holder.getBindingAdapterPosition();
            if (position == RecyclerView.NO_POSITION) return;
            ProgressViewModel model = models.get(position);
            model.task.resume();
        }
    }

    protected class OnButtonInterruptClickListener implements View.OnClickListener {
        ProgressViewHolder holder;

        public OnButtonInterruptClickListener(ProgressViewHolder holder) {
            this.holder = holder;
        }

        @Override
        public void onClick(View view) {
            int position = holder.getBindingAdapterPosition();
            if (position == RecyclerView.NO_POSITION) return;
            ProgressViewModel model = models.get(position);
            model.taskFuture.cancel(true);
        }
    }

    protected class OnButtonSuspendClickListener implements View.OnClickListener {
        ProgressViewHolder holder;

        public OnButtonSuspendClickListener(ProgressViewHolder holder) {
            this.holder = holder;
        }

        @Override
        public void onClick(View view) {
            int position = holder.getBindingAdapterPosition();
            if (position == RecyclerView.NO_POSITION) return;
            ProgressViewModel model = models.get(position);
            if (model.task.isSuspended()) {
                model.task.resume();
                model.state = ProgressViewModel.ControlButtonState.TRANSIT;
            } else {
                model.task.suspend();
                model.state = ProgressViewModel.ControlButtonState.PAUSED;
            }
            notifyItemChanged(position, new ProgressViewModel.Field[]{ ProgressViewModel.Field.state });
        }
    }

    protected class OnButtonOpenFileClickListener implements View.OnClickListener {
        ProgressViewHolder holder;

        public OnButtonOpenFileClickListener(ProgressViewHolder holder) {
            this.holder = holder;
        }

        @Override
        public void onClick(View view) {
            int position = holder.getBindingAdapterPosition();
            if (position == RecyclerView.NO_POSITION) return;
            ProgressViewModel model = models.get(position);
            CommonUtils.openFileByPath(context, model.fileTransfer.getFilePath());
        }
    }
}
