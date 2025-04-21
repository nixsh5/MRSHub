package com.example.mrshub;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ResourceAdapter extends FirebaseRecyclerAdapter<Resource, ResourceAdapter.ResourceViewHolder> {

    private ResourceClickListener clickListener;

    public interface ResourceClickListener {
        void onDownloadClick(Resource resource);
        void onItemClick(Resource resource);
        void onDeleteClick(Resource resource); // <-- Added
    }

    public ResourceAdapter(@NonNull FirebaseRecyclerOptions<Resource> options, ResourceClickListener listener) {
        super(options);
        this.clickListener = listener;
    }

    @Override
    protected void onBindViewHolder(@NonNull ResourceViewHolder holder, int position, @NonNull Resource model) {
        holder.tvFileName.setText(model.getFileName() != null ? model.getFileName() : "No Name");
        holder.tvDescription.setText(model.getDescription() != null ? model.getDescription() : "");
        holder.tvUploadedBy.setText("Uploaded by: " + (model.getUploadedBy() != null ? model.getUploadedBy() : "Unknown"));

        // Format timestamp to date
        if (model.getUploadTimestamp() != 0) {
            long timestamp = Math.abs(model.getUploadTimestamp());
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            holder.tvUploadDate.setText(sdf.format(new Date(timestamp)));
        } else {
            holder.tvUploadDate.setText("Recently");
        }

        // Set file type indicator
        String fileType = model.getFileType();
        if (fileType != null) {
            fileType = fileType.toLowerCase();
            if (fileType.equals("pdf")) {
                holder.tvFileType.setText("PDF");
            } else if (fileType.equals("doc") || fileType.equals("docx")) {
                holder.tvFileType.setText("DOC");
            } else if (fileType.equals("jpg") || fileType.equals("jpeg") ||
                    fileType.equals("png") || fileType.equals("gif")) {
                holder.tvFileType.setText("IMG");
            } else {
                holder.tvFileType.setText(fileType.toUpperCase());
            }
        } else {
            holder.tvFileType.setText("FILE");
        }

        // Set click listeners
        holder.btnDownload.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onDownloadClick(model);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onDeleteClick(model);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onItemClick(model);
            }
        });
    }

    @NonNull
    @Override
    public ResourceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_resource, parent, false);
        return new ResourceViewHolder(view);
    }

    static class ResourceViewHolder extends RecyclerView.ViewHolder {
        TextView tvFileName, tvDescription, tvUploadedBy, tvUploadDate, tvFileType;
        MaterialButton btnDownload, btnDelete; // <-- Added btnDelete

        public ResourceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvUploadedBy = itemView.findViewById(R.id.tvUploadedBy);
            tvUploadDate = itemView.findViewById(R.id.tvUploadDate);
            tvFileType = itemView.findViewById(R.id.tvFileType);
            btnDownload = itemView.findViewById(R.id.btnDownload);
            btnDelete = itemView.findViewById(R.id.btnDelete); // <-- Added
        }
    }
}
