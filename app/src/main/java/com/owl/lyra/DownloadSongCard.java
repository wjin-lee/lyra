package com.owl.lyra;

public class DownloadSongCard {
    private String mTitle;
    private String mArtists;
    private String mAlbum;
    private String mAlbumArtURL;
    private String mVideoURL;
    private int mVariation;
    private String mStatus;

    public DownloadSongCard(String trackTitle, String trackArtists, String trackAlbum, String albumArtURL, String videoURL, int variation, String status){
        mTitle = trackTitle;
        mArtists = trackArtists;
        mAlbum = trackAlbum;
        mAlbumArtURL = albumArtURL;
        mVideoURL = videoURL;
        mVariation = variation;
        mStatus = status;
    }

    public void setVideoURL(String mVideoURL) {
        this.mVideoURL = mVideoURL;
    }

    public void setVariation(int mVariation) {
        this.mVariation = mVariation;
    }

    public void setStatus(String mStatus) {
        this.mStatus = mStatus;
    }

    public String getTrackTitle() {
        return mTitle;
    }

    public String getTrackArtists() {
        return mArtists;
    }

    public String getAlbum() {
        return mAlbum;
    }

    public String getAlbumArtURL() {
        return mAlbumArtURL;
    }

    public String getVideoURL() {
        return mVideoURL;
    }

    public int getVariation() {
        return mVariation;
    }

    public String getStatus() {
        return mStatus;
    }
}
