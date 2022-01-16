package com.u4.avian.conversations;

import static android.provider.MediaStore.ACTION_IMAGE_CAPTURE;

import static com.u4.avian.common.Constants.MESSAGE_IMAGES_FOLDER;
import static com.u4.avian.common.Constants.MESSAGE_TYPE_IMAGE;
import static com.u4.avian.common.Constants.MESSAGE_TYPE_TEXT;
import static com.u4.avian.common.Constants.MESSAGE_TYPE_VIDEO;
import static com.u4.avian.common.Constants.MESSAGE_VIDEOS_FOLDER;
import static com.u4.avian.common.Constants.PROFILE_PICTURE;
import static com.u4.avian.common.Constants.USER_KEY;
import static com.u4.avian.common.Constants.USER_NAME;
import static com.u4.avian.common.NodeNames.MESSAGE;
import static com.u4.avian.common.NodeNames.MESSAGES;
import static com.u4.avian.common.NodeNames.MESSAGE_ID;
import static com.u4.avian.common.NodeNames.MESSAGE_TYPE;
import static com.u4.avian.common.Util.hasPermissions;
import static com.u4.avian.common.Util.sendNotification;
import static com.u4.avian.common.Util.updateChatDetails;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
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
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipDrawable;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.mlkit.nl.smartreply.SmartReply;
import com.google.mlkit.nl.smartreply.SmartReplyGenerator;
import com.google.mlkit.nl.smartreply.SmartReplySuggestion;
import com.google.mlkit.nl.smartreply.SmartReplySuggestionResult;
import com.google.mlkit.nl.smartreply.TextMessage;
import com.u4.avian.R;
import com.u4.avian.common.Constants;
import com.u4.avian.common.NodeNames;
import com.u4.avian.common.Util;
import com.u4.avian.selectuser.SelectUserActivity;

