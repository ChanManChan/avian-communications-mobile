package com.u4.avian.signup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.u4.avian.R;
import com.u4.avian.login.LoginActivity;

public class SignupActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etName, etPassword, etConfirmPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        etEmail = findViewById(R.id.etEmail);
        etName = findViewById(R.id.etName);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
    }

    public void btnSignupClick(View view) {
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
                            Toast.makeText(SignupActivity.this, "User created successfully", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                        } else {
                            Toast.makeText(SignupActivity.this, "Signup failed: " + task.getException(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            } else {
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
}