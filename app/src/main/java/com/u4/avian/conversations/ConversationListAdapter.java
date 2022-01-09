package com.u4.avian.conversations;

import static com.u4.avian.common.Constants.IMAGES_FOLDER;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.u4.avian.ConversationActivity;
import com.u4.avian.R;
import com.u4.avian.common.Constants;

import java.util.List;

public class ConversationListAdapter extends RecyclerView.Adapter<ConversationListAdapter.ConversationListViewHolder> {

    private final Context context;
    private final List<ConversationListModel> conversationListModelList;

    public ConversationListAdapter(Context context, List<ConversationListModel> conversationListModelList) {
        this.context = context;
        this.conversationListModelList = conversationListModelList;
    }

    @NonNull
    @Override
    public ConversationListAdapter.ConversationListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.conversation_layout, parent, false);
        return new ConversationListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationListAdapter.ConversationListViewHolder holder, int position) {
        ConversationListModel conversationListModel = conversationListModelList.get(position);
        holder.tvFullName.setText(conversationListModel.getUserName());
        String photoName = conversationListModel.getPhotoName();
        StorageReference fileRef = FirebaseStorage.getInstance().getReference().child(IMAGES_FOLDER + (!photoName.equals("") ? photoName.substring(photoName.lastIndexOf("/")) : ""));
        fileRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Glide.with(context)
                        .load(uri)
                        .placeholder(R.drawable.ic_default_profile)
                        .error(R.drawable.ic_default_profile)
                        .into(holder.ivProfile);
            }
        });

        holder.llConversationList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, ConversationActivity.class);
                intent.putExtra(Constants.USER_KEY, conversationListModel.getUserId());
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return conversationListModelList.size();
    }

    public class ConversationListViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout llConversationList;
        private final TextView tvFullName;
        private final TextView tvLastMessage;
        private final TextView tvLastMessageTime;
        private final TextView tvUnreadCount;
        private final ImageView ivProfile;

        public ConversationListViewHolder(@NonNull View itemView) {
            super(itemView);
            llConversationList = itemView.findViewById(R.id.llConversationList);
            tvFullName = itemView.findViewById(R.id.tvFullName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvLastMessageTime = itemView.findViewById(R.id.tvLastMessageTime);
            tvUnreadCount = itemView.findViewById(R.id.tvUnreadCount);
            ivProfile = itemView.findViewById(R.id.ivProfile);
        }
    }
}
