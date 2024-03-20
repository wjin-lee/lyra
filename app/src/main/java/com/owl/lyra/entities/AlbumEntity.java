package com.owl.lyra.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import org.jetbrains.annotations.NotNull;

@Entity(tableName = "tbl_albums")
public class AlbumEntity {
    @NotNull
    @PrimaryKey
    public String AlbumID;
    @NotNull
    public String ParseSource;
    @NotNull
    public String Title;
    public String AlbumArtURL;

    public AlbumEntity(String AlbumID, String ParseSource, String Title, String AlbumArtURL) {
        this.AlbumID = AlbumID;
        this.ParseSource = ParseSource;
        this.Title = Title;
        this.AlbumArtURL = AlbumArtURL;
    }

}
