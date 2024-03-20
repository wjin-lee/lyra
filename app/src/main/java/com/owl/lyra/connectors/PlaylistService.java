package com.owl.lyra.connectors;

import com.android.volley.Request;
import com.android.volley.toolbox.Volley;
import com.owl.lyra.objects.Artist;
import com.owl.lyra.objects.Playlist;

import android.content.Context;
import android.content.SharedPreferences;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.owl.lyra.objects.Track;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


public class PlaylistService {

    private static final String ENDPOINT = "https://api.spotify.com/v1/playlists/";
    private SharedPreferences mSharedPreferences;
    private RequestQueue mqueue;
    private Playlist playlist;

    public PlaylistService(Context context) {
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
    private Playlist cleanNames(Playlist targetPlaylist) {
        // Outer most loop goes through tracks & album names
        this.playlist.name = inputFilter(this.playlist.getName());
        for (Track currentTrack : targetPlaylist.tracks.items) {
            // INSERT CLEANING
            currentTrack.setName(inputFilter(currentTrack.getName()));
            currentTrack.album.setName(inputFilter(currentTrack.album.getName()));

            currentTrack.setParseSource("spotify");
            currentTrack.album.setParseSource("spotify");


            // 2nd Layer of cleaning - Artist Names
            for (Artist artist : currentTrack.artists) {
                // INSERT CLEANING
                artist.setName(inputFilter(artist.getName()));
                artist.setParseSource("spotify");

            }

            // 2nd Layer of cleaning - Album Artist Names
            for (Artist albumArtist : currentTrack.album.artists) {
                // INSERT CLEANING
                albumArtist.setName(inputFilter(albumArtist.getName()));
                albumArtist.setParseSource("spotify");

            }

        }
        return null;
    }

    public Playlist getPlaylist() {
        cleanNames(this.playlist);
        return this.playlist;
    }

    public void get(String playlistId, final VolleyCallBack callBack) {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET,
                ENDPOINT + playlistId + "?fields=id%2Cname%2Ctracks.offset%2Ctracks.total%2Ctracks.items(track.id%2Ctrack.name%2Ctrack.duration_ms%2Ctrack.album.id%2Ctrack.album.name%2Ctrack.album.images%2Ctrack.album.artists(id%2Cname)%2Ctrack.artists(id%2Cname))",
                null,
                response -> {
            Gson gson = new GsonBuilder()
                    .disableHtmlEscaping()
                    .create();
            // Removing unnecessary information and formatting to comply with our data structure.
                    JSONArray trackList = new JSONArray();
                    JSONObject modifiedResponse = null;
                    try {
                        JSONArray rawTrackList = response.getJSONObject("tracks").getJSONArray("items");
                        for (int i=0; i < rawTrackList.length(); i++) {
                            JSONObject track = rawTrackList.getJSONObject(i).getJSONObject("track");
                            trackList.put(track);
                        }
                        modifiedResponse = response;
                        modifiedResponse.getJSONObject("tracks").put("items", trackList);
               
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    
                    playlist = gson.fromJson(modifiedResponse.toString(), Playlist.class);

            callBack.onSuccess();
        }, error -> get(playlistId, () -> {

        })) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                String token = mSharedPreferences.getString("token", "");
                String auth = "Bearer " + token;
                headers.put("Authorization", auth);
                return headers;
            }

//            @Override
//            public Map<String, String> getParams() throws AuthFailureError {
//                Map<String, String> params = new HashMap<>();
//                String fields = "name,tracks.offset,tracks.total,tracks.items(track.id,track.name,track.duration_ms,track.album(id,name,images),track.artists(id,name))";
//                params.put("fields", fields);
//                return params;
//            }
        };
        mqueue.add(jsonObjectRequest);
    }

}
