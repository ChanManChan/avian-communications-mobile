package com.u4.avian.signup;

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
import android.util.Patterns;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
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

import java.util.HashMap;

public class SignupActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etName, etPassword, etConfirmPassword;
    private ImageView ivProfile;
    private FirebaseUser firebaseUser;
    private DatabaseReference databaseReference;
    private StorageReference storageReference;
    private Uri localFileUri, serverFileUri;
    private View progressBar, constraintLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        etEmail = findViewById(R.id.etEmail);
        etName = findViewById(R.id.etName);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        ivProfile = findViewById(R.id.ivProfile);
        progressBar = findViewById(R.id.progressBar);
        constraintLayout = findViewById(R.id.signupConstraintLayout);
        storageReference = FirebaseStorage.getInstance().getReference();
    }

    ActivityResultLauncher<String> mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(), new ActivityResultCallback<Uri>() {
        @Override
        public void onActivityResult(Uri result) {
            localFileUri = result;
            ivProfile.setImageURI(localFileUri);
        }
    });

    public void pickImage(View view) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            mGetContent.launch("image/*");
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 102);
        }
    }

    private void updateNameAndPhoto() {
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
                            updateUserDetails("usernameWithProfilePicture");
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

            if (operation.equals("usernameOnly")) {
                request = new UserProfileChangeRequest.Builder()
                        .setDisplayName(nameText.toString().trim())
                        .build();
            }

            if (operation.equals("usernameWithProfilePicture")) {
                request = new UserProfileChangeRequest.Builder()
                        .setDisplayName(nameText.toString().trim())
                        .setPhotoUri(serverFileUri)
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
                            hashMap.put(PHOTO, "");

                            if (operation.equals("usernameWithProfilePicture")) {
                                hashMap.put(PHOTO, serverFileUri.getPath());
                            }

                            databaseReference.child(userID).setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    progressBar.setVisibility(View.GONE);
                                    constraintLayout.setVisibility(View.VISIBLE);
                                    Toast.makeText(SignupActivity.this, "User created successfully", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                                }
                            });
                        } else {
                            progressBar.setVisibility(View.GONE);
                            constraintLayout.setVisibility(View.VISIBLE);
                            Toast.makeText(SignupActivity.this, "Failed to update profile: " + task.getException(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }
    }

    public void btnSignupClick(View view) {
        constraintLayout.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        Editable emailText = etEmail.getText();
        Editable nameText = etName.getText();
        Editable passwordText = etPassword.getText();
        Editable confirmPasswordText = etConfirmPassword.getText();
        String email, name, password, confirmPassword;

        if (emailText != null && nameText != null && passwordText != null && confirmPasswordText != null) {
            email = emailText.toString().trim();
            name = nameText.toString().trim();
            password = passwordText.toString().trim();
            confirmPassword = confirmPasswordText.toString().trim();
            boolean validEmail = !email.equals("") && Patterns.EMAIL_ADDRESS.matcher(email).matches();
            boolean matchingPasswords = !password.equals("") && !confirmPassword.equals("") && password.equals(confirmPassword);

            if (validEmail && !name.equals("") && matchingPasswords) {
                FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
                firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            firebaseUser = firebaseAuth.getCurrentUser();

                            if (localFileUri != null) {
                                updateNameAndPhoto();
                            } else {
                                updateUserDetails("usernameOnly");
                            }
                        } else {
                            constraintLayout.setVisibility(View.VISIBLE);
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(SignupActivity.this, "Signup failed: " + task.getException(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            } else {
                progressBar.setVisibility(View.GONE);
                constraintLayout.setVisibility(View.VISIBLE);

                if (!validEmail) {
                    etEmail.setError(getString(R.string.enter_email));
                }

                if (name.equals("")) {
                    etName.setError(getString(R.string.enter_name));
                }

                if (password.equals("")) {
                    etPassword.setError(getString(R.string.enter_password));
                }

                if (confirmPassword.equals("") || !matchingPasswords) {
                    etConfirmPassword.setError(getString(R.string.confirm_password));
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 102 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            pickImage(null);
        } else {
            Toast.makeText(this, getString(R.string.permission_required), Toast.LENGTH_SHORT).show();
        }
    }
}