package com.example.billgenerator.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.billgenerator.R;
import com.example.billgenerator.models.DraftItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class DraftAdapter extends RecyclerView.Adapter<DraftAdapter.ViewHolder> {

    private final List<DraftItem> drafts;
    private final OnDraftInteractionListener listener;

    public interface OnDraftInteractionListener {
        void onResume(DraftItem draft);
        void onDelete(DraftItem draft);
    }

    public DraftAdapter(List<DraftItem> drafts, OnDraftInteractionListener listener) {
        this.drafts = drafts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_draft, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DraftItem draft = drafts.get(position);
        holder.nameText.setText(draft.name);
        holder.dateText.setText(draft.date);

        // Parse summary from JSON
        try {
            JSONObject obj = new JSONObject(draft.jsonData);
            JSONArray items = obj.optJSONArray("items");
            int itemCount = items != null ? items.length() : 0;
            String total = obj.optString("total", "0");
            holder.summaryText.setText("Items: " + itemCount + " | Total: ₹" + total);
        } catch (Exception e) {
            holder.summaryText.setText("No details available");
        }

        holder.clickArea.setOnClickListener(v -> listener.onResume(draft));
        holder.deleteBtn.setOnClickListener(v -> listener.onDelete(draft));
    }

    @Override
    public int getItemCount() {
        return drafts.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, dateText, summaryText;
        View clickArea;
        ImageButton deleteBtn;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.draft_customer_name);
            dateText = itemView.findViewById(R.id.draft_date);
            summaryText = itemView.findViewById(R.id.draft_summary);
            clickArea = itemView.findViewById(R.id.draft_click_area);
            deleteBtn = itemView.findViewById(R.id.btn_delete_draft);
        }
    }
}
