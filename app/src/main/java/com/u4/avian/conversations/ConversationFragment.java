package com.u4.avian.conversations;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.u4.avian.R;
import com.u4.avian.common.NodeNames;

import java.util.ArrayList;
import java.util.List;

public class ConversationFragment extends Fragment {

    private View progressBar;
    private TextView tvEmptyConversationList;
    private ConversationListAdapter conversationListAdapter;
    private List<ConversationListModel> conversationListModelList;
    private List<String> userIds;
    private DatabaseReference databaseReferenceUsers;
    private ChildEventListener childEventListener;
    private Query query;

    public ConversationFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_conversation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView rvConversationList = view.findViewById(R.id.rvConversations);
        tvEmptyConversationList = view.findViewById(R.id.tvEmptyConversationList);
        progressBar = view.findViewById(R.id.progressBar);
        conversationListModelList = new ArrayList<>();
        userIds = new ArrayList<>();
        conversationListAdapter = new ConversationListAdapter(getActivity(), conversationListModelList);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        rvConversationList.setLayoutManager(linearLayoutManager);
        rvConversationList.setAdapter(conversationListAdapter);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        databaseReferenceUsers = databaseReference.child(NodeNames.USERS);
        DatabaseReference databaseReferenceConversations = databaseReference.child(NodeNames.CONVERSATIONS).child(currentUser.getUid());
        query = databaseReferenceConversations.orderByChild(NodeNames.TIMESTAMP);

        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                updateList(snapshot, true, snapshot.getKey());
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                updateList(snapshot, false, snapshot.getKey());
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };

        query.addChildEventListener(childEventListener);

        progressBar.setVisibility(View.VISIBLE);
        tvEmptyConversationList.setVisibility(View.VISIBLE);
    }

    private void updateList(DataSnapshot dataSnapshot, boolean isNew, String userId) {
        progressBar.setVisibility(View.GONE);
        tvEmptyConversationList.setVisibility(View.GONE);
        Object unreadObject = dataSnapshot.child(NodeNames.UNREAD_COUNT).getValue();
        String unreadCount = unreadObject != null ? unreadObject.toString() : "0";
        databaseReferenceUsers.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Object nameObject = snapshot.child(NodeNames.NAME).getValue();
                Object photoObject = snapshot.child(NodeNames.PHOTO).getValue();
                Object lastMessageObject = snapshot.child(NodeNames.LAST_MESSAGE).getValue();
                Object lastMessageTimeObject = snapshot.child(NodeNames.LAST_MESSAGE_TIME).getValue();
                if (nameObject != null && photoObject != null && lastMessageObject != null && lastMessageTimeObject != null) {
                    String fullName = nameObject.toString();
                    String photoName = photoObject.toString();
                    String lastMessage = lastMessageObject.toString();
                    String lastMessageTime = lastMessageTimeObject.toString();
                    ConversationListModel conversationListModel = new ConversationListModel(userId, fullName, photoName, unreadCount, lastMessage, lastMessageTime);

                    if (isNew) {
                        conversationListModelList.add(conversationListModel);
                        userIds.add(userId);
                    } else {
                        int indexOfClickedUser = userIds.indexOf(userId);
                        conversationListModelList.set(indexOfClickedUser, conversationListModel);
                    }

                    conversationListAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getActivity(), getString(R.string.failed_to_fetch_conversation_list, error.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        query.removeEventListener(childEventListener);
    }
}