package com.owl.lyra;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.RequestQueue;
import com.owl.lyra.services.LoggingService;
import com.owl.lyra.services.TimeService;
import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
public class SplashActivity extends AppCompatActivity {
    private final String CLIENT_ID = "d3b8a53b62024cf4bd82b8858e401097";
    private final String REDIRECT_URI = "com.owl.lyra://callback";
    private static final int REQUEST_CODE = 1333;

    private SharedPreferences.Editor editor;
    private SharedPreferences mSharedPreferences;
    private RequestQueue queue;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LoggingService.Logger.addRecordToLog(TimeService.now() + " | " + "Application started.");
        setContentView(R.layout.activity_splash);

        authenticateSpotify();

        mSharedPreferences = this.getSharedPreferences("SPOTIFY", 0);

    }

    private void startMainActivity(boolean auth_success) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("spotify_auth_outcome", auth_success);
        startActivity(intent);
    }

    private void authenticateSpotify() {
        AuthorizationRequest.Builder builder = new AuthorizationRequest.Builder(CLIENT_ID, AuthorizationResponse.Type.TOKEN, REDIRECT_URI);
        builder.setScopes(new String[]{"playlist-read-private"});
        AuthorizationRequest request = builder.build();
        AuthorizationClient.openLoginActivity(this, REQUEST_CODE, request);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthorizationResponse response = AuthorizationClient.getResponse(resultCode, intent);

            switch (response.getType()) {
                // Response was successful and contains auth token
                case TOKEN:
                    editor = getSharedPreferences("SPOTIFY", 0).edit();
                    editor.putString("token", response.getAccessToken());
                    editor.commit();
                    startMainActivity(true);






                    break;

                // Auth flow returned an error
                case ERROR:
                    // Handle error response
                    startMainActivity(false);
                    break;

                // Most likely auth flow was cancelled
                default:
                    // Handle other cases
            }
        }
    }

}