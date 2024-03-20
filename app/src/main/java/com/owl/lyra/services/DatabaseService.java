package com.owl.lyra.services;

import android.content.Context;

import com.owl.lyra.AppDatabase;
import com.owl.lyra.ConcurrentTask;
import com.owl.lyra.dao.AlbumDao;
import com.owl.lyra.dao.ArtistDao;
import com.owl.lyra.dao.TrackDao;
import com.owl.lyra.entities.AlbumEntity;
import com.owl.lyra.entities.ArtistEntity;
import com.owl.lyra.entities.TrackEntity;
import com.owl.lyra.objects.Album;
import com.owl.lyra.objects.AlbumArt;
import com.owl.lyra.objects.Artist;
import com.owl.lyra.objects.Track;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

public class DatabaseService {
    private AppDatabase database;
    public TrackDao trackDao;
    public ArtistDao artistDao;
    public AlbumDao albumDao;
    private final ConcurrentTask concurrency;

    private static DatabaseService instance = null;

    // TESTING VARIABLE - REMOVE LATER!!!!
    public static int counter;

    private DatabaseService(Context context) {
        this.database = AppDatabase.getDatabase(context);
        this.trackDao = database.trackDao();
        this.artistDao = database.artistDao();
        this.albumDao = database.albumDao();

        this.concurrency = ConcurrentTask.getInstance();

        counter++;
        LoggingService.Logger.addRecordToLog("INSTANCE OF DATABASE SERVICE CREATED, COUNT: " + counter);
    }

    public static DatabaseService getInstance(Context context) {
        if(instance == null) {
            instance = new DatabaseService(context);
        }

        return instance;
    }


    class cacheTrack implements Callable<Boolean> {
        private Track track;
        private String videoUrl;


        public cacheTrack(Track track, String videoUrl) {
            this.track = track;
            this.videoUrl = videoUrl;
        }


        @Override
        public Boolean call() {
            // Checking if the track is already cached,
            if (trackDao.getTrack(track.id) == null) {

                // Combining track and album artists together without duplicates
                Set<Artist> set = new LinkedHashSet<>(track.artists);
                set.addAll(track.album.artists);
                List<Artist> allArtists = new ArrayList<>(set);

                for (Artist trackArtist : allArtists) {
                    if (artistDao.getArtist(trackArtist.id) == null) {
                        // Not found in db, add to cache
                        artistDao.addArtist(new ArtistEntity(trackArtist.id, trackArtist.parseSource, trackArtist.name));
                    }
                }

                // Adding album to db if not found
                if (albumDao.getAlbum(track.album.id) == null) {
                    // Not found in db, add to cache
                    albumDao.addAlbum(new AlbumEntity(track.album.id, track.album.parseSource, track.album.name, track.album.images.get(1).url));
                }

                trackDao.addTrack(new TrackEntity(track.id, track.parseSource, track.album.id, track.name, videoUrl, TimeService.now()));
                LoggingService.Logger.addRecordToLog("Database entry made for track: " + track.getName());
                return true;

            }else {
                return false;
            }

        }
    }

    // DEBUG - PRINTING ALL DATA TABLES
    class printAll implements Callable<Boolean> {

        @Override
        public Boolean call() {
            for (TrackEntity track:trackDao.getAllTracks()
                 ) {
                System.out.println(track.toString());
            }

            for (ArtistEntity artist:artistDao.getAllArtists()
            ) {
                System.out.println(artist.toString());
            }

            for (AlbumEntity album:albumDao.getAllAlbums()
            ) {
                System.out.println(album.toString());
            }

            return true;
        }
    }

    class retrieveTrack implements Callable<Track> {
        private String trackId;

        // Constructor class defines the trackId as input.
        public retrieveTrack(String trackId) {
            this.trackId = trackId;
        }

        @Override
        public Track call() {

            // Do all the searches necessary to construct a Track instance.

            // Checking if the track exists.
            TrackEntity trackEntity = trackDao.getTrack(trackId);
            if (trackEntity == null) {
                return null;
            } else {
                // Preparing Artist List
                ArrayList<Artist> artists = new ArrayList<>();
                // Query artist entities related to the trackId
                List<ArtistEntity> artistEntities = trackDao.getTrackArtists(trackId).artists;
                for (ArtistEntity artistEntity : artistEntities) {
                    artists.add(artistEntity.toArtist());
                }

                // Construct Album Object

                AlbumEntity albumEntity = albumDao.getAlbum(trackEntity.AlbumID);
                // Get album artists
                ArrayList<Artist> albumArtists = new ArrayList<>();
                // Query album artists
                List<ArtistEntity> albumArtistEntities = albumDao.getAlbumArtists(albumEntity.AlbumID).artists;
                for (ArtistEntity artistEntity : albumArtistEntities) {
                    albumArtists.add(artistEntity.toArtist());
                }

                // Constructing alum art array.
                // Contains only one element.
                ArrayList<AlbumArt> albumArt = new ArrayList<>();
                albumArt.add(new AlbumArt(albumEntity.AlbumArtURL));

                Album album = new Album(albumEntity.AlbumID, albumEntity.Title, albumEntity.ParseSource, albumArt, albumArtists);

                Track track = new Track(trackEntity.TrackID, trackEntity.Title, album, artists, trackEntity.VideoURL, trackEntity.LastModified);

                System.out.println(track.toString());
                return track;
            }
        }

    }

