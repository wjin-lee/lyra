package com.owl.lyra.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.owl.lyra.entities.ArtistEntity;

import java.util.List;

@Dao
public interface ArtistDao {
    @Insert()
    void addArtist(ArtistEntity artistEntity);

    @Query("SELECT * FROM tbl_artists")
    List<ArtistEntity> getAllArtists();

    @Query("SELECT * FROM tbl_artists WHERE ArtistID=:artistId LIMIT 1")
    ArtistEntity getArtist(String artistId);


}
