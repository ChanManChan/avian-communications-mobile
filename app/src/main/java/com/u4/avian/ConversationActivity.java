package com.u4.avian;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.u4.avian.common.Constants;
import com.u4.avian.common.NodeNames;
import com.u4.avian.common.Util;

import java.util.HashMap;

public class ConversationActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText etMessage;
    private DatabaseReference rootRef;
    private String currentUserId, chatUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        ImageView ivSend = findViewById(R.id.ivSend);
        etMessage = findViewById(R.id.etMessage);

        ivSend.setOnClickListener(this);
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        currentUserId = firebaseAuth.getCurrentUser().getUid();
        rootRef = FirebaseDatabase.getInstance().getReference();

        if (getIntent().hasExtra(Constants.USER_KEY)) {
            chatUserId = getIntent().getStringExtra(Constants.USER_KEY);
        }
    }

    private void sendMessage(String message, String messageType, String pushId) {
        if (!message.equals("")) {
            HashMap<String, Object> messageMap = new HashMap<>();
            messageMap.put(NodeNames.MESSAGE_ID, pushId);
            messageMap.put(NodeNames.MESSAGE, message);
            messageMap.put(NodeNames.MESSAGE_TYPE, messageType);
            messageMap.put(NodeNames.MESSAGE_FROM, currentUserId);
            messageMap.put(NodeNames.MESSAGE_TIME, ServerValue.TIMESTAMP);

            String currentUserRef = NodeNames.MESSAGES + "/" + currentUserId + "/" + chatUserId;
            String chatUserRef = NodeNames.MESSAGES + "/" + chatUserId + "/" + currentUserId;

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

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ivSend:
                if (Util.connectionAvailable(this)) {
                    DatabaseReference userMessagePush = rootRef.child(NodeNames.MESSAGES).child(currentUserId).child(chatUserId).push();
                    String pushId = userMessagePush.getKey();
                    sendMessage(etMessage.getText().toString().trim(), Constants.MESSAGE_TYPE_TEXT, pushId);
                } else {
                    Toast.makeText(this, getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
}