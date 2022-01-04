package com.u4.avian.password;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.u4.avian.R;

public class ResetPasswordActivity extends AppCompatActivity {

    private TextInputEditText etEmail;
    private TextView tvMessage;
    private LinearLayout llResetPassword, llMessage;
    private Button btnRetry;
    private View progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        etEmail = findViewById(R.id.etEmail);
        tvMessage = findViewById(R.id.tvMessage);
        llMessage = findViewById(R.id.llMessage);
        llResetPassword = findViewById(R.id.llResetPassword);
        btnRetry = findViewById(R.id.btnRetry);
        progressBar = findViewById(R.id.progressBar);
    }

    public void btnResetPasswordClick(View view) {
        Editable emailText = etEmail.getText();

        if (emailText != null) {
            String email = emailText.toString().trim();
            if (email.equals("")) {
                etEmail.setError(getString(R.string.enter_email));
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
            firebaseAuth.sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    progressBar.setVisibility(View.GONE);
                    llResetPassword.setVisibility(View.GONE);
                    llMessage.setVisibility(View.VISIBLE);

                    if (task.isSuccessful()) {
                        tvMessage.setText(getString(R.string.reset_password_instructions, email));
                        btnRetry.setEnabled(false);
                        new CountDownTimer(30000, 1000) {

                            @Override
                            public void onTick(long l) {
                                btnRetry.setText(getString(R.string.resend_timer, String.valueOf(l / 1000)));
                            }

                            @Override
                            public void onFinish() {
                                btnRetry.setText(R.string.retry);
                                btnRetry.setEnabled(true);
                            }
                        }.start();

                        return;
                    }
                    tvMessage.setText(getString(R.string.email_failed, task.getException()));
                }
            });
        }
    }

    public void btnRetryClick(View view) {
        llResetPassword.setVisibility(View.VISIBLE);
        llMessage.setVisibility(View.GONE);
    }

    public void btnCloseClick(View view) {
        finish();
    }
}