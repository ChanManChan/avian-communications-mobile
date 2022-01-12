package com.u4.avian.conversations;

import static android.provider.MediaStore.ACTION_IMAGE_CAPTURE;

import static com.u4.avian.common.Constants.MESSAGE_IMAGES_FOLDER;
import static com.u4.avian.common.Constants.MESSAGE_TYPE_IMAGE;
import static com.u4.avian.common.Constants.MESSAGE_TYPE_VIDEO;
import static com.u4.avian.common.Constants.MESSAGE_VIDEOS_FOLDER;
import static com.u4.avian.common.NodeNames.MESSAGES;
import static com.u4.avian.common.NodeNames.MESSAGE_TYPE;
import static com.u4.avian.common.Util.hasPermissions;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.u4.avian.R;
import com.u4.avian.common.Constants;
import com.u4.avian.common.NodeNames;
import com.u4.avian.common.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ConversationActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText etMessage;
    private DatabaseReference rootRef;
    private String currentUserId, chatUserId;
    private RecyclerView rvMessages;
    private SwipeRefreshLayout srlMessages;
    private MessageAdapter messageAdapter;
    private List<MessageModel> messageModelList;
    private int currentPage = 1;
    private static final int RECORDS_PER_PAGE = 30;
    //    private static final int REQUEST_CODE_PICK_IMAGE = 101;
    //    private static final int REQUEST_CODE_PICK_VIDEO = 102;
    private ChildEventListener childEventListener;
    private BottomSheetDialog bottomSheetDialog;
    private LinearLayout llProgress;
    Uri camUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        ImageView ivSend = findViewById(R.id.ivSend);
        ImageView ivAttachment = findViewById(R.id.ivAttachment);
        etMessage = findViewById(R.id.etMessage);
        llProgress = findViewById(R.id.llProgress);

        ivSend.setOnClickListener(this);
        ivAttachment.setOnClickListener(this);
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        currentUserId = firebaseAuth.getCurrentUser().getUid();
        rootRef = FirebaseDatabase.getInstance().getReference();

        if (getIntent().hasExtra(Constants.USER_KEY)) {
            chatUserId = getIntent().getStringExtra(Constants.USER_KEY);
        }

        rvMessages = findViewById(R.id.rvMessages);
        srlMessages = findViewById(R.id.srlMessages);
        messageModelList = new ArrayList<>();
        messageAdapter = new MessageAdapter(this, messageModelList);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(messageAdapter);

        loadMessages();
        rvMessages.scrollToPosition(messageModelList.size() - 1);

        srlMessages.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                currentPage++;
                loadMessages();
            }
        });

        bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.chat_file_options, null);
        bottomSheetView.findViewById(R.id.llCamera).setOnClickListener(this);
        bottomSheetView.findViewById(R.id.llGallery).setOnClickListener(this);
        bottomSheetView.findViewById(R.id.llVideo).setOnClickListener(this);
        bottomSheetView.findViewById(R.id.ivClose).setOnClickListener(this);
        bottomSheetDialog.setContentView(bottomSheetView);
    }

    private void loadMessages() {
        messageModelList.clear();
        DatabaseReference databaseReferenceMessages = rootRef.child(MESSAGES).child(currentUserId).child(chatUserId);
        Query messageQuery = databaseReferenceMessages.limitToLast(currentPage * RECORDS_PER_PAGE);

        if (childEventListener != null) {
            messageQuery.removeEventListener(childEventListener);
        }
        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                MessageModel messageModel = snapshot.getValue(MessageModel.class);
                messageModelList.add(messageModel);
                messageAdapter.notifyDataSetChanged();
                rvMessages.scrollToPosition(messageModelList.size() - 1);
                srlMessages.setRefreshing(false);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                srlMessages.setRefreshing(false);
            }
        };
        messageQuery.addChildEventListener(childEventListener);
    }

    private void sendMessage(String message, String messageType, String pushId) {
        if (!message.equals("")) {
            HashMap<String, Object> messageMap = new HashMap<>();
            messageMap.put(NodeNames.MESSAGE_ID, pushId);
            messageMap.put(NodeNames.MESSAGE, message);
            messageMap.put(MESSAGE_TYPE, messageType);
            messageMap.put(NodeNames.MESSAGE_FROM, currentUserId);
            messageMap.put(NodeNames.MESSAGE_TIME, ServerValue.TIMESTAMP);

            String currentUserRef = MESSAGES + "/" + currentUserId + "/" + chatUserId;
            String chatUserRef = MESSAGES + "/" + chatUserId + "/" + currentUserId;

            HashMap<String, Object> messageUserMap = new HashMap<>();
            messageUserMap.put(currentUserRef + "/" + pushId, messageMap);
            messageUserMap.put(chatUserRef + "/" + pushId, messageMap);

            etMessage.setText("");
            rootRef.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                    if (error != null) {
                        Toast.makeText(ConversationActivity.this, getString(R.string.failed_to_send_message, error.getMessage()), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ConversationActivity.this, getString(R.string.message_sent_successfully), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    ActivityResultLauncher<String> getImage = registerForActivityResult(new ActivityResultContracts.GetContent(), new ActivityResultCallback<Uri>() {
        @Override
        public void onActivityResult(Uri result) {
            uploadFile(result, MESSAGE_TYPE_IMAGE);
        }
    });

    ActivityResultLauncher<String> getVideo = registerForActivityResult(new ActivityResultContracts.GetContent(), new ActivityResultCallback<Uri>() {
        @Override
        public void onActivityResult(Uri result) {
            uploadFile(result, MESSAGE_TYPE_VIDEO);
        }
    });

    ActivityResultLauncher<Intent> captureImage = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == RESULT_OK) {
//                Bitmap bitmap = (Bitmap) result.getData().getExtras().get("data");
//                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
                uploadFile(camUri, MESSAGE_TYPE_IMAGE);
            }
        }
    });

