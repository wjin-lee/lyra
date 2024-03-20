package com.owl.lyra.entities;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import java.util.List;

public class PlaylistTracksEntity {
    @Embedded
    public PlaylistEntity playlist;
    @Relation(
            parentColumn = "PlaylistID",
            entityColumn = "TrackID",
            associateBy = @Junction(PlaylistTracksReference.class)
    )
    public List<TrackEntity> tracks;
}