package com.owl.lyra;

import android.content.Context;
import android.util.SparseArray;

import androidx.lifecycle.MutableLiveData;

import java.lang.Math;

import com.owl.lyra.services.LoggingService;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import at.huber.youtubeExtractor.VideoMeta;
import at.huber.youtubeExtractor.YouTubeExtractor;
import at.huber.youtubeExtractor.YtFile;

public class Youtube {
    public final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    public final JsonFactory JSON_FACTORY = new JacksonFactory();
    private final String[] API_KEYS= System.getenv("GOOGLE_API_KEYS").split(",");

    private YouTube youtube;
    private Integer keyIndex = 0;


    public Youtube() {
        youtube = new YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY, new HttpRequestInitializer() {
            public void initialize(HttpRequest request) {
            }
        }).build();
    }

    public void advanceKeyIndex() {
        keyIndex += 1;
    }

    private String formatIdList(ArrayList<SearchResult> videos) {
        String idString = "";
        for (SearchResult result : videos) {
            idString += result.getId().getVideoId()+",";
        }

        return idString.substring(0, idString.length()-1);
    }


    class youtubeSearch implements Callable<String> {
        private final String qTerm;
        private final int songDuration;

        public youtubeSearch(String queryTerm, int songDuration) {
            this.qTerm = queryTerm;
            this.songDuration = songDuration;
        }


        @Override
        public String call() throws Exception {
//      ADD A TRY EXCEPT LOOP HERE TO ADVANCE KEY INDEX
            LoggingService.Logger.addRecordToLog("Processing search request for query term: " + qTerm + " | Executing on thread: " + Thread.currentThread().getName());
            // Getting a copy of the key index to do a bandaid fix on thread lock
            // Saving a copy of the key index used for this instance and setting the global key index to saved+1
            int instanceKeyIndex = 0;
            for (String key : API_KEYS) {
                if (!key.equals(API_KEYS[keyIndex])) {
                    instanceKeyIndex += 1;
                }
            }
            YouTube.Search.List search = youtube.search().list("snippet");
            search.setKey(API_KEYS[instanceKeyIndex]);
            search.setMaxResults(10L);
            search.setQ(qTerm);

            int minVariation = -1;
            String bestMatchId = null;
            ArrayList<SearchResult> filteredResults = new ArrayList<SearchResult>();
            
            try {
                SearchListResponse response = search.execute();
                
                String[] qTermComponents = qTerm.split(" ");

                for (SearchResult result : response.getItems()) {
                    LoggingService.Logger.addRecordToLog("===========================================================");
                    LoggingService.Logger.addRecordToLog("KIND: " + result.getId() + " " + result.getId().getKind());
                    // BEGIN FILTERING RESULTS
                    // Filter out some obvious cases where titles or content type (e.g. Playlist instead of video) do not match
                    // Checking if the result is actually a video
                    if (result.getId().getKind().equals("youtube#video")) {
                        // Checking if the very first word can be found within the target video title.
                        LoggingService.Logger.addRecordToLog("FIRST WORD: " + qTermComponents[0] + "TITLE: " + result.getSnippet().getTitle() + "OUTCOME: " + result.getSnippet().getTitle().toLowerCase().contains(qTermComponents[0].toLowerCase()));
                        LoggingService.Logger.addRecordToLog(result.getSnippet().getTitle().toLowerCase());
                        LoggingService.Logger.addRecordToLog(qTermComponents[0].toLowerCase());

                        if (result.getSnippet().getTitle().toLowerCase().contains(qTermComponents[0].toLowerCase())) {
                            filteredResults.add(result);
                        }
                    }
                }
                // If no valid searches
                if (filteredResults.size() == 0) {

                }

            }
            catch (Exception e){
                advanceKeyIndex();
            }
            LoggingService.Logger.addRecordToLog(filteredResults.toString());

            // Return the duration of the filtered results for comparison
            YouTube.Videos.List videos = youtube.videos().list("contentDetails");
            videos.setId(formatIdList(filteredResults)); // List of IDS that is to be queried
            videos.setKey(API_KEYS[keyIndex]);
            VideoListResponse videoResponse = videos.execute();

            for (Video video : videoResponse.getItems()) {
                java.time.Duration duration = java.time.Duration.parse(video.getContentDetails().getDuration());
                int videoVariation = Math.abs(this.songDuration- (int)duration.getSeconds()*1000 - minVariation);
                LoggingService.Logger.addRecordToLog("DURATION: " + duration.getSeconds() + "VARIATION: " + videoVariation);

                if ((videoVariation < minVariation || minVariation == -1) && videoVariation < 10000) { // Possible search threshold option implementation in the future with the 10000ms value?
                    minVariation = videoVariation;
                    bestMatchId = video.getId();
                }

            }

            return bestMatchId;
        }
    }

    public Entry<String, Integer> retrieveVideoId(String queryTerm, int songDuration) throws Exception {
//      ADD A TRY EXCEPT LOOP HERE TO ADVANCE KEY INDEX
        LoggingService.Logger.addRecordToLog("Processing search request for query term: " + queryTerm + " | Executing on thread: " + Thread.currentThread().getName());
        YouTube.Search.List search = youtube.search().list("snippet");
        search.setKey(API_KEYS[keyIndex]);
        search.setMaxResults(10L);
        search.setQ(queryTerm);
        SearchListResponse response = search.execute();

        ArrayList<SearchResult> filteredResults = new ArrayList<SearchResult>();
        String[] qTermComponents = queryTerm.split(" ");

        int minVariation = -1;
        String bestMatchId = null;

        for (SearchResult result : response.getItems()) {
            LoggingService.Logger.addRecordToLog("===========================================================");
            LoggingService.Logger.addRecordToLog("KIND: " + result.getId() + " " + result.getId().getKind());
            // BEGIN FILTERING RESULTS
            // Filter out some obvious cases where titles or content type (e.g. Playlist instead of video) do not match
            // Checking if the result is actually a video
            if (result.getId().getKind().equals("youtube#video")) {
                // Checking if the very first word can be found within the target video title.
                LoggingService.Logger.addRecordToLog("FIRST WORD: " + qTermComponents[0] + "TITLE: " + result.getSnippet().getTitle() + "OUTCOME: " + result.getSnippet().getTitle().toLowerCase().contains(qTermComponents[0].toLowerCase()));
                LoggingService.Logger.addRecordToLog(result.getSnippet().getTitle().toLowerCase());
                LoggingService.Logger.addRecordToLog(qTermComponents[0].toLowerCase());

                if (result.getSnippet().getTitle().toLowerCase().contains(qTermComponents[0].toLowerCase())) {
                    filteredResults.add(result);
                }
            }
        }

        // If no valid searches
        if (filteredResults.size() == 0) {
            LoggingService.Logger.addRecordToLog("Search attempt resulted no plausible videos.");
            return null;
        }

        else {
            LoggingService.Logger.addRecordToLog(filteredResults.toString());

            // Return the duration of the filtered results for comparison
            YouTube.Videos.List videos = youtube.videos().list("contentDetails");
            videos.setId(formatIdList(filteredResults)); // List of IDS that is to be queried
            videos.setKey(API_KEYS[keyIndex]);
            VideoListResponse videoResponse = videos.execute();

            for (Video video : videoResponse.getItems()) {
                java.time.Duration duration = java.time.Duration.parse(video.getContentDetails().getDuration());
                int videoVariation = Math.abs(songDuration- (int)duration.getSeconds()*1000 - minVariation);
                LoggingService.Logger.addRecordToLog("DURATION: " + duration.getSeconds() + "VARIATION: " + videoVariation);

                if ((videoVariation < minVariation || minVariation == -1) && videoVariation < 10000) { // Possible search threshold option implementation in the future with the 10000ms value?
                    minVariation = videoVariation;
                    bestMatchId = video.getId();
                }

            }

            return new AbstractMap.SimpleEntry<>(bestMatchId, minVariation);
        }
    }


    public Entry<String, Integer> search(String track_title, String primary_artist, int track_duration) {
        try {
            Entry<String, Integer> firstSearch = retrieveVideoId(track_title + " " + primary_artist + " " +  "audio", track_duration);
            if (firstSearch == null) {
                Entry<String, Integer> secondSearch = retrieveVideoId(track_title + " " +  primary_artist + " " +  "Topic", track_duration);
                return secondSearch;
            }
            else {
                return firstSearch;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Boolean verifyUrlValidity(String videoId) {
        try {
            YouTube.Videos.List request = youtube.videos().list("id");
            request.setId(videoId);
            request.setKey(API_KEYS[keyIndex]);
            VideoListResponse response = request.execute();

            return response.getItems().get(0).getId().equals(videoId);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }



    public static MutableLiveData<String> getYoutubeDownloadUrl(Context context, String youtubeLink) {
        final MutableLiveData<String>[] downloadUrl = new MutableLiveData[]{new MutableLiveData<String>()};
        new YouTubeExtractor(context) {
            @Override
            public void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta vMeta) {
                if (ytFiles != null) {
                    int itag = 171;
                    downloadUrl[0].setValue(ytFiles.get(itag).getUrl());
                }
            }
        }.extract(youtubeLink, true, true);

        return downloadUrl[0];
    }




}
