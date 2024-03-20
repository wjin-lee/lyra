package com.owl.lyra.objects;

import java.util.ArrayList;

public class Album {
    public String id;
    public String name;
    public String parseSource;
    public ArrayList<AlbumArt> images;
    public ArrayList<Artist> artists;
    // Possibly unused
    public TrackList tracks;

    public Album(String id, String name, String parseSource, ArrayList<AlbumArt> images, ArrayList<Artist> artists) {
        this.id = id;
        this.name = name;
        this.parseSource = parseSource;
        this.images = images;
        this.artists = artists;
    }


    // Getters
    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getAlbumArtURL() {
        if (images.size() == 1) {
            return images.get(0).url;
        }
        else{
            return images.get(1).url;
        }
    }

    // Setters
    public void setName(String newName) {
        this.name = newName;
    }

    public void setParseSource(String parseSource) {
        this.parseSource = parseSource;
    }
}
