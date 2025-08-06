package com.example.myapplication;

//import static com.example.myapplication.Manifest.*;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.icu.number.Scale;
import android.media.MediaPlayer;
import android.Manifest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chibde.visualizer.BarVisualizer;
import com.jgabrielfreitas.core.BlurImageView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import de.hdodenhof.circleimageview.CircleImageView;
import jp.wasabeef.recyclerview.adapters.ScaleInAnimationAdapter;

public class MainActivity extends AppCompatActivity {
    //attributes
    RecyclerView recyclerView;
    SongAdapter songAdapter;
    List<Songs> allSongs = new ArrayList<>();
    ActivityResultLauncher<String> storagePermissionLauncher;
    final String permission = Manifest.permission.READ_EXTERNAL_STORAGE;
    ExoPlayer player;
    ActivityResultLauncher<String> recordAudioPermissionLauncher;
    final String recordAudioPermission = Manifest.permission.RECORD_AUDIO;
    TextView playerClosedBtn;
    //controls
    TextView songNameView,skipPreviousBtn,skipNextBtn,playPauseBtn, repeatModeBtn,playListBtn;
    TextView homeSongNameView,homeSkipPreviousBtn, homePlayPauseBtn,homeSkipNextBtn;

    //wrappers
    ConstraintLayout homeControlWrapper, artworkWrapper, seekbarWrapper, controlWrapper,audioVisualizerWrapper,bannerWrapperView;
    //artwork
    CircleImageView artWorkView;
    SeekBar seekbar;
    View playerView;
    TextView progressView, durationView;
    BarVisualizer audioVisualizer;
    BlurImageView blurImageView;
    int defaultStatusColor;
    int repeatMode = 1; //repeat all =1 repeat one =2 and shuffle all =3

    //is the activity bound to service?
    boolean isBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //save the status color
        defaultStatusColor = getWindow().getStatusBarColor();
        //set navigation color
        getWindow().setNavigationBarColor(ColorUtils.setAlphaComponent(defaultStatusColor, 199));


        //setting toolbar and app title
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle(getResources().getString(R.string.app_name));

        recyclerView = findViewById(R.id.recyclerView);
        storagePermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) {
                fetchSongs();
            } else {
                userResponse();
            }
        });

        //launch storage permission on create
        //storagePermissionLauncher.launch(permission); we will launch this after the binding

        //launch record audio permission
        recordAudioPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted && player.isPlaying()) {
                activateAudiovisualizer();
            } else {
                userResponseOnRecordAudioPerm();
            }
        });

        //views
//        player = new ExoPlayer.Builder(this).build();
        playerView = findViewById(R.id.playerView);
        playerClosedBtn = findViewById(R.id.playerCloseBtn);
        songNameView = findViewById(R.id.songNameView);
        skipPreviousBtn = findViewById(R.id.skipPreviousBtn);
        skipNextBtn = findViewById(R.id.skipNextBtn);
        playPauseBtn = findViewById(R.id.playPauseBtn);
        repeatModeBtn = findViewById(R.id.repeatModeBtn);
        playListBtn = findViewById(R.id.playListBtn);

        homeSongNameView = findViewById(R.id.homeSongNameView);
        homeSkipPreviousBtn = findViewById(R.id.homeSkipPreviousButton);
        homeSkipNextBtn = findViewById(R.id.homeSkipNextNextButton);
        homePlayPauseBtn = findViewById(R.id.homePlayPauseButton);

        //wrappers
        homeControlWrapper = findViewById(R.id.homeControlWrapper);