import java.io.File;
import java.io.IOException;
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
    private TextView tvUserStatus;
    private ChipGroup cgSmartReply;
    private List<TextMessage> mlConversation;
    private List<MessageModel> messageModelList;
    private int currentPage = 1;
    private static final int RECORDS_PER_PAGE = 30;
    //    private static final int REQUEST_CODE_PICK_IMAGE = 101;
    //    private static final int REQUEST_CODE_PICK_VIDEO = 102;
    private ChildEventListener childEventListener;
    private BottomSheetDialog bottomSheetDialog;
    private LinearLayout llProgress;
    private ActivityResultLauncher<Intent> intentActivityResultLauncher;
    Uri camUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("");
            ViewGroup actionBarLayout = (ViewGroup) getLayoutInflater().inflate(R.layout.custom_action_bar, null);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setElevation(0);
            actionBar.setCustomView(actionBarLayout);
            actionBar.setDisplayOptions(actionBar.getDisplayOptions() | ActionBar.DISPLAY_SHOW_CUSTOM);
        }

        cgSmartReply = findViewById(R.id.cgSmartReply);
        mlConversation = new ArrayList<>();
        ImageView ivSend = findViewById(R.id.ivSend);
        ImageView ivAttachment = findViewById(R.id.ivAttachment);
        ImageView ivProfile = findViewById(R.id.ivProfile);
        TextView tvUserName = findViewById(R.id.tvUserName);
        etMessage = findViewById(R.id.etMessage);
        llProgress = findViewById(R.id.llProgress);
        tvUserStatus = findViewById(R.id.tvUserStatus);

        ivSend.setOnClickListener(this);
        ivAttachment.setOnClickListener(this);
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        currentUserId = firebaseAuth.getCurrentUser().getUid();
        rootRef = FirebaseDatabase.getInstance().getReference();

        if (getIntent().hasExtra(Constants.USER_KEY)) {
            chatUserId = getIntent().getStringExtra(Constants.USER_KEY);
        }

        if (getIntent().hasExtra(Constants.USER_NAME)) {
            tvUserName.setText(getIntent().getStringExtra(Constants.USER_NAME));
        }

        if (getIntent().hasExtra(PROFILE_PICTURE)) {
            String photoName = getIntent().getStringExtra(PROFILE_PICTURE);
            if (!TextUtils.isEmpty(photoName)) {
                StorageReference photoRef = FirebaseStorage.getInstance().getReference().child(Constants.PROFILE_IMAGES_FOLDER).child(photoName.substring(photoName.lastIndexOf("/")));
                photoRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Glide.with(ConversationActivity.this)
                                .load(uri)
                                .placeholder(R.drawable.ic_default_profile)
                                .error(R.drawable.ic_default_profile)
                                .into(ivProfile);
                    }
                });
            }
        }

        if (getIntent().hasExtra(MESSAGE) && getIntent().hasExtra(MESSAGE_ID) && getIntent().hasExtra(MESSAGE_TYPE)) {
            String message = getIntent().getStringExtra(MESSAGE);
            String messageId = getIntent().getStringExtra(MESSAGE_ID);
            String messageType = getIntent().getStringExtra(MESSAGE_TYPE);

            DatabaseReference forwardMessageRef = rootRef.child(MESSAGES).child(currentUserId).child(chatUserId).push();
            String newMessageId = forwardMessageRef.getKey();

            if (messageType.equals(MESSAGE_TYPE_TEXT)) {
                sendMessage(message, messageType, newMessageId);
            } else {
                StorageReference storageReference = FirebaseStorage.getInstance().getReference();
                String folder = messageType.equals(MESSAGE_TYPE_VIDEO) ? MESSAGE_VIDEOS_FOLDER : MESSAGE_IMAGES_FOLDER;
                String fileExtension = messageType.equals(MESSAGE_TYPE_VIDEO) ? ".mp4" : ".jpg";
                String oldFileName = messageId + fileExtension;
                String newFileName = newMessageId + fileExtension;
                String localFilePath = getExternalFilesDir(messageType.equals(MESSAGE_TYPE_VIDEO) ? "videos" : "images").getAbsolutePath() + "/" + oldFileName;
                File localFile = new File(localFilePath);
                StorageReference newFileRef = storageReference.child(folder).child(newFileName);

                storageReference.child(folder).child(oldFileName).getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        UploadTask uploadTask = newFileRef.putFile(Uri.fromFile(localFile));
                        uploadProgress(uploadTask, newFileRef, newMessageId, messageType);
                    }
                });
            }
        }

        rvMessages = findViewById(R.id.rvMessages);
        srlMessages = findViewById(R.id.srlMessages);
        messageModelList = new ArrayList<>();
        messageAdapter = new MessageAdapter(this, messageModelList);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(messageAdapter);

        loadMessages();
        rootRef.child(NodeNames.CONVERSATIONS).child(currentUserId).child(chatUserId).child(NodeNames.UNREAD_COUNT).setValue(0);
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

        intentActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if (result.getData() != null) {
                    String userId = result.getData().getStringExtra(USER_KEY);
                    String userName = result.getData().getStringExtra(USER_NAME);
                    String photoName = result.getData().getStringExtra(PROFILE_PICTURE);
                    String message = result.getData().getStringExtra(MESSAGE);
                    String messageId = result.getData().getStringExtra(MESSAGE_ID);
                    String messageType = result.getData().getStringExtra(MESSAGE_TYPE);

                    Intent forwardIntent = new Intent(ConversationActivity.this, ConversationActivity.class);
                    forwardIntent.putExtra(MESSAGE, message);
                    forwardIntent.putExtra(MESSAGE_ID, messageId);
                    forwardIntent.putExtra(MESSAGE_TYPE, messageType);
                    forwardIntent.putExtra(USER_KEY, userId);
                    forwardIntent.putExtra(USER_NAME, userName);
                    forwardIntent.putExtra(PROFILE_PICTURE, photoName);
                    startActivity(forwardIntent);
                    finish();
                }
            }
        });

        DatabaseReference databaseReferenceStatus = rootRef.child(NodeNames.USERS).child(chatUserId);
        databaseReferenceStatus.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Object onlineObject = snapshot.child(NodeNames.ONLINE).getValue();
                if (onlineObject != null) {
                    String status = onlineObject.toString();
                    if (status.equals("true")) {
                        tvUserStatus.setText(Constants.STATUS_ONLINE);
                    } else {
                        tvUserStatus.setText(Constants.STATUS_OFFLINE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                DatabaseReference databaseReferenceTyping = rootRef.child(NodeNames.CONVERSATIONS).child(chatUserId).child(currentUserId);
                if (editable.toString().matches("")) {
                    databaseReferenceTyping.child(NodeNames.TYPING).setValue(Constants.TYPING_STOPPED);
                } else {
                    databaseReferenceTyping.child(NodeNames.TYPING).setValue(Constants.TYPING_STARTED);
                }
            }
        });

        DatabaseReference chatUserRef = rootRef.child(NodeNames.CONVERSATIONS).child(currentUserId).child(chatUserId);
        chatUserRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Object typingObject = snapshot.child(NodeNames.TYPING).getValue();
                if (typingObject != null) {
                    String typingStatus = typingObject.toString();
                    if (typingStatus.equals(Constants.TYPING_STARTED)) {
                        tvUserStatus.setText(Constants.STATUS_TYPING);
                    } else {
                        tvUserStatus.setText(Constants.STATUS_ONLINE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void loadMessages() {
        messageModelList.clear();
        mlConversation.clear();
        cgSmartReply.removeAllViews();
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
                showSmartReplies(messageModel);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                loadMessages();
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

            if (messageType.equals(MESSAGE_TYPE_TEXT)) {
                mlConversation.add(TextMessage.createForLocalUser(message, System.currentTimeMillis()));
            }

            etMessage.setText("");
            rootRef.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                    if (error != null) {
                        Toast.makeText(ConversationActivity.this, getString(R.string.failed_to_send_message, error.getMessage()), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ConversationActivity.this, getString(R.string.message_sent_successfully), Toast.LENGTH_SHORT).show();
                        String title = messageType.equals(MESSAGE_TYPE_TEXT) ? "New Message" : messageType.equals(MESSAGE_TYPE_IMAGE) ? "New Image" : "New Video";
                        String lastMessage = !title.equals("New Message") ? title : message;
                        sendNotification(ConversationActivity.this, title, message, messageType, chatUserId);
                        updateChatDetails(ConversationActivity.this, currentUserId, chatUserId, lastMessage);
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

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case android.R.id.home:
                finish();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void deleteMessage(String messageId, String messageType) {
        DatabaseReference databaseReferenceCurrentUser = rootRef.child(MESSAGES).child(currentUserId).child(chatUserId).child(messageId);
        DatabaseReference databaseReferenceChatUser = rootRef.child(MESSAGES).child(chatUserId).child(currentUserId).child(messageId);
        databaseReferenceCurrentUser.removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    databaseReferenceChatUser.removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(ConversationActivity.this, getString(R.string.message_deleted_successfully), Toast.LENGTH_SHORT).show();
                                if (!messageType.equals(MESSAGE_TYPE_TEXT)) {
                                    StorageReference storageReference = FirebaseStorage.getInstance().getReference();
                                    String folder = messageType.equals(MESSAGE_TYPE_VIDEO) ? MESSAGE_VIDEOS_FOLDER : MESSAGE_IMAGES_FOLDER;
                                    String fileName = messageId + (messageType.equals(MESSAGE_TYPE_VIDEO) ? ".mp4" : ".jpg");
                                    StorageReference fileRef = storageReference.child(folder).child(fileName);
                                    fileRef.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (!task.isSuccessful()) {
                                                Toast.makeText(ConversationActivity.this, getString(R.string.failed_to_delete_file, task.getException()), Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                                }
                            } else {
                                Toast.makeText(ConversationActivity.this, getString(R.string.failed_to_delete_message, task.getException()), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {
                    Toast.makeText(ConversationActivity.this, getString(R.string.failed_to_delete_message, task.getException()), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void downloadFile(String messageId, String messageType, boolean isShare) {
        if (hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            String folderName = messageType.equals(MESSAGE_TYPE_VIDEO) ? MESSAGE_VIDEOS_FOLDER : MESSAGE_IMAGES_FOLDER;
            String fileName = messageId + (messageType.equals(MESSAGE_TYPE_VIDEO) ? ".mp4" : ".jpg");
            StorageReference fileRef = FirebaseStorage.getInstance().getReference().child(folderName).child(fileName);
            String localFilePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/" + fileName.substring(1);
//            String localFilePath = getExternalFilesDir(messageType.equals(MESSAGE_TYPE_VIDEO) ? "videos" : "images").getAbsolutePath() + "/" + fileName;
            File localFile = new File(localFilePath);
            try {
                if (localFile.exists() || localFile.createNewFile()) {
                    FileDownloadTask downloadTask = fileRef.getFile(localFile);
                    View view = getLayoutInflater().inflate(R.layout.file_progress, null);
                    ProgressBar pbProgress = view.findViewById(R.id.pbProgress);
                    TextView tvProgress = view.findViewById(R.id.tvFileProgress);
                    ImageView ivPlay = view.findViewById(R.id.ivPlay);
                    ImageView ivPause = view.findViewById(R.id.ivPause);
                    ImageView ivCancel = view.findViewById(R.id.ivCancel);

                    ivPause.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            downloadTask.pause();
                            ivPlay.setVisibility(View.VISIBLE);
                            ivPause.setVisibility(View.GONE);
                        }
                    });

                    ivPlay.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            downloadTask.resume();
                            ivPlay.setVisibility(View.GONE);
                            ivPause.setVisibility(View.VISIBLE);
                        }
                    });

                    ivCancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            downloadTask.cancel();
                        }
                    });

                    llProgress.addView(view);
                    tvProgress.setText(getString(R.string.download_progress, messageType, "0"));

                    downloadTask.addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(@NonNull FileDownloadTask.TaskSnapshot snapshot) {
                            double progress = ((double) snapshot.getBytesTransferred() / snapshot.getTotalByteCount()) * 100.0;
                            pbProgress.setProgress((int) progress);
                            tvProgress.setText(getString(R.string.download_progress, messageType, String.valueOf(pbProgress.getProgress())));
                        }
                    });

                    downloadTask.addOnCompleteListener(new OnCompleteListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task) {
                            llProgress.removeView(view);
                            if (task.isSuccessful()) {
                                if (isShare) {
                                    Intent intentShareFile = new Intent(Intent.ACTION_SEND);
                                    intentShareFile.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(ConversationActivity.this, ConversationActivity.this.getApplicationContext().getPackageName() + ".fileprovider", localFile));
                                    intentShareFile.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    intentShareFile.setType(messageType.equals(MESSAGE_TYPE_VIDEO) ? "video/*" : "image/*");
                                    startActivity(Intent.createChooser(intentShareFile, getString(R.string.share_via)));
                                } else {
                                    Snackbar snackbar = Snackbar.make(llProgress, getString(R.string.file_downloaded_successfully), Snackbar.LENGTH_LONG);
                                    snackbar.setAction(R.string.view, new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            Uri uri = Uri.parse(localFilePath);
                                            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                            if (messageType.equals(MESSAGE_TYPE_VIDEO)) {
                                                intent.setDataAndType(uri, "video/mp4");
                                            } else if (messageType.equals(MESSAGE_TYPE_IMAGE)) {
                                                intent.setDataAndType(uri, "image/jpg");
                                            }
                                            startActivity(intent);
                                        }
                                    });
                                    snackbar.show();
                                }
                            }
                        }
                    });

                    downloadTask.addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            llProgress.removeView(view);
                            Toast.makeText(ConversationActivity.this, getString(R.string.failed_to_download, e.getMessage()), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(this, getString(R.string.failed_to_store_file), Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(ConversationActivity.this, getString(R.string.failed_to_download, e.getMessage()), Toast.LENGTH_SHORT).show();
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
        }
    }

    public void forwardMessage(String selectedMessageId, String selectedMessage, String selectedMessageType) {
        Intent intent = new Intent(this, SelectUserActivity.class);
        intent.putExtra(MESSAGE, selectedMessage);
        intent.putExtra(MESSAGE_ID, selectedMessageId);
        intent.putExtra(MESSAGE_TYPE, selectedMessageType);
        intentActivityResultLauncher.launch(intent);
    }

    @Override
    public void onBackPressed() {
        rootRef.child(NodeNames.CONVERSATIONS).child(currentUserId).child(chatUserId).child(NodeNames.UNREAD_COUNT).setValue(0);
        super.onBackPressed();
    }

    private void showSmartReplies(MessageModel messageModel) {
        mlConversation.clear();
        cgSmartReply.removeAllViews();
        DatabaseReference databaseReferenceChatUser = rootRef.child(MESSAGES).child(currentUserId).child(chatUserId);
        Query lastMessage = databaseReferenceChatUser.orderByKey().limitToLast(1);
        lastMessage.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot data : snapshot.getChildren()) {
                    MessageModel message = data.getValue(MessageModel.class);
                    if (message != null && message.getMessageFrom().equals(chatUserId) && messageModel.getMessageId().equals(message.getMessageId()) && message.getMessageType().equals(MESSAGE_TYPE_TEXT)) {
                        mlConversation.add(TextMessage.createForRemoteUser(message.getMessage(), System.currentTimeMillis(), chatUserId));
                        if (!mlConversation.isEmpty()) {
                            SmartReplyGenerator smartReplyGenerator = SmartReply.getClient();
                            smartReplyGenerator.suggestReplies(mlConversation).addOnSuccessListener(new OnSuccessListener<SmartReplySuggestionResult>() {
                                @Override
                                public void onSuccess(@NonNull SmartReplySuggestionResult smartReplySuggestionResult) {
                                    if (smartReplySuggestionResult.getStatus() == SmartReplySuggestionResult.STATUS_NOT_SUPPORTED_LANGUAGE) {
                                        Toast.makeText(ConversationActivity.this, getString(R.string.language_not_supported), Toast.LENGTH_SHORT).show();
                                    } else if (smartReplySuggestionResult.getStatus() == SmartReplySuggestionResult.STATUS_SUCCESS) {
                                        for (SmartReplySuggestion suggestion : smartReplySuggestionResult.getSuggestions()) {
                                            String replyText = suggestion.getText();
                                            Chip chip = new Chip(ConversationActivity.this);
                                            ChipDrawable drawable = ChipDrawable.createFromAttributes(ConversationActivity.this, null, 0, R.style.Widget_MaterialComponents_Chip_Action);
                                            chip.setChipDrawable(drawable);
                                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                                            params.setMargins(8, 8, 8, 8);
                                            chip.setLayoutParams(params);
                                            chip.setText(replyText);
                                            chip.setTag(replyText);
                                            chip.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    DatabaseReference messageRef = rootRef.child(MESSAGES).child(currentUserId).child(chatUserId).push();
                                                    String pushId = messageRef.getKey();
                                                    sendMessage(view.getTag().toString(), MESSAGE_TYPE_TEXT, pushId);
                                                }
                                            });
                                            cgSmartReply.addView(chip);
                                        }
                                    }
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(ConversationActivity.this, getString(R.string.generic_error, e.getMessage()), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}