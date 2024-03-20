package com.owl.lyra.connectors;

import android.content.Context;
import android.content.SharedPreferences;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.owl.lyra.objects.Artist;
import com.owl.lyra.objects.Track;
import com.owl.lyra.objects.TrackList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
//import com.jlubecki.soundcloud.webapi.android.SoundCloudAPI;
//import com.jlubecki.soundcloud.webapi.android.SoundCloudService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
//
//import retrofit2.Call;
//import retrofit2.Callback;
//import retrofit2.Response;

public class TrackService {
    private static final String ENDPOINT = "https://api.spotify.com/v1/tracks/";
    private SharedPreferences mSharedPreferences;
    private RequestQueue mqueue;
    private TrackList track;

    public TrackService(Context context) {
        mqueue = Volley.newRequestQueue(context);
        mSharedPreferences = context.getSharedPreferences("SPOTIFY", 0);;
    }

    private String inputFilter(String targetString) {
        String[][] ILLEGAL_CHARS = {{"'", "'"}, {"\"", "'"}, {"/", ""}, {"\\", ""}, {"|", "-"}, {"?", ""}, {"*", ""}, {"<", ""},{">", ""},};
        for (String[] replacementCharPair:ILLEGAL_CHARS
        ) {
            targetString = targetString.replace(replacementCharPair[0], replacementCharPair[1]);
        }
        return targetString;
    }

    // The response obtained from Spotify may contain unescaped characters that must be converted into standard unicode.
    // Nested for loops crawl through the tree and correct for any characters that may cause issues.
    private TrackList cleanNames(TrackList trackList) {
        // Cleaning
        Track targetTrack = trackList.items.get(0);

        targetTrack.setName(inputFilter(targetTrack.getName()));
        targetTrack.setParseSource("spotify");

        targetTrack.album.setName(inputFilter(targetTrack.album.getName()));
        targetTrack.album.setParseSource("spotify");

        for (Artist artist : targetTrack.artists) {
            artist.setName(inputFilter(artist.getName()));
            artist.setParseSource("spotify");
        }

        for (Artist albumArtist:targetTrack.album.artists) {
            albumArtist.setName(inputFilter(albumArtist.getName()));
            albumArtist.setParseSource("spotify");

        }
        
        return null;
    }

    public TrackList getTrack() {
        cleanNames(this.track);
        return this.track;
    }

    public void get(String trackId, final VolleyCallBack callBack) {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET,
                ENDPOINT + trackId,
                null,
                response -> {
                    Gson gson = new GsonBuilder()
                            .disableHtmlEscaping()
                            .create();

                    JSONObject trackList = new JSONObject();
                    try {
                        JSONArray rawTrackList = new JSONArray();
                        rawTrackList.put(response);
                        trackList.put("total", 1);
                        trackList.put("offset", 0);
                        trackList.put("items", rawTrackList);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    track = gson.fromJson(trackList.toString(), TrackList.class);
                    callBack.onSuccess();
                }, error -> get(trackId, () -> {

        })) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                String token = mSharedPreferences.getString("token", "");
                String auth = "Bearer " + token;
                headers.put("Authorization", auth);
                return headers;
            }
        };
        mqueue.add(jsonObjectRequest);
    }
}
