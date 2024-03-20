package com.owl.lyra.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import org.jetbrains.annotations.NotNull;

@Entity(tableName = "tbl_tracks")
public class TrackEntity {
    @PrimaryKey
    @NotNull
    public String TrackID;
    @NotNull
    public String ParseSource;
    @NotNull
    public String Title;
    @NotNull
    public String LastModified;
    @NotNull
    public String AlbumID;
    public String VideoURL;

    public TrackEntity(String TrackID, String ParseSource, String AlbumID, String Title, String VideoURL, String LastModified) {
        this.TrackID = TrackID;
        this.ParseSource = ParseSource;
        this.AlbumID = AlbumID;
        this.Title = Title;
        this.VideoURL = VideoURL;
        this.LastModified = LastModified;
    }

//    public Track toTrack() {
//        // public String id;
//        //    public String parseSource;
//        //    public String name;
//        //    public int duration_ms;
//        //    public Album album;
//        //    public ArrayList<Artist> artists;
//        //    public String videoURL;
//        //    public String lastModified;
//
//
//        // Neccesary
//        //        id = trackId;
//        //        name = trackName;
//        //        album = trackAlbum;
//        //        artists = trackArtists;
//        //        videoURL = trackVideoURL;
//        //        lastModified = lastModifiedDate;
//    }

}
