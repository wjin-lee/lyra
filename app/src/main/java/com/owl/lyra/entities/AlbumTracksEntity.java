package com.owl.lyra.entities;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.owl.lyra.objects.Album;

import java.util.List;

public class AlbumTracksEntity {
    @Embedded
    public Album album;
    @Relation(
            parentColumn = "AlbumID",
            entityColumn = "AlbumID"
    )
    public List<TrackEntity> tracks;
}
