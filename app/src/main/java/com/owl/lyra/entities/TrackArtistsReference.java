package com.owl.lyra.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;

import org.jetbrains.annotations.NotNull;

@Entity(primaryKeys = {"TrackID", "ArtistID"})
public class TrackArtistsReference {
    @NotNull
    @ColumnInfo(index = true)
    public String ArtistID;
    @NotNull
    public String TrackID;
}
