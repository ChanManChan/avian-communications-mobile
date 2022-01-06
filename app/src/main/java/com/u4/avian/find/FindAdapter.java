package com.u4.avian.find;

import static com.u4.avian.common.Constants.IMAGES_FOLDER;
import static com.u4.avian.common.Constants.REQUEST_STATUS_RECEIVED;
import static com.u4.avian.common.Constants.REQUEST_STATUS_SENT;
import static com.u4.avian.common.NodeNames.REQUESTS;
import static com.u4.avian.common.NodeNames.REQUEST_TYPE;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.u4.avian.R;

import java.util.List;

public class FindAdapter extends RecyclerView.Adapter<FindAdapter.FindViewHolder> {

    private Context context;
    private List<FindModel> findModelList;
    private DatabaseReference databaseReference;
    private FirebaseUser currentUser;
    private String userId;

    public FindAdapter(Context context, List<FindModel> findModelList) {
        this.context = context;
        this.findModelList = findModelList;
    }

    @NonNull
    @Override
    public FindAdapter.FindViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.find_layout, parent, false);
        return new FindViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FindAdapter.FindViewHolder holder, int position) {
        FindModel findModel = findModelList.get(position);
        holder.tvFullName.setText(findModel.getUserName());
        String photoName = findModel.getPhotoName();
        StorageReference fileRef = FirebaseStorage.getInstance().getReference().child(IMAGES_FOLDER + photoName.substring(photoName.lastIndexOf("/")));
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

        databaseReference = FirebaseDatabase.getInstance().getReference().child(REQUESTS);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        holder.btnSendRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                holder.btnSendRequest.setEnabled(false);
                holder.pbRequest.setVisibility(View.VISIBLE);
                userId = findModel.getUserId();
                databaseReference.child(currentUser.getUid()).child(userId).child(REQUEST_TYPE).setValue(REQUEST_STATUS_SENT)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    databaseReference.child(userId).child(currentUser.getUid()).child(REQUEST_TYPE).setValue(REQUEST_STATUS_RECEIVED).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Toast.makeText(context, R.string.request_sent_successfully, Toast.LENGTH_SHORT).show();
                                                holder.btnSendRequest.setVisibility(View.GONE);
                                                holder.btnCancelRequest.setVisibility(View.VISIBLE);
                                            } else {
                                                Toast.makeText(context, context.getString(R.string.failed_to_send_request, task.getException()), Toast.LENGTH_SHORT).show();
                                                holder.btnSendRequest.setVisibility(View.VISIBLE);
                                                holder.btnCancelRequest.setVisibility(View.GONE);
                                            }
                                            holder.pbRequest.setVisibility(View.GONE);
                                        }
                                    });
                                } else {
                                    Toast.makeText(context, context.getString(R.string.failed_to_send_request, task.getException()), Toast.LENGTH_SHORT).show();
                                    holder.btnSendRequest.setVisibility(View.VISIBLE);
                                    holder.btnCancelRequest.setVisibility(View.GONE);
                                    holder.pbRequest.setVisibility(View.GONE);
                                }
                            }
                        });
            }
        });
    }

    @Override
    public int getItemCount() {
        return findModelList.size();
    }

    public class FindViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivProfile;
        private TextView tvFullName;
        private Button btnSendRequest, btnCancelRequest;
        private ProgressBar pbRequest;

        public FindViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.ivProfile);
            tvFullName = itemView.findViewById(R.id.tvFullName);
            btnSendRequest = itemView.findViewById(R.id.btnSendRequest);
            btnCancelRequest = itemView.findViewById(R.id.btnCancelRequest);
            pbRequest = itemView.findViewById(R.id.pbRequest);
        }
    }
}
