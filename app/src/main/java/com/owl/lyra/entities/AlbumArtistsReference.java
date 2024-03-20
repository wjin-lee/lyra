package com.owl.lyra.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;

import org.jetbrains.annotations.NotNull;

@Entity(primaryKeys = {"AlbumID", "ArtistID"})
public class AlbumArtistsReference {
    @NotNull
    @ColumnInfo(index = true)
    public String ArtistID;
    @NotNull
    public String AlbumID;
}
