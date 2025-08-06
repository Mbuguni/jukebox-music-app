package com.example.myapplication;

import android.net.Uri;

public class Songs {
    //members
    String title;
    Uri uri;//for song name
    Uri artWordUri;
    String size;
    String duration;

    //constructor

    public Songs(String title, Uri uri, Uri artWordUri, String size, String duration) {
        this.title = title;
        this.uri = uri;
        this.artWordUri = artWordUri;
        this.size = size;
        this.duration = duration;
    }

    //getters

    public String getTitle() {
        return title;
    }

    public Uri getUri() {
        return uri;
    }

    public Uri getArtWordUri() {
        return artWordUri;
    }

    public String  getSize() {
        return size;
    }

    public String getDuration() {
        return duration;
    }
}
