package com.owl.lyra.objects;

public class Playlist {
    public String id;
    public String name;
    public String parseSource;
    public TrackList tracks;

    // Getters
    public String getName() {
        return this.name;
    }

    // Setters

    public void setParseSource(String parseSource) {
        this.parseSource = parseSource;
    }
}

