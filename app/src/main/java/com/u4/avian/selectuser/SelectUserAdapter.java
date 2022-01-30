package com.u4.avian.selectuser;

import static com.u4.avian.common.Constants.PROFILE_IMAGES_FOLDER;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
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
import com.u4.avian.R;

import java.util.List;

public class SelectUserAdapter extends RecyclerView.Adapter<SelectUserAdapter.SelectUserViewHolder> {

    private final Context context;
    private final List<SelectUserModel> selectUserModelList;

    public SelectUserAdapter(Context context, List<SelectUserModel> selectUserModelList) {
        this.context = context;
        this.selectUserModelList = selectUserModelList;
    }

    @NonNull
    @Override
    public SelectUserAdapter.SelectUserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.select_user_layout, parent, false);
        return new SelectUserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SelectUserAdapter.SelectUserViewHolder holder, int position) {
        SelectUserModel userModel = selectUserModelList.get(position);
        holder.tvFullName.setText(userModel.getUserName());
        String photoName = userModel.getPhotoName();
        if (!TextUtils.isEmpty(photoName)) {
            StorageReference fileRef = FirebaseStorage.getInstance().getReference().child(PROFILE_IMAGES_FOLDER + (!photoName.equals("") ? photoName.substring(photoName.lastIndexOf("/")) : ""));
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
        }
        holder.llSelectUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (context instanceof SelectUserActivity) {
                    ((SelectUserActivity) context).returnSelectedUser(userModel.getUserId(), userModel.getUserName(), userModel.getPhotoName());
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return selectUserModelList.size();
    }

    public static class SelectUserViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout llSelectUser;
        private final ImageView ivProfile;
        private final TextView tvFullName;

        public SelectUserViewHolder(@NonNull View itemView) {
            super(itemView);
            llSelectUser = itemView.findViewById(R.id.llSelectUser);
            ivProfile = itemView.findViewById(R.id.ivProfile);
            tvFullName = itemView.findViewById(R.id.tvFullName);
        }
    }
}
