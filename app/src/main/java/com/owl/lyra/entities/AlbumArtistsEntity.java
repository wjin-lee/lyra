package com.owl.lyra.entities;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import java.util.List;

public class AlbumArtistsEntity {
    @Embedded
    public AlbumEntity album;
    @Relation(
            parentColumn = "AlbumID",
            entityColumn = "ArtistID",
            associateBy = @Junction(AlbumArtistsReference.class)
    )
    public List<ArtistEntity> artists;
}
