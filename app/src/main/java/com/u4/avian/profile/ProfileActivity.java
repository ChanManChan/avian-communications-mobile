package com.u4.avian.profile;

import static com.u4.avian.common.NodeNames.EMAIL;
import static com.u4.avian.common.NodeNames.NAME;
import static com.u4.avian.common.NodeNames.ONLINE;
import static com.u4.avian.common.NodeNames.PHOTO;
import static com.u4.avian.common.NodeNames.USERS;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.u4.avian.R;
import com.u4.avian.login.LoginActivity;
import com.u4.avian.password.ChangePasswordActivity;

import java.util.HashMap;

public class ProfileActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail;
    private ImageView ivProfile;
    private FirebaseUser firebaseUser;
    private DatabaseReference databaseReference;
    private StorageReference storageReference;
    private Uri localFileUri, serverFileUri;
    private boolean removePicture = false;
    private View progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        etEmail = findViewById(R.id.etEmail);
        etName = findViewById(R.id.etName);
        ivProfile = findViewById(R.id.ivProfile);
        progressBar = findViewById(R.id.progressBar);
        storageReference = FirebaseStorage.getInstance().getReference();
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        if (firebaseUser != null) {
            etName.setText(firebaseUser.getDisplayName());
            etEmail.setText(firebaseUser.getEmail());
            etEmail.setEnabled(false);
            serverFileUri = firebaseUser.getPhotoUrl();

            if (serverFileUri != null) {
                Glide.with(this)
                        .load(serverFileUri)
                        .placeholder(R.drawable.ic_default_profile)
                        .error(R.drawable.ic_default_profile)
                        .into(ivProfile);
            }
        }
    }

    public void btnSaveClick(View view) {
        Editable nameText = etName.getText();
        if (nameText != null) {
            if (nameText.toString().equals("")) {
                etName.setError(getString(R.string.enter_name));
                return;
            }

            progressBar.setVisibility(View.VISIBLE);

            if (localFileUri != null) {
                updatePhoto();
                progressBar.setVisibility(View.GONE);
                return;
            }

            if (removePicture) {
                updateUserDetails("removePhoto");
                removePicture = false;
                progressBar.setVisibility(View.GONE);
                return;
            }

            String currentName = nameText.toString().trim();
            String previousName = firebaseUser.getDisplayName();

            if (!currentName.equals(previousName)) {
                updateUserDetails("updateName");
                progressBar.setVisibility(View.GONE);
            }
        }
    }

    public void btnLogoutClick(View view) {
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        firebaseAuth.signOut();
        startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
        finish();
    }

    public void changeImage(View view) {
        if (serverFileUri == null) {
            pickImage();
        } else {
            PopupMenu popupMenu = new PopupMenu(this, view);
            popupMenu.getMenuInflater().inflate(R.menu.menu_change_profile_picture, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    int id = menuItem.getItemId();

                    if (id == R.id.menuChangePicture) {
                        pickImage();
                    } else if (id == R.id.menuRemovePicture) {
                        removePicture = true;
                        ivProfile.setImageResource(R.drawable.ic_default_profile);
                    }
                    return false;
                }
            });
            popupMenu.show();
        }
    }

    ActivityResultLauncher<String> mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(), new ActivityResultCallback<Uri>() {
        @Override
        public void onActivityResult(Uri result) {
            localFileUri = result;
            ivProfile.setImageURI(localFileUri);
        }
    });

    private void pickImage() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            mGetContent.launch("image/*");
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 102);
        }
    }

    private void updatePhoto() {
        String fileName = firebaseUser.getUid() + ".jpg";
        StorageReference fileRef = storageReference.child("images/" + fileName);
        fileRef.putFile(localFileUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                if (task.isSuccessful()) {
                    fileRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            serverFileUri = uri;
                            updateUserDetails("addPhoto");
                        }
                    });
                }
            }
        });
    }

    private void updateUserDetails(String operation) {
        Editable nameText = etName.getText();
        Editable emailText = etEmail.getText();
        if (nameText != null && emailText != null) {
            UserProfileChangeRequest request = null;

            if (operation.equals("updateName")) {
                request = new UserProfileChangeRequest.Builder()
                        .setDisplayName(nameText.toString().trim())
                        .build();
            }

            if (operation.equals("addPhoto")) {
                request = new UserProfileChangeRequest.Builder()
                        .setDisplayName(nameText.toString().trim())
                        .setPhotoUri(serverFileUri)
                        .build();
            }

            if (operation.equals("removePhoto")) {
                request = new UserProfileChangeRequest.Builder()
                        .setDisplayName(nameText.toString().trim())
                        .setPhotoUri(null)
                        .build();
            }

            if (request != null) {
                firebaseUser.updateProfile(request).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            String userID = firebaseUser.getUid();
                            databaseReference = FirebaseDatabase.getInstance().getReference().child(USERS);
                            HashMap<String, String> hashMap = new HashMap<>();
                            hashMap.put(NAME, nameText.toString().trim());
                            hashMap.put(EMAIL, emailText.toString().trim());
                            hashMap.put(ONLINE, "true");

                            if (operation.equals("removePhoto")) {
                                hashMap.put(PHOTO, "");
                            }

                            if (operation.equals("addPhoto") || (operation.equals("updateName") && serverFileUri != null)) {
                                hashMap.put(PHOTO, serverFileUri.getPath());
                            }

                            databaseReference.child(userID).setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    Toast.makeText(ProfileActivity.this, "User profile updated successfully", Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                            });
                        } else {
                            Toast.makeText(ProfileActivity.this, "Failed to update profile: " + task.getException(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }
    }

    public void btnChangePasswordClick(View view) {
        startActivity(new Intent(ProfileActivity.this, ChangePasswordActivity.class));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 102 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            pickImage();
        } else {
            Toast.makeText(this, getString(R.string.permission_required), Toast.LENGTH_SHORT).show();
        }
    }
}