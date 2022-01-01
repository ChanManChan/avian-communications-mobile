package com.u4.avian.login;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.u4.avian.R;
import com.u4.avian.signup.SignupActivity;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
    }

    public void btnLoginClick(View view) {
        Editable emailText = etEmail.getText();
        Editable passwordText = etPassword.getText();
        String email, password;

        if (emailText != null && passwordText != null) {
            email = emailText.toString().trim();
            password = passwordText.toString().trim();

            if (!email.equals("") && !password.equals("")) {
                FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
                firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            //
                        } else {
                            Toast.makeText(LoginActivity.this, "Login failed: " + task.getException(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            } else {
                if (email.equals("")) {
                    etEmail.setError(getString(R.string.enter_email));
                }

                if (password.equals("")) {
                    etPassword.setError(getString(R.string.enter_password));
                }
            }
        }
    }

    public void tvSignupClick(View view) {
        startActivity(new Intent(this, SignupActivity.class));
    }
}