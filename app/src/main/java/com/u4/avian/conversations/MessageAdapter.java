package com.u4.avian.conversations;

import static com.u4.avian.common.Constants.MESSAGE_TYPE_TEXT;

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
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.u4.avian.R;
import com.u4.avian.common.Constants;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private final Context context;
    private final List<MessageModel> messageModelList;

    public MessageAdapter(Context context, List<MessageModel> messageModelList) {
        this.context = context;
        this.messageModelList = messageModelList;
    }

    @NonNull
    @Override
    public MessageAdapter.MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.message_layout, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageAdapter.MessageViewHolder holder, int position) {
        MessageModel messageModel = messageModelList.get(position);
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String fromUserId = messageModel.getMessageFrom();

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.US);
        String dateTime = simpleDateFormat.format(messageModel.getMessageTime());
        String[] splitString = dateTime.split(" ");
        String messageTime = splitString[1];

        if (fromUserId.equals(currentUserId)) {
            if (messageModel.getMessageType().equals(MESSAGE_TYPE_TEXT)) {
                holder.llSent.setVisibility(View.VISIBLE);
                holder.llSentImage.setVisibility(View.GONE);
                holder.tvSentMessage.setText(messageModel.getMessage());
                holder.tvSentMessageTime.setText(messageTime);
            } else {
                holder.llSent.setVisibility(View.GONE);
                holder.llSentImage.setVisibility(View.VISIBLE);
                holder.tvSentImageTime.setText(messageTime);
                Glide.with(context)
                        .load(messageModel.getMessage())
                        .placeholder(R.drawable.ic_default_image)
                        .error(R.drawable.ic_default_image)
                        .into(holder.ivSent);
            }
            holder.llReceived.setVisibility(View.GONE);
            holder.llReceivedImage.setVisibility(View.GONE);
        } else {
            if (messageModel.getMessageType().equals(MESSAGE_TYPE_TEXT)) {
                holder.llReceived.setVisibility(View.VISIBLE);
                holder.llReceivedImage.setVisibility(View.GONE);
                holder.tvReceivedMessage.setText(messageModel.getMessage());
                holder.tvReceivedMessageTime.setText(messageTime);
            } else {
                holder.llReceived.setVisibility(View.GONE);
                holder.llReceivedImage.setVisibility(View.VISIBLE);
                holder.tvReceivedImageTime.setText(messageTime);
                Glide.with(context)
                        .load(messageModel.getMessage())
                        .placeholder(R.drawable.ic_default_image_alt)
                        .error(R.drawable.ic_default_image_alt)
                        .into(holder.ivReceived);
            }
            holder.llSent.setVisibility(View.GONE);
            holder.llSentImage.setVisibility(View.GONE);
        }

        holder.clMessage.setTag(R.id.TAG_MESSAGE, messageModel.getMessage());
        holder.clMessage.setTag(R.id.TAG_MESSAGE_ID, messageModel.getMessageId());
        holder.clMessage.setTag(R.id.TAG_MESSAGE_TYPE, messageModel.getMessageType());

        holder.clMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String messageType = view.getTag(R.id.TAG_MESSAGE_TYPE).toString();
                Uri uri = Uri.parse(view.getTag(R.id.TAG_MESSAGE).toString());
                if (messageType.equals(Constants.MESSAGE_TYPE_VIDEO)) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    intent.setDataAndType(uri, "video/mp4");
                    context.startActivity(intent);
                } else if (messageType.equals(Constants.MESSAGE_TYPE_IMAGE)) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    intent.setDataAndType(uri, "image/jpg");
                    context.startActivity(intent);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return messageModelList.size();
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout llSent;
        private final LinearLayout llReceived;
        private final LinearLayout llSentImage;
        private final LinearLayout llReceivedImage;
        private final ImageView ivSent;
        private final ImageView ivReceived;
        private final TextView tvSentMessage;
        private final TextView tvSentMessageTime;
        private final TextView tvSentImageTime;
        private final TextView tvReceivedMessage;
        private final TextView tvReceivedMessageTime;
        private final TextView tvReceivedImageTime;
        private final ConstraintLayout clMessage;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            llSent = itemView.findViewById(R.id.llSentContainer);
            llReceived = itemView.findViewById(R.id.llReceivedContainer);
            tvSentMessage = itemView.findViewById(R.id.tvSentMessage);
            tvSentMessageTime = itemView.findViewById(R.id.tvSentMessageTime);
            tvReceivedMessage = itemView.findViewById(R.id.tvReceivedMessage);
            tvReceivedMessageTime = itemView.findViewById(R.id.tvReceivedMessageTime);
            clMessage = itemView.findViewById(R.id.clMessage);
            llSentImage = itemView.findViewById(R.id.llSentImageContainer);
            llReceivedImage = itemView.findViewById(R.id.llReceivedImageContainer);
            ivSent = itemView.findViewById(R.id.ivSentImage);
            ivReceived = itemView.findViewById(R.id.ivReceivedImage);
            tvSentImageTime = itemView.findViewById(R.id.tvSentImageTime);
            tvReceivedImageTime = itemView.findViewById(R.id.tvReceivedImageTime);
        }
    }
}
