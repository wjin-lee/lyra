package com.owl.lyra.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;

import org.jetbrains.annotations.NotNull;

@Entity(primaryKeys = {"PlaylistID", "TrackID"})
public class PlaylistTracksReference {
    @NotNull
    @ColumnInfo(index = true)
    public String PlaylistID;
    @NotNull
    public String TrackID;
}
