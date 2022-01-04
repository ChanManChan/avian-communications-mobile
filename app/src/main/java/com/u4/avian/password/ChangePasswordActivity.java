package com.u4.avian.password;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.u4.avian.R;

public class ChangePasswordActivity extends AppCompatActivity {

    private TextInputEditText etPassword, etConfirmPassword;
    private View progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        progressBar = findViewById(R.id.progressBar);
    }

    public void btnChangePasswordClick(View view) {
        progressBar.setVisibility(View.VISIBLE);
        Editable passwordText = etPassword.getText();
        Editable confirmPasswordText = etConfirmPassword.getText();

        if (passwordText != null && confirmPasswordText != null) {
            String password = passwordText.toString().trim();
            String confirmPassword = confirmPasswordText.toString().trim();

            if (!password.equals("") && !confirmPassword.equals("") && password.equals(confirmPassword)) {
                FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
                FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

                if (firebaseUser != null) {
                    firebaseUser.updatePassword(password).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            progressBar.setVisibility(View.GONE);
                            if (task.isSuccessful()) {
                                Toast.makeText(ChangePasswordActivity.this, getString(R.string.password_updated), Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(ChangePasswordActivity.this, getString(R.string.generic_error, task.getException()), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            } else {
                progressBar.setVisibility(View.GONE);

                if (password.equals("")) {
                    etPassword.setError(getString(R.string.enter_password));
                }

                if (confirmPassword.equals("")) {
                    etConfirmPassword.setError(getString(R.string.confirm_password));
                }

                if (!password.equals(confirmPassword)) {
                    etConfirmPassword.setError(getString(R.string.password_mismatch));
                }
            }
        }
    }
}