//    private StorageReference generateFileRef(String messageType) {
//        DatabaseReference databaseReference = rootRef.child(MESSAGES).child(currentUserId).child(chatUserId).push();
//        String pushId = databaseReference.getKey();
//        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
//        String folderName = messageType.equals(MESSAGE_TYPE_IMAGE) ? MESSAGE_IMAGES_FOLDER : MESSAGE_VIDEOS_FOLDER;
//        String fileName = pushId + (messageType.equals(MESSAGE_TYPE_VIDEO) ? ".mp4" : ".jpg");
//        return storageReference.child(folderName).child(fileName);
//    }

    private void uploadFile(Uri uri, String messageType) {
        DatabaseReference databaseReference = rootRef.child(MESSAGES).child(currentUserId).child(chatUserId).push();
        String pushId = databaseReference.getKey();
        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        String folderName = messageType.equals(MESSAGE_TYPE_IMAGE) ? MESSAGE_IMAGES_FOLDER : MESSAGE_VIDEOS_FOLDER;
        String fileName = pushId + (messageType.equals(MESSAGE_TYPE_VIDEO) ? ".mp4" : ".jpg");
        StorageReference fileRef = storageReference.child(folderName).child(fileName);
        UploadTask uploadTask = fileRef.putFile(uri);
        uploadProgress(uploadTask, fileRef, pushId, messageType);
    }

//    private void uploadBytes(ByteArrayOutputStream bytes, String messageType) {
//        StorageReference fileRef = generateFileRef(messageType);
//        fileRef.putBytes(bytes.toByteArray());
//    }

    private void uploadProgress(UploadTask task, StorageReference filePath, String pushId, String messageType) {
        View view = getLayoutInflater().inflate(R.layout.file_progress, null);
        ProgressBar pbProgress = view.findViewById(R.id.pbProgress);
        TextView tvProgress = view.findViewById(R.id.tvFileProgress);
        ImageView ivPlay = view.findViewById(R.id.ivPlay);
        ImageView ivPause = view.findViewById(R.id.ivPause);
        ImageView ivCancel = view.findViewById(R.id.ivCancel);

        ivPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                task.pause();
                ivPlay.setVisibility(View.VISIBLE);
                ivPause.setVisibility(View.GONE);
            }
        });

        ivPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                task.resume();
                ivPlay.setVisibility(View.GONE);
                ivPause.setVisibility(View.VISIBLE);
            }
        });

        ivCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                task.cancel();
            }
        });

        llProgress.addView(view);

        tvProgress.setText(getString(R.string.upload_progress, messageType, "0"));

        task.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                double progress = ((double) snapshot.getBytesTransferred() / snapshot.getTotalByteCount()) * 100.0;
                pbProgress.setProgress((int) progress);
                tvProgress.setText(getString(R.string.upload_progress, messageType, String.valueOf(pbProgress.getProgress())));
            }
        });

        task.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                llProgress.removeView(view);
                if (task.isSuccessful()) {
                    filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            String downloadUrl = uri.toString();
                            sendMessage(downloadUrl, messageType, pushId);
                        }
                    });
                }
            }
        });

        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                llProgress.removeView(view);
                Toast.makeText(ConversationActivity.this, getString(R.string.failed_to_upload, e.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ivSend:
                if (Util.connectionAvailable(this)) {
                    DatabaseReference userMessagePush = rootRef.child(MESSAGES).child(currentUserId).child(chatUserId).push();
                    String pushId = userMessagePush.getKey();
                    sendMessage(etMessage.getText().toString().trim(), Constants.MESSAGE_TYPE_TEXT, pushId);
                } else {
                    Toast.makeText(this, getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.ivAttachment:
                String read = Manifest.permission.READ_EXTERNAL_STORAGE, write = Manifest.permission.WRITE_EXTERNAL_STORAGE, camera = Manifest.permission.CAMERA;
                if (hasPermissions(this, read, write, camera)) {
                    showBottomDialog();
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{read, write, camera}, 1);
                }
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (inputMethodManager != null) {
                    inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
                break;
            case R.id.llCamera:
                bottomSheetDialog.dismiss();
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.TITLE, "Captured Image");
                values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera");
                camUri = getApplicationContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                Intent cameraIntent = new Intent(ACTION_IMAGE_CAPTURE);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, camUri);
                captureImage.launch(cameraIntent);
                break;
            case R.id.llGallery:
                bottomSheetDialog.dismiss();
                getImage.launch("image/*");
                break;
            case R.id.llVideo:
                bottomSheetDialog.dismiss();
                getVideo.launch("video/*");
                break;
        }
    }

    private void showBottomDialog() {
        if (bottomSheetDialog != null) {
            bottomSheetDialog.show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
            if (grantResults.length > 0 && Arrays.stream(grantResults).allMatch(grant -> grant == PackageManager.PERMISSION_GRANTED)) {
                showBottomDialog();
            } else {
                Toast.makeText(this, "Permission required to access files", Toast.LENGTH_SHORT).show();
            }
        }
    }
}