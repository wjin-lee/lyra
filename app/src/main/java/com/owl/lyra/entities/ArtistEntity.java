package com.owl.lyra.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.owl.lyra.objects.Artist;

import org.jetbrains.annotations.NotNull;

@Entity(tableName = "tbl_artists")
public class ArtistEntity {
    @NotNull
    @PrimaryKey
    public String ArtistID;
    @NotNull
    public String ParseSource;
    @NotNull
    public String Name;

    public ArtistEntity(String ArtistID, String ParseSource, String Name) {
        this.ArtistID = ArtistID;
        this.ParseSource = ParseSource;
        this.Name = Name;
    }

    public Artist toArtist() {
        return new Artist(this.ArtistID, this.Name, this.ParseSource);
    }

}
