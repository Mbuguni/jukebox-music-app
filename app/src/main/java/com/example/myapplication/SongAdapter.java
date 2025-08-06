package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    //members
    Context context;
    List<Songs> songs;
    ExoPlayer player;
    ConstraintLayout playerView;
    // Add a new member to hold the filtered list
    private List<Songs> currentSongs;

    public SongAdapter(Context context, List<Songs> songs, ExoPlayer player, ConstraintLayout playerView) {
        this.context = context;
        this.songs = songs;
        this.player = player;
        this.playerView = playerView;
        // Initialize currentSongs with the original list
        this.currentSongs = new ArrayList<>(songs);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        //inflate song row item layout
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.song_row_item, parent, false);
        return new SongViewHolder(view);
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        //current song and view holder
        Songs song = currentSongs.get(position); // Use currentSongs here
        SongViewHolder viewHolder = (SongViewHolder) holder;

        //set values for views
        viewHolder.titleHolder.setText(song.getTitle());
        viewHolder.durationHolder.setText(String.valueOf(song.getDuration()));
        viewHolder.sizeHolder.setText(String.valueOf(song.getSize()));

        //setting the art work
        Uri artWork = song.getArtWordUri();

        if (artWork != null) {
            //set uri to image view
            viewHolder.artworkHolder.setImageURI(artWork);

            if (viewHolder.artworkHolder.getDrawable() == null) {
                viewHolder.artworkHolder.setImageResource(R.drawable.album_place_holder2);
            }
        }

        //when the song is clicked
        viewHolder.itemView.setOnClickListener(view -> {
            //start the playBack service
            context.startService(new Intent(context.getApplicationContext(), PlayerServie.class));
            //show player view
            playerView.setVisibility(View.VISIBLE);
            //playing the song
            List<MediaItem> mediaItems = getMediaItems(currentSongs);
            player.setMediaItems(mediaItems, 0, 0);
            player.seekTo(position,0);
            //prepare and play
            player.prepare();
            player.play();
            Toast.makeText(context, song.getTitle(), Toast.LENGTH_SHORT).show();

            //check if the record audio permission is granted, hence request
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                //record the record audio permission
                ((MainActivity) context).recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
            }

        });
    }

    private List<MediaItem> getMediaItems(List<Songs> songsList) {
        //define a list of media items
        List<MediaItem> mediaItems = new ArrayList<>();

        // Use currentSongs here instead of songs
        for (Songs song : songsList) {
            MediaItem mediaItem = new MediaItem.Builder()
                    .setUri(song.getUri())
                    .setMediaMetadata(getMetaData(song))
                    .build();

            //add medaiitem to media items list
            mediaItems.add(mediaItem);
        }
        return mediaItems;
    }

    private MediaMetadata getMetaData(Songs song) {
        return new MediaMetadata.Builder()
                .setTitle(song.getTitle())
                .setArtworkUri(song.getArtWordUri())
                .build();
    }

    public static class SongViewHolder extends RecyclerView.ViewHolder {
        //members
        ImageView artworkHolder;
        TextView titleHolder, durationHolder, sizeHolder;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);

            artworkHolder = itemView.findViewById(R.id.artWorkView);
            titleHolder = itemView.findViewById(R.id.titleView);
            durationHolder = itemView.findViewById(R.id.durationView);
            sizeHolder = itemView.findViewById(R.id.sizeView);
        }
    }

    @Override
    public int getItemCount() {
        return currentSongs.size(); // Use currentSongs here
    }

    //searching songs method
    @SuppressLint("NotifyDataSetChanged")
    public void filterSongs(List<Songs> filtered_list) {
        currentSongs = filtered_list; // Update currentSongs with the filtered list
        notifyDataSetChanged();
    }
}