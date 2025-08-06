package com.example.myapplication;

import static android.app.NotificationManager.IMPORTANCE_HIGH;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Binder;
import android.os.IBinder;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.Player;
import androidx.media3.common.util.NotificationUtil;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerNotificationManager;

import java.util.Objects;

@UnstableApi public class PlayerServie extends Service {
    //members
    private final IBinder serviceBinder = new SeviceBinder();
    //player
    ExoPlayer player;
    PlayerNotificationManager notificationManager;

    //class for binder clients
    public class SeviceBinder extends Binder {
        public PlayerServie getPlayerService(){
            return PlayerServie.this;
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return serviceBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //assign variables
        player = new ExoPlayer.Builder(getApplicationContext()).build();

        //audio focus attributes
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build();

        player.setAudioAttributes(audioAttributes,true);

        //notification manager
        final String channeLid = getResources().getString(R.string.app_name)+"Music Channel";
        final int notificationId = 1111111;
        notificationManager = new PlayerNotificationManager.Builder(this,notificationId,channeLid)
                .setNotificationListener(notificationListener)
                .setMediaDescriptionAdapter(descriptionAdapter)
                .setChannelImportance(IMPORTANCE_HIGH)
                .setSmallIconResourceId(R.drawable.icon)
                .setChannelDescriptionResourceId(R.string.app_name)
                .setNextActionIconResourceId(R.drawable.baseline_fast_forward_24)
                .setPreviousActionIconResourceId(R.drawable.baseline_fast_rewind_24)
                .setPauseActionIconResourceId(R.drawable.baseline_pause_24)
                .setPlayActionIconResourceId(R.drawable.baseline_play_arrow_24)
                .setChannelNameResourceId(R.string.app_name)
                .setChannelDescriptionResourceId(R.string.app_name)
                .build();

        //set player to notification manager
        notificationManager.setPlayer(player);
        notificationManager.setPriority(NotificationCompat.PRIORITY_MAX);
        notificationManager.setUseRewindAction(false);
        notificationManager.setUseFastForwardAction(false);
        notificationManager.setUseNextAction(true);
        notificationManager.setUsePreviousAction(true);

    }

    @Override
    public void onDestroy() {
        //release the player
        if (player.isPlaying())
            player.stop();
        notificationManager.setPlayer(null);
        player.release();
        player = null;
        stopForeground(true);
        stopSelf();
        super.onDestroy();
    }

    //notification listener
    PlayerNotificationManager.NotificationListener notificationListener = new PlayerNotificationManager.NotificationListener() {
        @Override
        public void onNotificationCancelled(int notificationId, boolean dismissedByUser) {
            PlayerNotificationManager.NotificationListener.super.onNotificationCancelled(notificationId, dismissedByUser);
            stopForeground(true);
            if(player.isPlaying()){
                player.pause();
            }
        }

        @SuppressLint("ForegroundServiceType")
        @Override
        public void onNotificationPosted(int notificationId, Notification notification, boolean ongoing) {
            PlayerNotificationManager.NotificationListener.super.onNotificationPosted(notificationId, notification, ongoing);
            startForeground(notificationId,notification);
        }
    };

    //notification description adapter
    PlayerNotificationManager.MediaDescriptionAdapter descriptionAdapter =new PlayerNotificationManager.MediaDescriptionAdapter() {
        @Override
        public CharSequence getCurrentContentTitle(Player player) {
            return Objects.requireNonNull(player.getCurrentMediaItem()).mediaMetadata.title;
        }

        @Nullable
        @Override
        public PendingIntent createCurrentContentIntent(Player player) {
            //intent to open app when notification is clicked
            Intent intent = new Intent(getApplicationContext(),MainActivity.class);
            return PendingIntent.getActivity(getApplicationContext(),0,intent,PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        }

        @Nullable
        @Override
        public CharSequence getCurrentContentText(Player player) {
            return null;
        }

        @Nullable
        @Override
        public Bitmap getCurrentLargeIcon(Player player, PlayerNotificationManager.BitmapCallback callback) {
            //try creating an Image view on the fly then get its drawable
            ImageView view = new ImageView(getApplicationContext());
            view.setImageURI(Objects.requireNonNull(player.getCurrentMediaItem()).mediaMetadata.artworkUri);
            view.buildDrawingCache();

            //change the view to bitmap
            BitmapDrawable bitmapDrawable = (BitmapDrawable) view.getDrawable();
            //check if the bitmap is null
            if(bitmapDrawable == null){
                bitmapDrawable = (BitmapDrawable) ContextCompat.getDrawable(getApplicationContext(),R.drawable.album_place_holder2);
            }
            assert bitmapDrawable != null;
            return bitmapDrawable.getBitmap();
        }
    };
}