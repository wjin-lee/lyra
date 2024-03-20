package com.owl.lyra.objects;

import com.owl.lyra.services.TimeService;
import com.owl.lyra.entities.TrackEntity;

import java.util.ArrayList;

public class Track {
    public String id;
    public String parseSource;
    public String name;
    public int duration_ms;
    public Album album;
    public ArrayList<Artist> artists;
    public String videoURL;
    public int variation;
    public String lastModified;
    // This is mostly just used in the download step.
    public String status;
    public String cacheFileExt;
    public long downloadId;



    public Track(String trackId, String trackName, Album trackAlbum, ArrayList<Artist> trackArtists, String trackVideoURL, String lastModifiedDate) {
        id = trackId;
        name = trackName;
        album = trackAlbum;
        artists = trackArtists;
        videoURL = trackVideoURL;
        lastModified = lastModifiedDate;
    }


    public TrackEntity toDatabaseEntity() {
        if (lastModified.equals("")) {
            lastModified = TimeService.now();
        }

        return new TrackEntity(id, parseSource, album.getId(), name, videoURL, lastModified);
    }

//    public Track(String trackId, String trackName, int duration, Album trackAlbum, ArrayList<Artist> trackArtists) {
//        id = trackId;
//        name = trackName;
//        duration_ms = duration;
//        album = trackAlbum;
//        artists = trackArtists;
//    }

    // Getters
    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public int getDuration_ms() {
        return this.duration_ms;
    }

    public String getVideoURL() {
        return videoURL;
    }

    public String getStatus() {
        return status;
    }

    public String getCacheFileExt() {
        return cacheFileExt;
    }

    public String getLastModified() {return this.lastModified;}

    public String getArtistString() {
        String artistString = "";
        for (Artist artist : artists) {
            artistString += artist.name+", ";
        }

        return artistString.substring(0, artistString.length()-2);
    }

    public long getDownloadId() {
        return downloadId;
    }

    public Album getAlbum() {
        return album;
    }

    // Setters

    public void setVariation(int variation) {
        this.variation = variation;
    }

    public void setName(String newName) {
        this.name = newName;
    }

    public void setParseSource(String source) {this.parseSource = source;}

    public void setVideoURL(String url) {this.videoURL = url;}

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCacheFileExt(String cacheFileExt) {
        this.cacheFileExt = cacheFileExt;
    }

    public void setDownloadId(long downloadId) {
        this.downloadId = downloadId;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    //    public void setVideo(String videoID, String videoTitle, int videoDuration) {
//        mVideoID = videoID;
//        mVideoTitle = videoTitle;
//        mVideoDuration = videoDuration;
//    }


}
