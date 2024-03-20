package com.owl.lyra.entities;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import java.util.List;

public class TrackArtistsEntity {
    @Embedded
    public TrackEntity track;
    @Relation(
            parentColumn = "TrackID",
            entityColumn = "ArtistID",
            associateBy = @Junction(TrackArtistsReference.class)
    )
    public List<ArtistEntity> artists;
}
