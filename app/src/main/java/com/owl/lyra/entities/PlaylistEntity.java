package com.owl.lyra.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import org.jetbrains.annotations.NotNull;

@Entity(tableName = "tbl_playlists")
public class PlaylistEntity {
    @NotNull
    @PrimaryKey
    public String PlaylistID;
    @NotNull
    public String ParseSource;
    @NotNull
    public String Title;
    @NotNull
    public int TotalTracks;
    @NotNull
    public String LastUpdated;

    public PlaylistEntity(String PlaylistID, String ParseSource, String Title, int TotalTracks, String LastUpdated) {
        this.PlaylistID = PlaylistID;
        this.ParseSource = ParseSource;
        this.Title = Title;
        this.TotalTracks = TotalTracks;
        this.LastUpdated = LastUpdated;
    }

}
