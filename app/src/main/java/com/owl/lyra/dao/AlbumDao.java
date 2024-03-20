package com.owl.lyra.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import com.owl.lyra.entities.AlbumArtistsEntity;
import com.owl.lyra.entities.AlbumEntity;

import java.util.List;

@Dao
public interface AlbumDao {
    @Insert()
    void addAlbum(AlbumEntity albumEntity);

    @Query("SELECT * FROM tbl_albums")
    List<AlbumEntity> getAllAlbums();

    @Query("SELECT * FROM tbl_albums WHERE AlbumID=:albumId LIMIT 1")
    AlbumEntity getAlbum(String albumId);

    @Transaction
    @Query("SELECT * FROM tbl_albums WHERE AlbumID=:albumId")
    AlbumArtistsEntity getAlbumArtists(String albumId);
}
