package com.owl.lyra.services;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;

import com.owl.lyra.ConcurrentTask;
import com.owl.lyra.Youtube;
import com.owl.lyra.objects.Track;
import com.owl.lyra.objects.TrackList;

import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

// Instance lives within the GUI thread and co-ordinates the youtube search and caching

public class SearchService {
    private final Youtube youtube = new Youtube();
//    private TrackList trackList;
    private DatabaseService database;

    private final ConcurrentTask concurrency;

    public SearchService(Context context) {
        this.database = DatabaseService.getInstance(context);
        this.concurrency = ConcurrentTask.getInstance();
    }

    class retrieveVideoURL implements Callable<Track> {
        private final Track track;

        // Constructor class defines the trackId as input.
        public retrieveVideoURL(Track track) {
            this.track = track;
        }

        @Override
        public Track call() {
            // cachedData will be a String[] in the form of [Cached URL, Cached Last Modified Date]
            String[] cachedData = database.getVideoURL(track.id);
            if (cachedData == null) {
                LoggingService.Logger.addRecordToLog("Cache search returned NULL for Track: " + track.name);
                // If not found in the database
                LoggingService.Logger.addRecordToLog("Initiating search for track: " + track.name);
                Entry<String, Integer> bestMatchSet = youtube.search(track.name, track.artists.get(0).name, track.duration_ms);
                // if bestMatchId is not null.
                if (bestMatchSet != null) {
                    database.addTrackToCacheSingle(track, "https://www.youtube.com/watch?v=" + bestMatchSet.getKey());
                    LoggingService.Logger.addRecordToLog("https://www.youtube.com/watch?v=" + bestMatchSet.getKey());

                    track.setVideoURL("https://www.youtube.com/watch?v=" + bestMatchSet.getKey());
                    track.setVariation(bestMatchSet.getValue());
                    track.setLastModified(TimeService.now());

                    return track;
                }
                // If search failed
                else {
                    database.addTrackToCacheSingle(track, "https://www.youtube.com/watch?v=null");
                    LoggingService.Logger.addRecordToLog("Database entry made for " + track.getName() + " excluding link.");

                    track.setVideoURL("https://www.youtube.com/watch?v=null");
                    track.setVariation(404);
                    track.setLastModified(TimeService.now());
                    return track;
                }
            }else {
                LoggingService.Logger.addRecordToLog("Cache search returned potential URL: " + cachedData[0]);
                track.setVideoURL(cachedData[0]);
                track.setVariation(-2);
                track.setLastModified(cachedData[1]);

                return track;
            }
        }
    }

    public MutableLiveData<TrackList> beginSearch(TrackList trackList) {
        MutableLiveData<TrackList> searchResults = new MutableLiveData<TrackList>();

        // Temp Variable 'internal results' defined because items cannot be directly added to mutable live data.
        // instead, the internal results variable is updated then its result directly set to equal the live data.
        TrackList internalResults = new TrackList();
        internalResults.items = new ArrayList<Track>();
        for (Track track : trackList.items) {
            concurrency.executeAsync(new retrieveVideoURL(track), result -> {
                internalResults.items.add(track);
                internalResults.total += 1;
                searchResults.setValue(internalResults);
            });
        }

        return searchResults;

    }

    class verifyVideoUrl implements Callable<Boolean> {
        private final String trackId;
        private final String videoId;

        // Constructor class defines the trackId as input.
        public verifyVideoUrl(String trackId, String videoId) {
            this.trackId = trackId;
            this.videoId = videoId;
        }

        @Override
        public Boolean call() {
            if (youtube.verifyUrlValidity(videoId)) {
                database.setVideoURL(trackId, videoId);
                return true;
            }
            else {
                return false;
            }
        }
    }


    public MutableLiveData<Track> updateVideoUrl(Track track, String videoId) {
        MutableLiveData<Track> liveTrack = new MutableLiveData<Track>();

        // Temp Variable 'internal results' defined because items cannot be directly added to mutable live data.
        // instead, the internal results variable is updated then its result directly set to equal the live data.
        Track internalResults = track;

            concurrency.executeAsync(new verifyVideoUrl(track.getId(), videoId), result -> {
                // If verification of the YouTube URL was successful,
                // Update MutableLiveData track to include URL so it can be passed into updateTrackList()
                if (result) {
                    internalResults.setVideoURL("https://www.youtube.com/watch?v=" + videoId);
                    liveTrack.setValue(internalResults);
                }
                else {
                    internalResults.setVideoURL("null");
                    liveTrack.setValue(internalResults);
                }
            });


        return liveTrack;

    }



}
