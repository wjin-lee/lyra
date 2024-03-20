package com.owl.lyra;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.owl.lyra.dao.AlbumDao;
import com.owl.lyra.dao.ArtistDao;
import com.owl.lyra.dao.TrackDao;
import com.owl.lyra.entities.AlbumArtistsReference;
import com.owl.lyra.entities.AlbumEntity;
import com.owl.lyra.entities.ArtistEntity;
import com.owl.lyra.entities.PlaylistEntity;
import com.owl.lyra.entities.PlaylistTracksReference;
import com.owl.lyra.entities.TrackArtistsReference;
import com.owl.lyra.entities.TrackEntity;

@Database(entities = {TrackEntity.class, ArtistEntity.class, AlbumEntity.class, PlaylistEntity.class, TrackArtistsReference.class, AlbumArtistsReference.class, PlaylistTracksReference.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract TrackDao trackDao();
    public abstract ArtistDao artistDao();
    public abstract AlbumDao albumDao();

    private static AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "song_cache").enableMultiInstanceInvalidation().build();
                }
            }
        }
        return INSTANCE;
    }
}

