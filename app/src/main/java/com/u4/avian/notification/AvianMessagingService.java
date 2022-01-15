package com.u4.avian.notification;

import static com.u4.avian.common.Constants.MESSAGE_TYPE_IMAGE;
import static com.u4.avian.common.Constants.MESSAGE_TYPE_VIDEO;
import static com.u4.avian.common.Util.updateDeviceToken;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.u4.avian.R;
import com.u4.avian.common.Constants;
import com.u4.avian.login.LoginActivity;

public class AvianMessagingService extends FirebaseMessagingService {
    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        updateDeviceToken(this, s);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = remoteMessage.getData().get(Constants.NOTIFICATION_TITLE);
        String message = remoteMessage.getData().get(Constants.NOTIFICATION_MESSAGE);
        String messageType = remoteMessage.getData().get(Constants.NOTIFICATION_TYPE);

        Intent intentConversation = new Intent(this, LoginActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intentConversation, PendingIntent.FLAG_ONE_SHOT);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(Constants.NOTIFICATION_CHANNEL_ID, Constants.NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(Constants.NOTIFICATION_CHANNEL_DESCRIPTION);
            notificationManager.createNotificationChannel(channel);
            notificationBuilder = new NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID);
        } else {
            notificationBuilder = new NotificationCompat.Builder(this);
        }

        notificationBuilder.setSmallIcon(R.drawable.ic_eagle);
        notificationBuilder.setColor(getResources().getColor(R.color.red_500));
        notificationBuilder.setContentTitle(title);
        notificationBuilder.setAutoCancel(true);
        notificationBuilder.setSound(defaultSoundUri);
        notificationBuilder.setContentIntent(pendingIntent);

        if (messageType != null && (messageType.equals(MESSAGE_TYPE_VIDEO) || messageType.equals(MESSAGE_TYPE_IMAGE))) {
            NotificationCompat.BigPictureStyle bigPictureStyle = new NotificationCompat.BigPictureStyle();
            Glide.with(this)
                    .asBitmap()
                    .load(message)
                    .into(new CustomTarget<Bitmap>(200, 100) {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            bigPictureStyle.bigPicture(resource);
                            notificationBuilder.setStyle(bigPictureStyle);
                            notificationManager.notify(666, notificationBuilder.build());
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {

                        }
                    });
            notificationBuilder.setContentText(messageType.equals(MESSAGE_TYPE_VIDEO) ? "New video received" : "New image received");
        } else {
            notificationBuilder.setContentText(message);
            notificationManager.notify(666, notificationBuilder.build());
        }
    }
}