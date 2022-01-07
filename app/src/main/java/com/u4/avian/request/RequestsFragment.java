package com.u4.avian.request;

import static com.u4.avian.common.Constants.REQUEST_STATUS_RECEIVED;
import static com.u4.avian.common.NodeNames.NAME;
import static com.u4.avian.common.NodeNames.PHOTO;
import static com.u4.avian.common.NodeNames.REQUESTS;
import static com.u4.avian.common.NodeNames.REQUEST_TYPE;
import static com.u4.avian.common.NodeNames.USERS;

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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.u4.avian.R;

import java.util.ArrayList;
import java.util.List;

public class RequestsFragment extends Fragment {

    private RequestAdapter requestAdapter;
    private List<RequestModel> requestModelList;
    private TextView tvEmptyRequestsList;
    private DatabaseReference databaseReferenceUsers;
    private View progressBar;

    public RequestsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_requests, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView rvRequests = view.findViewById(R.id.rvRequests);
        tvEmptyRequestsList = view.findViewById(R.id.tvEmptyRequests);
        progressBar = view.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        rvRequests.setLayoutManager(new LinearLayoutManager(getActivity()));
        requestModelList = new ArrayList<>();
        requestAdapter = new RequestAdapter(getActivity(), requestModelList);
        rvRequests.setAdapter(requestAdapter);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        databaseReferenceUsers = FirebaseDatabase.getInstance().getReference().child(USERS);
        DatabaseReference databaseReferenceRequests = FirebaseDatabase.getInstance().getReference().child(REQUESTS).child(currentUser.getUid());

        databaseReferenceRequests.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBar.setVisibility(View.GONE);
                requestModelList.clear();
                if (snapshot.getChildrenCount() == 0) {
                    tvEmptyRequestsList.setVisibility(View.VISIBLE);
                    requestAdapter.notifyDataSetChanged();
                    return;
                }
                for (DataSnapshot ds : snapshot.getChildren()) {
                    if (ds.exists()) {
                        Object requestTypeObject = ds.child(REQUEST_TYPE).getValue();
                        if (requestTypeObject != null) {
                            String requestType = requestTypeObject.toString();
                            if (requestType.equals(REQUEST_STATUS_RECEIVED)) {
                                String userId = ds.getKey();
                                if (userId != null) {
                                    databaseReferenceUsers.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            Object nameObject = snapshot.child(NAME).getValue();
                                            Object photoObject = snapshot.child(PHOTO).getValue();
                                            if (nameObject != null) {
                                                String userName = nameObject.toString();
                                                String photoName = "";
                                                if (photoObject != null) {
                                                    photoName = photoObject.toString();
                                                }
                                                RequestModel requestModel = new RequestModel(userId, userName, photoName);
                                                requestModelList.add(requestModel);
                                                tvEmptyRequestsList.setVisibility(View.GONE);
                                                requestAdapter.notifyDataSetChanged();
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            progressBar.setVisibility(View.GONE);
                                            Toast.makeText(getActivity(), getString(R.string.failed_to_fetch_requests, error.getMessage()), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getActivity(), getString(R.string.failed_to_fetch_requests, error.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }
}