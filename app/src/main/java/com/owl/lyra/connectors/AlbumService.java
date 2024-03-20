package com.owl.lyra.connectors;

import android.content.Context;
import android.content.SharedPreferences;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.owl.lyra.objects.Album;
import com.owl.lyra.objects.Artist;
import com.owl.lyra.objects.Track;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashMap;
import java.util.Map;

public class AlbumService {
    private static final String ENDPOINT = "https://api.spotify.com/v1/albums/";
    private SharedPreferences mSharedPreferences;
    private RequestQueue mqueue;
    private Album album;

    public AlbumService(Context context) {
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
    private Album cleanNames(Album targetAlbum) {
        this.album.name = inputFilter(this.album.getName());
        // Outer most loop goes through tracks & album names
        targetAlbum.setName(inputFilter(targetAlbum.getName()));
        targetAlbum.setParseSource("spotify");

        for (Artist albumArtist : targetAlbum.artists) {
            albumArtist.setName(inputFilter(albumArtist.getName()));
            albumArtist.setParseSource("spotify");
        }

        for (Track currentTrack : targetAlbum.tracks.items) {
            currentTrack.setName(inputFilter(currentTrack.getName()));
            currentTrack.setParseSource("spotify");

            // 2nd Layer of cleaning - Artist Names
            for (Artist artist : currentTrack.artists) {
                artist.setName(inputFilter(artist.getName()));
                artist.setParseSource("spotify");

            }
        }
        return null;
    }

    public Album getAlbum() {
        cleanNames(this.album);
        return this.album;
    }

    public void get(String albumId, final VolleyCallBack callBack) {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET,
                ENDPOINT + albumId,
                null,
                response -> {
                    Gson gson = new GsonBuilder()
                            .disableHtmlEscaping()
                            .create();
                    album = gson.fromJson(response.toString(), Album.class);
                    callBack.onSuccess();
                }, error -> get(albumId, () -> {

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