//        headWrapper = findViewById(R.id.headWrapper);
        seekbarWrapper = findViewById(R.id.seekbarWrapper);
        artworkWrapper = findViewById(R.id.artworkWrapper);
        controlWrapper = findViewById(R.id.controlWrapper);
        audioVisualizerWrapper = findViewById(R.id.audioVisualzerWrapper);

        //artwork
        artWorkView = findViewById(R.id.artworkView);
        //seekbar
        seekbar = findViewById(R.id.seekbar);
        progressView = findViewById(R.id.progressView);
        durationView = findViewById(R.id.durationView);
        //audio visualizer
        audioVisualizer = findViewById(R.id.visualiser);
        //blur imageview
        blurImageView = findViewById(R.id.blurImageView);

        //player controls methood
        //playerControls();

        //bind to the player service, and do everything after the binding
        doBindService();
    }

    @OptIn(markerClass = UnstableApi.class) private void doBindService() {
        Intent playerServiceIntent = new Intent(this,PlayerServie.class);
        bindService(playerServiceIntent, playerServiceConnection, Context.BIND_AUTO_CREATE);
    }

    ServiceConnection playerServiceConnection = new ServiceConnection() {
        @OptIn(markerClass = UnstableApi.class) @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            //get the service instance
            PlayerServie.SeviceBinder binder = (PlayerServie.SeviceBinder) iBinder;
            player = binder.getPlayerService().player;
            isBound = true;

            //ready to show the songs
            storagePermissionLauncher.launch(permission);
            //call player controlls method
            playerControls();

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    @Override
    public void onBackPressed() {
        if(playerView.getVisibility() == View.VISIBLE)
            exitPlayerView();
        else
            super.onBackPressed();
        homeControlWrapper.setVisibility(View.VISIBLE);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //release the player
//        if(player.isPlaying()){
//            player.stop();
//        }
//        else{
//            player.release();
//        }
        doUnbindService();
    }

    private void doUnbindService() {
        if (isBound) {
            unbindService(playerServiceConnection);
            isBound = false;
        }
    }

    private void playerControls() {
        //song name marquee
        songNameView.setSelected(true);
        homeSongNameView.setSelected(true);

        //exit the player
        playerClosedBtn.setOnClickListener(view -> exitPlayerView());
        //open player view on home control wrapper click
        homeControlWrapper.setOnClickListener(view -> showPlayerView());

        //player listener
        player.addListener(new Player.Listener() {
            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                Player.Listener.super.onMediaItemTransition(mediaItem, reason);
                // show the playing song title
                assert mediaItem != null;
                songNameView.setText(mediaItem.mediaMetadata.title);
                homeSongNameView.setText(mediaItem.mediaMetadata.title);

                progressView.setText(getReadableTime((int)player.getCurrentPosition()));
                seekbar.setProgress((int)player.getCurrentPosition());
                seekbar.setMax((int)player.getDuration());
                durationView.setText(getReadableTime((int)player.getDuration()));
                playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.playcolor,0,0,0);
                homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.baseline_play_arrow_24,0,0,0);

                //showing the current artWork
                showCurrentArtwork();
                //update the progress position of a current playing song
                updatePlayerPositionProgress();
                //load the artwork animation
                artWorkView.setAnimation((loadRotation()));

                //set Visualizer
                activateAudiovisualizer();
                //cerate player view color
                updatePlayerColors();

                if(!player.isPlaying()){
                    player.play();
                }
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                Player.Listener.super.onPlaybackStateChanged(playbackState);
                if (playbackState == ExoPlayer.STATE_READY){
                    //set values to player views
                    songNameView.setText(Objects.requireNonNull(player.getCurrentMediaItem()).mediaMetadata.title);
                    homeSongNameView.setText(Objects.requireNonNull(player.getCurrentMediaItem()).mediaMetadata.title);
                    progressView.setText(getReadableTime((int)player.getCurrentPosition()));
                    durationView.setText(getReadableTime((int)player.getDuration()));
                    seekbar.setMax((int) player.getDuration());
                    seekbar.setProgress((int) player.getCurrentPosition());
                    playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.pausecolor,0,0,0);
                    homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.baseline_pause_24,0,0,0);

                    showCurrentArtwork();
                    //update the progress position of a current playing song
//                    updatePlayerPositionProgress();
                    //load the artwork animation
                    artWorkView.setAnimation((loadRotation()));
                    //set Visualizer
                    activateAudiovisualizer();
                    //create player view color
                    updatePlayerColors();
                }

                else{
                    playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.playcolor,0,0,0);
                    homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.baseline_play_arrow_24,0,0,0);
                }
            }
        });
        skipNextBtn.setOnClickListener(view -> skipToNextSong());
        homeSkipNextBtn.setOnClickListener(view -> skipToNextSong());

        skipPreviousBtn.setOnClickListener(view -> skipToPrevious());
        homeSkipPreviousBtn.setOnClickListener(view -> skipToPrevious());

        //play pause button
        playPauseBtn.setOnClickListener(view -> playPause());
        homePlayPauseBtn.setOnClickListener(view -> playPause());

        //seekbar listener
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progressValue = 0;
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                progressValue = seekBar.getProgress();

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if(player.getPlaybackState() == ExoPlayer.STATE_READY){
                    seekbar.setProgress(progressValue);
                    progressView.setText((getReadableTime(progressValue)));
                    player.seekTo(progressValue);
                }
            }
        });

        //repeat mode
        repeatModeBtn.setOnClickListener(view -> {
            if(repeatMode == 1){
                player.setRepeatMode(ExoPlayer.REPEAT_MODE_ONE);
                repeatMode = 2;
                repeatModeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.repeat_once,0,0,0);
            } else if (repeatMode == 2) {
                player.setShuffleModeEnabled(true);
                player.setRepeatMode(ExoPlayer.REPEAT_MODE_ALL);
                repeatMode = 3;
                repeatModeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.shuffle,0,0,0);
            } else if(repeatMode == 3){
                player.setRepeatMode(ExoPlayer.REPEAT_MODE_ALL);
                player.setShuffleModeEnabled(false);
                repeatMode = 1;
                repeatModeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.repeat,0,0,0);
            }
            updatePlayerColors();
        });
    }

    private void playPause() {
        if(player.isPlaying()){
            player.pause();
            playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.playcolor, 0,0,0);
            homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.baseline_play_arrow_24, 0,0,0);
            artWorkView.clearAnimation();
        }
        else{
            player.play();
            playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.pausecolor, 0,0,0);
            homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.baseline_pause_24, 0,0,0);
            artWorkView.startAnimation(loadRotation());
        }

        //update player colors
        updatePlayerColors();
    }

    private void skipToNextSong() {
        if (player.hasNextMediaItem()){
            player.seekToNext();
        }
    }

    private void skipToPrevious() {
        if (player.hasNextMediaItem()){
            player.seekToPrevious();
        }
    }

    private Animation loadRotation() {
        RotateAnimation rotateAnimation = new RotateAnimation(0,360,Animation.RELATIVE_TO_SELF,0.5f, Animation.RELATIVE_TO_SELF,0.5F);
        rotateAnimation.setInterpolator(new LinearInterpolator());
        rotateAnimation.setDuration(10000);
        rotateAnimation.setRepeatCount(Animation.INFINITE);
        return rotateAnimation;
    }

    private void updatePlayerPositionProgress() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (player.isPlaying()){
                    progressView.setText((getReadableTime((int) player.getCurrentPosition())));
                    seekbar.setProgress((int) player.getCurrentPosition());
                }
                //repeat calling message
                updatePlayerPositionProgress();

            }
        },1000);
    }

    private void showCurrentArtwork() {
        artWorkView.setImageURI(Objects.requireNonNull(player.getCurrentMediaItem()).mediaMetadata.artworkUri);
        if (artWorkView.getDrawable() == null){
            artWorkView.setImageResource(R.drawable.disc);
        }
    }

    String getReadableTime(int duration) {
        String time;
        int hrs = duration/(1000*60*60);
        int min = (duration%(1000*60*60))/(1000*60);
        int secs = (((duration%(1000*60*60))%(1000*60*60))%(1000*60))/1000;

        if(hrs<1){
            time = min + ":" + secs;
        }
        else{
            time = hrs + ":" + min + ":" + secs;
        }
        return time;
    }

    private void showPlayerView() {
        playerView.setVisibility(View.VISIBLE);
        updatePlayerColors();
    }

    private void updatePlayerColors() {
        if(playerView.getVisibility() == View.GONE)
            return;

        BitmapDrawable bitmapDrawable = (BitmapDrawable) artWorkView.getDrawable();
        if (bitmapDrawable == null){
            bitmapDrawable = (BitmapDrawable) ContextCompat.getDrawable(this,R.drawable.disc);
        }
            assert bitmapDrawable != null;
            Bitmap bmp = bitmapDrawable.getBitmap();

            //set bitmap to blur image
            blurImageView.setImageBitmap(bitmapDrawable.getBitmap());
            blurImageView.setBlur(4);

            //player control colors
        Palette.from(bmp).generate(palette -> {
            if(palette != null){
                Palette.Swatch vibrant = palette.getDarkVibrantSwatch();
                if(vibrant == null){
                    vibrant = palette.getMutedSwatch();
                    if(vibrant == null){
                        vibrant = palette.getVibrantSwatch();
                    }
                }

                //extract text colors
                assert vibrant != null;
                int titleColor = vibrant.getTitleTextColor();
                int bodyColor = vibrant.getBodyTextColor();
                int rgb = vibrant.getRgb();

                //set colors to the views
                //status and navigation bar colors
                getWindow().setStatusBarColor(rgb);
                getWindow().setNavigationBarColor(rgb);

                //more view colors
                songNameView.setTextColor(titleColor);
                homeSongNameView.setTextColor(titleColor);
                homePlayPauseBtn.setTextColor(titleColor);
                progressView.setTextColor(bodyColor);
                durationView.setTextColor(bodyColor);


                playerClosedBtn.getCompoundDrawables()[0].setTint(titleColor);
                repeatModeBtn.getCompoundDrawables()[0].setTint(bodyColor);
                skipNextBtn.getCompoundDrawables()[0].setTint(bodyColor);
                skipPreviousBtn.getCompoundDrawables()[0].setTint(bodyColor);
//                playPauseBtn.getCompoundDrawables()[0].setTint(titleColor);
                homeSkipNextBtn.getCompoundDrawables()[0].setTint(bodyColor);
                homeSkipPreviousBtn.getCompoundDrawables()[0].setTint(bodyColor);
                homePlayPauseBtn.getCompoundDrawables()[0].setTint(bodyColor);
                playListBtn.getCompoundDrawables()[0].setTint(bodyColor);

//                seekbar.getThumb().setTint(bodyColor);
//                seekbar.getProgressDrawable().setTint(bodyColor);

            }
        });
    }

    private void exitPlayerView() {
        homeControlWrapper.setVisibility(View.VISIBLE);
        playerView.setVisibility(View.GONE);
        getWindow().setStatusBarColor(defaultStatusColor);
        getWindow().setNavigationBarColor(ColorUtils.setAlphaComponent(defaultStatusColor,199));
    }

    private void userResponseOnRecordAudioPerm() {
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            if(shouldShowRequestPermissionRationale(recordAudioPermission)){
                //show an explanation UI explaining why we need this
                //use Alert Dialog
                new AlertDialog.Builder(this)
                        .setTitle("Requesting to show audio visualiser")
                        .setMessage("Allow Audio Visualizer to be displayed")
                        .setPositiveButton("allow", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //request the permission
                                recordAudioPermissionLauncher.launch(recordAudioPermission);
                            }
                        })
                        .setNegativeButton("no", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Toast.makeText(MainActivity.this, "audio visualizer won't be displayed", Toast.LENGTH_SHORT).show();
                                dialogInterface.dismiss();
                            }
                        })
                        .show();
            }
            else {
                Toast.makeText(MainActivity.this, "audio visualizer won't be displayed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //audio visualiser
    @SuppressLint("UnsafeOptInUsageError")
    private void activateAudiovisualizer() {
        //check if we have record audio permission to show visualizer
        if(ContextCompat.checkSelfPermission(this, recordAudioPermission) != PackageManager.PERMISSION_GRANTED){
            return;
        }

        //set color of audio visualizer
        audioVisualizer.setColor(ContextCompat.getColor(this,R.color.white));
        //set number of visualizer btn 10&256
        audioVisualizer.setDensity(100);
        //set the audio session id from the player
        audioVisualizer.setPlayer(player.getAudioSessionId());

    }

    @SuppressLint("ObsoleteSdkInt")
    private  void userResponse(){
        if(ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED){
            fetchSongs();
        }
        else if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            if (shouldShowRequestPermissionRationale(permission)){
                //show alertdialog to explain why this permission is needed
                new AlertDialog.Builder(this)
                        .setTitle("requesting permission")
                        .setMessage("allow this app to access files in your device")
                        .setPositiveButton("allow", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //request permission
                                storagePermissionLauncher.launch(permission);
                            }
                        })
                        .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Toast.makeText(getApplicationContext(),"you denied storage permission", Toast.LENGTH_LONG).show();
                                dialogInterface.dismiss();
                            }
                        })
                        .show();

            }

            else{
                Toast.makeText(getApplicationContext(),"you canceled showing songs", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void fetchSongs() {
        // Define a list to carry songs
        List<Songs> songs = new ArrayList<>();
        Uri mediaStoreUri;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mediaStoreUri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        } else {
            mediaStoreUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }

        // Define projection
        String[] projection = new String[]{
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.ALBUM_ID
        };

        // Ordering
        String songsOrder = MediaStore.Audio.Media.DATE_ADDED + " DESC";

        // Get songs
        try (Cursor cursor = getContentResolver().query(mediaStoreUri, projection, null, null, songsOrder)) {
            if (cursor != null && cursor.moveToFirst()) {
                // Catch cursor indices
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
                int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE);
                int albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);

                do {
                    // Get the values of a column of a given audio file
                    long id = cursor.getLong(idColumn);
                    String name = cursor.getString(nameColumn);
                    int duration = cursor.getInt(durationColumn);
                    int size = cursor.getInt(sizeColumn);
                    long albumId = cursor.getLong(albumIdColumn);

                    // Remove file extension safely
                    if (name.contains(".")) {
                        name = name.substring(0, name.lastIndexOf("."));
                    }

                    // Song URI
                    Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);

                    // Album artwork URI (check for valid album ID)
                    Uri albumArtUri = null;
                    if (albumId != 0) {
                        albumArtUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId);
                    }

                    // Convert size to MB and duration to minutes:seconds
                    String sizeInMB = String.format(Locale.getDefault(), "%.2f MB", (double) size / (1024 * 1024)); // Convert bytes to MB
                    String durationFormatted = String.format(Locale.getDefault(),"%02d:%02d",
                            TimeUnit.MILLISECONDS.toMinutes(duration) % TimeUnit.HOURS.toMinutes(1),
                            TimeUnit.MILLISECONDS.toSeconds(duration) % TimeUnit.MINUTES.toSeconds(1));

                    // Song item
                    Songs song = new Songs(name, uri, albumArtUri, sizeInMB, durationFormatted);

                    // Add song item to song list
                    songs.add(song);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            // Log the error and show a message to the user
            e.printStackTrace();
            Toast.makeText(this, "Error fetching songs: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        // Display songs
        showSongs(songs);
    }


    private void showSongs(List<Songs> songs) {
        if(songs.isEmpty()){ //
            Toast.makeText(this,"No songs",Toast.LENGTH_SHORT).show();
        }

        //save songs
        allSongs.clear();
        allSongs.addAll(songs);

        //update the app bar title to indicate no of songs
        String title = getResources().getString(R.string.app_name);
        Objects.requireNonNull(getSupportActionBar()).setTitle(title);

        //layout manager
//        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
//        recyclerView.setLayoutManager(layoutManager);

        //songs adapter
        songAdapter = new SongAdapter(this,songs,player, (ConstraintLayout) playerView);
        //set the adapter recyclerview
        recyclerView.setAdapter(songAdapter);

        // Create a LinearLayoutManager with smooth scrolling enabled
        LinearLayoutManager layoutManager = new LinearLayoutManager(this) {
            @Override
            public boolean canScrollVertically() {
                return true; // Allow vertical scrolling
            }

            @Override
            public boolean canScrollHorizontally() {
                return false; // Disable horizontal scrolling
            }
        };

// Enable smooth scrolling
        layoutManager.setSmoothScrollbarEnabled(true);

// Set the custom LinearLayoutManager to RecyclerView
        recyclerView.setLayoutManager(layoutManager);

        //recyclerView animation
//        ScaleInAnimationAdapter scaleInAnimationAdapter = new ScaleInAnimationAdapter(songAdapter);
//        scaleInAnimationAdapter.setDuration(500);
//        scaleInAnimationAdapter.setInterpolator(new AccelerateInterpolator());
//        scaleInAnimationAdapter.setFirstOnly(true); //this will make animation occur everytime you scroll.
//        recyclerView.setAdapter(scaleInAnimationAdapter);

    }

    //setting the menu/ search bar

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search, menu);

        //search logic
        MenuItem menuItem = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) menuItem.getActionView();
        //call searchSong method
        assert searchView != null;
        searchSong(searchView);
        return super.onCreateOptionsMenu(menu);
    }

    private void searchSong(SearchView searchView) {
        //searchView listener
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterSongs(newText.toLowerCase());
                return true;
            }
        });
    }

    private void filterSongs(String query) {
        List<Songs> filteredSongs = new ArrayList<>();
        if (!allSongs.isEmpty()){
            for (Songs song: allSongs){
                if(song.getTitle().toLowerCase().contains(query)){
                    filteredSongs.add(song);
                }
            }

            if(songAdapter != null){
                songAdapter.filterSongs(filteredSongs);
            }
        }
    }
}