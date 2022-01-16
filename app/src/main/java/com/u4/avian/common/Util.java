package com.u4.avian.common;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.text.format.DateUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.u4.avian.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class Util {
    public static boolean connectionAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null && connectivityManager.getActiveNetworkInfo() != null) {
            return connectivityManager.getActiveNetworkInfo().isAvailable();
        }
        return false;
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void updateDeviceToken(Context context, String token) {
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
            DatabaseReference databaseReference = rootRef.child(NodeNames.TOKENS).child(currentUser.getUid());
            HashMap<String, String> hashMap = new HashMap<>();
            hashMap.put(NodeNames.DEVICE_TOKEN, token);
            databaseReference.setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (!task.isSuccessful()) {
                        Toast.makeText(context, context.getString(R.string.failed_to_fetch_device_token), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    public static void sendNotification(Context context, String title, String message, String messageType, String userId) {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        DatabaseReference tokenDatabaseReference = rootRef.child(NodeNames.TOKENS).child(userId);
        tokenDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Object tokenObject = snapshot.child(NodeNames.DEVICE_TOKEN).getValue();
                if (tokenObject != null) {
                    String deviceToken = tokenObject.toString();
                    JSONObject notification = new JSONObject();
                    JSONObject notificationData = new JSONObject();
                    try {
                        notificationData.put(Constants.NOTIFICATION_TITLE, title);
                        notificationData.put(Constants.NOTIFICATION_MESSAGE, message);
                        notificationData.put(Constants.NOTIFICATION_TYPE, messageType);
                        notification.put(Constants.NOTIFICATION_TO, deviceToken);
                        notification.put(Constants.NOTIFICATION_DATA, notificationData);
                        String fcmApiUrl = "https://fcm.googleapis.com/fcm/send";
                        String contentType = "application/json";

                        Response.Listener successListener = new Response.Listener() {
                            @Override
                            public void onResponse(Object response) {
                                Toast.makeText(context, context.getString(R.string.notification_sent), Toast.LENGTH_SHORT).show();
                            }
                        };

                        Response.ErrorListener errorListener = new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Toast.makeText(context, context.getString(R.string.failed_to_send_notification, error.getMessage()), Toast.LENGTH_SHORT).show();
                            }
                        };

                        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(1, fcmApiUrl, notification, successListener, errorListener) {
                            @Override
                            public Map<String, String> getHeaders() throws AuthFailureError {
                                Map<String, String> params = new HashMap<>();
                                params.put("Authorization", "key=" + Constants.FIREBASE_SERVER_KEY);
                                params.put("Sender", "id=" + Constants.FIREBASE_SENDER_ID);
                                params.put("Content-Type", contentType);
                                return params;
                            }
                        };

                        RequestQueue requestQueue = Volley.newRequestQueue(context);
                        requestQueue.add(jsonObjectRequest);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(context, context.getString(R.string.failed_to_send_notification, e.getMessage()), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, context.getString(R.string.failed_to_send_notification, error.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void updateChatDetails(Context context, String currentUserId, String chatUserId, String lastMessage) {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        DatabaseReference conversationRef = rootRef.child(NodeNames.CONVERSATIONS).child(chatUserId).child(currentUserId);
        conversationRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String currentCount = "0";
                Object unreadCountObject = snapshot.child(NodeNames.UNREAD_COUNT).getValue();
                if (unreadCountObject != null) {
                    currentCount = unreadCountObject.toString();
                }

                Map<String, Object> conversationMap = new HashMap<>();
                conversationMap.put(NodeNames.TIMESTAMP, ServerValue.TIMESTAMP);
                conversationMap.put(NodeNames.UNREAD_COUNT, Integer.parseInt(currentCount) + 1);
                conversationMap.put(NodeNames.LAST_MESSAGE, lastMessage);
                conversationMap.put(NodeNames.LAST_MESSAGE_TIME, ServerValue.TIMESTAMP);

                Map<String, Object> chatUserMap = new HashMap<>();
                chatUserMap.put(NodeNames.CONVERSATIONS + "/" + chatUserId + "/" + currentUserId, conversationMap);

                rootRef.updateChildren(chatUserMap, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                        if (error != null) {
                            Toast.makeText(context, context.getString(R.string.generic_error, error.getMessage()), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, context.getString(R.string.generic_error, error.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static String getTimeAgo(String time) {
        if (!time.equals("")) {
            CharSequence relativeTimeSpanString = DateUtils.getRelativeTimeSpanString(Long.parseLong(time));
            return relativeTimeSpanString.toString();
        }
        return null;
    }
}
