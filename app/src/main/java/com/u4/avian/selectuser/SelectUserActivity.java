package com.u4.avian.selectuser;

import static com.u4.avian.common.Constants.PROFILE_PICTURE;
import static com.u4.avian.common.Constants.USER_KEY;
import static com.u4.avian.common.Constants.USER_NAME;
import static com.u4.avian.common.NodeNames.MESSAGE;
import static com.u4.avian.common.NodeNames.MESSAGE_ID;
import static com.u4.avian.common.NodeNames.MESSAGE_TYPE;
import static com.u4.avian.common.NodeNames.PHOTO;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.u4.avian.R;
import com.u4.avian.common.NodeNames;

import java.util.ArrayList;
import java.util.List;

public class SelectUserActivity extends AppCompatActivity {

    private SelectUserAdapter selectUserAdapter;
    private List<SelectUserModel> selectUserModelList;
    private View progressBar;
    private DatabaseReference databaseReferenceUsers, databaseReferenceConversations;
    private ValueEventListener valueEventListener;
    private String selectedMessage, selectedMessageId, selectedMessageType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_user);

        if (getIntent().hasExtra(MESSAGE) && getIntent().hasExtra(MESSAGE_ID) && getIntent().hasExtra(MESSAGE_TYPE)) {
            selectedMessage = getIntent().getStringExtra(MESSAGE);
            selectedMessageId = getIntent().getStringExtra(MESSAGE_ID);
            selectedMessageType = getIntent().getStringExtra(MESSAGE_TYPE);
        }

        RecyclerView rvSelectUser = findViewById(R.id.rvSelectUser);
        progressBar = findViewById(R.id.progressBar);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        rvSelectUser.setLayoutManager(linearLayoutManager);
        selectUserModelList = new ArrayList<>();
        selectUserAdapter = new SelectUserAdapter(this, selectUserModelList);
        rvSelectUser.setAdapter(selectUserAdapter);
        progressBar.setVisibility(View.VISIBLE);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        databaseReferenceConversations = databaseReference.child(NodeNames.CONVERSATIONS).child(currentUser.getUid());
        databaseReferenceUsers = databaseReference.child(NodeNames.USERS);

        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String userId = ds.getKey();
                    if (userId != null) {
                        databaseReferenceUsers.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                Object userNameObject = snapshot.child(NodeNames.NAME).getValue();
                                Object photoNameObject = snapshot.child(PHOTO).getValue();
                                String userName = userNameObject != null ? userNameObject.toString() : "";
                                String photoName = photoNameObject != null ? photoNameObject.toString() : "";
                                SelectUserModel userModel = new SelectUserModel(userId, userName, photoName);
                                selectUserModelList.add(userModel);
                                selectUserAdapter.notifyDataSetChanged();
                                progressBar.setVisibility(View.GONE);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(SelectUserActivity.this, getString(R.string.failed_to_fetch_users, error.getMessage()), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SelectUserActivity.this, getString(R.string.failed_to_fetch_users, error.getMessage()), Toast.LENGTH_SHORT).show();
            }
        };
        databaseReferenceConversations.addValueEventListener(valueEventListener);
    }

    public void returnSelectedUser(String userId, String userName, String photoName) {
        databaseReferenceConversations.removeEventListener(valueEventListener);
        Intent intent = new Intent();
        intent.putExtra(MESSAGE, selectedMessage);
        intent.putExtra(MESSAGE_ID, selectedMessageId);
        intent.putExtra(MESSAGE_TYPE, selectedMessageType);
        intent.putExtra(USER_KEY, userId);
        intent.putExtra(USER_NAME, userName);
        intent.putExtra(PROFILE_PICTURE, photoName);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
}