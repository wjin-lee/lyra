package com.owl.lyra.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import com.owl.lyra.entities.AlbumEntity;
import com.owl.lyra.entities.TrackArtistsEntity;
import com.owl.lyra.entities.TrackEntity;

import java.util.List;

@Dao
public interface TrackDao {

    @Insert()
    void addTrack(TrackEntity trackEntity);

    @Query("UPDATE tbl_tracks SET VideoURL=:url, LastModified=datetime('now') WHERE TrackID=:trackId")
    void updateVideoURL(String trackId, String url);

    @Query("SELECT * FROM tbl_tracks")
    List<TrackEntity> getAllTracks();

    @Query("SELECT * FROM tbl_tracks WHERE TrackID=:trackId LIMIT 1")
    TrackEntity getTrack(String trackId);

    @Transaction
    @Query("SELECT * FROM tbl_tracks WHERE TrackID=:trackId")
    TrackArtistsEntity getTrackArtists(String trackId);

    @Transaction
    @Query("SELECT * FROM tbl_albums WHERE AlbumID=:albumId LIMIT 1")
    AlbumEntity getTrackAlbum(String albumId);





}