    public void addTrackToCacheSingle(Track track, String videoUrl) {
        // Checking if the track is already cached,
        LoggingService.Logger.addRecordToLog(TimeService.now() + " | " + "Checking track via getTrack");
        if (trackDao.getTrack(track.id) == null) {

            // Combining track and album artists together without duplicates
            Set<Artist> set = new LinkedHashSet<>(track.artists);
            set.addAll(track.album.artists);
            List<Artist> allArtists = new ArrayList<>(set);

            for (Artist trackArtist : allArtists) {
                if (artistDao.getArtist(trackArtist.id) == null) {
                    // Not found in db, add to cache
                    artistDao.addArtist(new ArtistEntity(trackArtist.id, trackArtist.parseSource, trackArtist.name));
                }
            }

            // Adding album to db if not found
            if (albumDao.getAlbum(track.album.id) == null) {
                // Not found in db, add to cache
                albumDao.addAlbum(new AlbumEntity(track.album.id, track.album.parseSource, track.album.name, track.album.images.get(1).url));
            }

            trackDao.addTrack(new TrackEntity(track.id, track.parseSource, track.album.id, track.name, videoUrl, TimeService.now()));
            LoggingService.Logger.addRecordToLog(TimeService.now() + " | " + "Database entry made for track: " + track.getName());

        }
    }

    public void printAllData() {
        concurrency.executeAsync(new printAll(), (System.out::println));
    }

    public String[] getVideoURL(String trackId) {
        LoggingService.Logger.addRecordToLog(TimeService.now() + " | " + "Checking track via getTrack");
        TrackEntity trackEntity = trackDao.getTrack(trackId);
        if (trackEntity == null) {
            return null;
        } else {
            return new String[]{trackEntity.VideoURL, trackEntity.LastModified};
            }
    }

    public void setVideoURL(String trackId, String newUrl) {
        trackDao.updateVideoURL(trackId, newUrl);
    }


    // Returns a track element based on the track ID provided.
    // null if none found.
    public Track getTrack(String trackId) {
//        final Track[] track = new Track[1];
//        concurrency.executeAsync(new retrieveTrack(trackId), (result -> {
//            track[0] = result;
//        }));
//
//        System.out.println("RETURNING CACHED TRACK" + track[0]);
//        return track[0];
        // Do all the searches necessary to construct a Track instance.

        // Checking if the track exists.
        TrackEntity trackEntity = trackDao.getTrack(trackId);
        if (trackEntity == null) {
            return null;
        } else {
            // Preparing Artist List
            ArrayList<Artist> artists = new ArrayList<>();
            // Query artist entities related to the trackId
            List<ArtistEntity> artistEntities = trackDao.getTrackArtists(trackId).artists;
            for (ArtistEntity artistEntity : artistEntities) {
                artists.add(artistEntity.toArtist());
            }

            // Construct Album Object

            AlbumEntity albumEntity = albumDao.getAlbum(trackEntity.AlbumID);
            // Get album artists
            ArrayList<Artist> albumArtists = new ArrayList<>();
            // Query album artists
            List<ArtistEntity> albumArtistEntities = albumDao.getAlbumArtists(albumEntity.AlbumID).artists;
            for (ArtistEntity artistEntity : albumArtistEntities) {
                albumArtists.add(artistEntity.toArtist());
            }

            // Constructing alum art array.
            // Contains only one element.
            ArrayList<AlbumArt> albumArt = new ArrayList<>();
            albumArt.add(new AlbumArt(albumEntity.AlbumArtURL));

            Album album = new Album(albumEntity.AlbumID, albumEntity.Title, albumEntity.ParseSource, albumArt, albumArtists);

            Track track = new Track(trackEntity.TrackID, trackEntity.Title, album, artists, trackEntity.VideoURL, trackEntity.LastModified);

            System.out.println(track.toString());
            return track;
        }
    }



    }
