package com.u4.avian.request;

import static com.u4.avian.common.Constants.IMAGES_FOLDER;
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
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.Transaction;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.u4.avian.R;
import com.u4.avian.common.Constants;
import com.u4.avian.common.NodeNames;

import java.util.List;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.RequestViewHolder> {

    private final Context context;
    private final List<RequestModel> requestModelList;
    private DatabaseReference databaseReferenceRequests, databaseReferenceConversations;
    private FirebaseUser currentUser;

    public RequestAdapter(Context context, List<RequestModel> requestModelList) {
        this.context = context;
        this.requestModelList = requestModelList;
    }

    @NonNull
    @Override
    public RequestAdapter.RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.request_layout, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestAdapter.RequestViewHolder holder, int position) {
        RequestModel requestModel = requestModelList.get(position);
        holder.tvFullName.setText(requestModel.getUserName());
        String photoName = requestModel.getPhotoName();
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

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        databaseReferenceRequests = reference.child(REQUESTS);
        databaseReferenceConversations = reference.child(NodeNames.CONVERSATIONS);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        holder.btnAcceptRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                holder.pbDecision.setVisibility(View.VISIBLE);
                holder.btnRejectRequest.setEnabled(false);
                holder.btnAcceptRequest.setEnabled(false);
                String userId = requestModel.getUserId();
                reference.runTransaction(new Transaction.Handler() {
                    @NonNull
                    @Override
                    public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                        databaseReferenceConversations.child(currentUser.getUid()).child(userId).child(NodeNames.TIMESTAMP).setValue(ServerValue.TIMESTAMP);
                        databaseReferenceConversations.child(userId).child(currentUser.getUid()).child(NodeNames.TIMESTAMP).setValue(ServerValue.TIMESTAMP);
                        databaseReferenceRequests.child(currentUser.getUid()).child(userId).child(REQUEST_TYPE).setValue(Constants.REQUEST_STATUS_ACCEPTED);
                        databaseReferenceRequests.child(userId).child(currentUser.getUid()).child(REQUEST_TYPE).setValue(Constants.REQUEST_STATUS_ACCEPTED);
                        return Transaction.success(currentData);
                    }

                    @Override
                    public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                        Toast.makeText(context, context.getString(R.string.accepted_request), Toast.LENGTH_SHORT).show();
                        enableButtons(holder);
                    }
                }, false);
            }
        });

        holder.btnRejectRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                holder.pbDecision.setVisibility(View.VISIBLE);
                holder.btnRejectRequest.setEnabled(false);
                holder.btnAcceptRequest.setEnabled(false);

                String userId = requestModel.getUserId();
                databaseReferenceRequests.child(currentUser.getUid()).child(userId).child(REQUEST_TYPE).setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            databaseReferenceRequests.child(userId).child(currentUser.getUid()).child(REQUEST_TYPE).setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(context, context.getString(R.string.request_rejected), Toast.LENGTH_SHORT).show();
                                        enableButtons(holder);
                                    } else {
                                        Toast.makeText(context, context.getString(R.string.failed_to_reject_request, task.getException()), Toast.LENGTH_SHORT).show();
                                        enableButtons(holder);
                                    }
                                }
                            });
                        } else {
                            Toast.makeText(context, context.getString(R.string.failed_to_reject_request, task.getException()), Toast.LENGTH_SHORT).show();
                            enableButtons(holder);
                        }
                    }
                });
            }
        });
    }

    @Override
    public int getItemCount() {
        return requestModelList.size();
    }

    private void enableButtons(RequestAdapter.RequestViewHolder holder) {
        holder.pbDecision.setVisibility(View.GONE);
        holder.btnRejectRequest.setEnabled(true);
        holder.btnAcceptRequest.setEnabled(true);
    }

    public static class RequestViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvFullName;
        private final ImageView ivProfile;
        private final Button btnAcceptRequest;
        private final Button btnRejectRequest;
        private final ProgressBar pbDecision;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFullName = itemView.findViewById(R.id.tvFullName);
            ivProfile = itemView.findViewById(R.id.ivProfile);
            btnAcceptRequest = itemView.findViewById(R.id.btnAcceptRequest);
            btnRejectRequest = itemView.findViewById(R.id.btnRejectRequest);
            pbDecision = itemView.findViewById(R.id.pbDecision);
        }
    }
}
