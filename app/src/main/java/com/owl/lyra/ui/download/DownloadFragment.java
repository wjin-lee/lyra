package com.owl.lyra.ui.download;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.owl.lyra.DownloadSongCard;
import com.owl.lyra.MainActivity;
import com.owl.lyra.R;
import com.owl.lyra.RecyclerAdaptor;
import com.owl.lyra.Parser;
import com.owl.lyra.services.DownloadService;
import com.owl.lyra.services.LoggingService;
import com.owl.lyra.services.SearchService;
import com.owl.lyra.connectors.AlbumService;
import com.owl.lyra.connectors.PlaylistService;
import com.owl.lyra.connectors.TrackService;
import com.owl.lyra.objects.Album;
import com.owl.lyra.objects.Playlist;
import com.owl.lyra.objects.Track;
import com.owl.lyra.objects.TrackList;
import com.owl.lyra.services.TimeService;
import com.owl.lyra.ui.dialog.BooleanDialog;
import com.owl.lyra.ui.dialog.MessageDialog;
import com.owl.lyra.ui.dialog.TextInputDialog;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class DownloadFragment extends Fragment {

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private SearchService searchService;
    private DownloadService downloadService;

    private TextInputDialog linkPopup;
    private ArrayList<DownloadSongCard> trackList;

    private TrackList pendingList;
    private ArrayList<Track> removalList;
    private String requestTitle;

    private ArrayList<Track> downloadingTracks;

    final Observer<ArrayList<Track>> conversionObserver = new Observer<ArrayList<Track>>() {
        @Override
        public void onChanged(ArrayList<Track> trackArray) {
            Track updatedTrack = trackArray.get(trackArray.size()-1);
            updateTrackList(updatedTrack);
        }
    };

    private BroadcastReceiver onDownloadCompleteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Fetching the download id received with the broadcast
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            //Checking if the received broadcast is for our enqueued download by matching download id
            LoggingService.Logger.addRecordToLog("ACCESSING DOWNLOADING TRACKS");
            for (Track track : downloadingTracks) {
                if (track.getDownloadId() == id) {
                    track.setStatus("converting");
                    updateTrackList(track);
                    downloadService.convertToMp3(track, downloadService.getRequestDirectory().toString(), getContext(), (resultantStatus) -> {
                        track.setStatus(resultantStatus);
                        updateTrackList(track);
                        if (downloadingTracks.size() == trackList.size()) {
                            boolean noError = true;
                            for (Track downloadTrack : downloadingTracks) {
                                if (!downloadTrack.getStatus().equals("success")) {
                                    noError = false;
                                    break;
                                }
                            }
                            if (noError) {
                                LoggingService.Logger.addRecordToLog(TimeService.now() + " | " + "Download complete with 0 errors.");
                                MessageDialog successPopup = new MessageDialog("Success", "Download for all tracks have been completed without errors.");
                                successPopup.buildDialog(getContext());
                                successPopup.showDialog();
                            }
                            else{
                                LoggingService.Logger.addRecordToLog(TimeService.now() + " | " + "Download complete with errors.");
                            }
                        }
                    });
                }
            }
        }
    };


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().registerReceiver(onDownloadCompleteReceiver,new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(onDownloadCompleteReceiver);
    }

    private void appendNewTrackList(TrackList tracks) {
        trackList.clear();
        for (Track track : tracks.items) {
            trackList.add(new DownloadSongCard(
                    track.name,
                    track.getArtistString(),
                    track.album.name,
                    track.album.images.get(2).url, // Index two on the images list returns the smallest size (64x64) album art.
                    null,
                    -1,
                    "searching"
                    )
            );
        }
        mAdapter.notifyDataSetChanged();
        // Making the help text invisible
        TextView help_text = getActivity().findViewById(R.id.default_background_text);
        help_text.setAlpha(0);
    }

    private void appendNewTrackList(TrackList tracks, String albumName, String albumImage) {
        trackList.clear();
        for (Track track : tracks.items) {
            trackList.add(new DownloadSongCard(
                    track.name,
                    track.getArtistString(),
                    albumName,
                    albumImage,
                    null,
                    -1,
                    "searching"
                    )
            );
        }
        mAdapter.notifyDataSetChanged();
        // Making the help text invisible
        TextView help_text = getActivity().findViewById(R.id.default_background_text);
        help_text.setAlpha(0);
    }

    private void updateTrackList(Track updatedTrack) {
        //find the specific track within track list, update contents and notifydatasetchanged.
        int trackIndex = 0;
        for (DownloadSongCard songCard : trackList) {
            if (songCard.getTrackTitle().equals(updatedTrack.getName())) {
                break;
            }
            else {
                trackIndex ++;
            }
        }
        // If song card was not found, something went very wrong.
        if (trackIndex == trackList.size()-1 && !trackList.get(trackIndex).getTrackTitle().equals(updatedTrack.getName())) {
            // ADD CRITICAL LOG HERE
            LoggingService.Logger.addRecordToLog("-- CRITICAL -- The relevant track was not found inside the GUI Track List.");
        }

        if (updatedTrack.getStatus() == null) {
            if (updatedTrack.videoURL.equals("https://www.youtube.com/watch?v=null")) {
                // A url was not found for this. Add to removal list to queue it for removal.
                // If the user does not manually search and add this in by download request, it will be removed.
                trackList.get(trackIndex).setStatus("search_failed");
                removalList.add(updatedTrack);
            } else{
                //update the trackList element
                trackList.get(trackIndex).setVideoURL(updatedTrack.videoURL);
                trackList.get(trackIndex).setVariation(updatedTrack.variation);
                trackList.get(trackIndex).setStatus("awaiting_dl");
            }
        }
        else {
            // If it is in download stage.
            switch(updatedTrack.getStatus()) {
                case "download_queued":
                    LoggingService.Logger.addRecordToLog("Download for " + updatedTrack.getName() + " Queued");
                    trackList.get(trackIndex).setStatus("download_queued");
                    break;

                case "link_extraction_failed":
                    LoggingService.Logger.addRecordToLog("Link extraction for " + updatedTrack.getName() + " failed.");
                    trackList.get(trackIndex).setStatus("link_extraction_failed");
                    break;

                case "converting":
                    LoggingService.Logger.addRecordToLog("Converting cached track for " + updatedTrack.getName() + " to mp3");
                    trackList.get(trackIndex).setStatus("converting");
                    break;

                case "conversion_failed":
                    LoggingService.Logger.addRecordToLog("Ffmpeg format conversion failed for: " + updatedTrack.getName());
                    trackList.get(trackIndex).setStatus("conversion_failed");
                    break;

                case "metadata_assignment_failure":
                    LoggingService.Logger.addRecordToLog("Meta data assignment failed for: " + updatedTrack.getName());
                    trackList.get(trackIndex).setStatus("metadata_assignment_failure");
                    break;

                case "success":
                    LoggingService.Logger.addRecordToLog("Download completed for track: " + updatedTrack.getName());
                    trackList.get(trackIndex).setStatus("success");
                    break;
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_download, container, false);

        //Defining the song list that will hold the tracks
        trackList = new ArrayList<>();
        removalList = new ArrayList<>();

        LoggingService.Logger.addRecordToLog("SYSTEM PID:" + android.os.Process.myPid());
//        trackList.add(new DownloadSongCard("Test Title", "artist 12", "Album Name", "https://i.scdn.co/image/ab67616d00001e02d9ac4a9970da8f98ce2e2b35"));

        // Defining search service that will handle the searching of the parsed songs on YT
        MainActivity activity = (MainActivity) getActivity();
        assert activity != null;
        searchService = new SearchService(activity.getApplicationContext());
        downloadService = new DownloadService();

        // TESTING! ---------------------------------------------------------------------------
        final Observer<ArrayList<Track>> downloadObserver = new Observer<ArrayList<Track>>() {
            @Override
            public void onChanged(ArrayList<Track> trackArray) {
                downloadingTracks = trackArray;
                Track updatedTrack = trackArray.get(trackArray.size()-1);
                updateTrackList(updatedTrack);
            }
        };

        // Create the observer which updates the UI.
        final Observer<TrackList> urlObserver = new Observer<TrackList>() {
            @Override
            public void onChanged(@Nullable final TrackList newList) {
                // Update the UI, in this case, a TextView.

                Track updatedTrack = newList.items.get(newList.items.size()-1);
                // Setting the pending list to the most up-to-date list.
                // This list includes all songs including ones that do not have a link found.
                pendingList = newList;

                // If searching is done, show start btn
                if (newList.items.size() == trackList.size() && root.findViewById(R.id.dl_start).getAlpha() == 0) {
                    ExtendedFloatingActionButton startDownloadBtn = root.findViewById(R.id.dl_start);
                    startDownloadBtn.setOnClickListener(
                            view -> {
                                // Removal list check, prompt user if there are any songs missing links.
                                if (removalList.size() != 0) {
                                    BooleanDialog confirmationPopup = new BooleanDialog("Missing Tracks", "There are tracks that are missing a valid video link. These tracks will not be downloaded. Continue?", "YES", "NO");
                                    confirmationPopup.buildDialog(getContext(), () -> {
                                        // If user confirms continuation, remove tracks from removal list.
                                        for (Track track : removalList) {
                                            int cardIndex = 0;
                                            for (DownloadSongCard songCard : trackList) {
                                                if (songCard.getTrackTitle().equals(track.getName())) {
                                                    break;
                                                }
                                                else {
                                                    cardIndex ++;
                                                }
                                            }

                                            trackList.remove(cardIndex);
                                            pendingList.items.remove(track);
                                        }

                                        downloadService.beginDownload(getContext(), pendingList, requestTitle).observe(getViewLifecycleOwner(), downloadObserver);
                                    });
                                    confirmationPopup.showDialog();
                                }

                                else {
                                    LoggingService.Logger.addRecordToLog("Beginning download");
                                    downloadService.beginDownload(getContext(), pendingList, requestTitle).observe(getViewLifecycleOwner(), downloadObserver);
                                }
                            }
                    );
                    startDownloadBtn.hide();
                    startDownloadBtn.setAlpha(1);
                    startDownloadBtn.show();
                }

                updateTrackList(updatedTrack);
            }
        };



        // Initialising the popup
        linkPopup = new TextInputDialog("Spotify Link", "Please input the Spotify link you would like to download", "PARSE", "Cancel");
        linkPopup.buildDialog(getContext(), (link) -> {
                    // Lambda function defining behaviour after PARSE btn pressed.
                    LoggingService.Logger.addRecordToLog("PARSING --> " + link);
                    // parseLink method returns a String list in the format [REQUEST_ID, REQUEST TYPE]
                    // or null if it is an invalid request.
                    try {
                        String[] request = Parser.parseSpotifyLink(link);
                        String requestType = request[1];
                        String requestId = request[0];
                        switch(requestType) {
                            case "playlist":
                                pendingList = null;
                                removalList.clear();
                                PlaylistService playlistService = new PlaylistService(getActivity().getApplicationContext());
                                playlistService.get(request[0], () -> {
                                    Playlist playlist = playlistService.getPlaylist();
                                    // Printing
                                    playlist.toString();
                                    String jsonSender = new Gson().toJson(playlist, new TypeToken<Object>() {
                                    }.getType());
                                    LoggingService.Logger.addRecordToLog(jsonSender);

                                    // Updating GUI Track list
                                    appendNewTrackList(playlist.tracks);
                                    requestTitle = playlist.getName();
                                    searchService.beginSearch(playlist.tracks).observe(getViewLifecycleOwner(), urlObserver);
                                });
                                break;

                            case "album":
                                pendingList = null;
                                removalList.clear();
                                AlbumService albumService = new AlbumService(getActivity().getApplicationContext());
                                albumService.get(request[0], () -> {
                                    Album album = albumService.getAlbum();
                                    // Printing
                                    album.toString();
                                    String jsonSender = new Gson().toJson(album, new TypeToken<Object>() {
                                    }.getType());
                                    LoggingService.Logger.addRecordToLog(jsonSender);

                                    // Updating GUI Track list
                                    appendNewTrackList(album.tracks, album.name, album.images.get(2).url);
                                    requestTitle = album.getName();
                                    searchService.beginSearch(album.tracks);
                                });
                                break;

                            case "track":
                                pendingList = null;
                                removalList.clear();
                                TrackService trackService = new TrackService(getActivity().getApplicationContext());
                                trackService.get(request[0], () -> {
                                    TrackList track = trackService.getTrack();
                                    // Printing
                                    track.toString();
                                    String jsonSender = new Gson().toJson(track, new TypeToken<Object>() {
                                    }.getType());
                                    LoggingService.Logger.addRecordToLog(jsonSender);

                                    // Updating GUI Track list
                                    appendNewTrackList(track);
                                    requestTitle = track.items.get(0).getName();
                                    searchService.beginSearch(track).observe(getViewLifecycleOwner(), urlObserver);
                                });
                                break;
                            }


                    } catch (IllegalArgumentException e) {
                                        Snackbar.make(root, "Invalid URL, please try again with a valid Spotify URL.", 1500)
                                                .setAction("abcdef", null)
                                                .show();
                    }

                });

        //Initialising the Recyclerview
        mRecyclerView = root.findViewById(R.id.dl_song_container);

        //Setting the size of the recycler view itself to fixed to make it stay the same size
        //regardless of how many items are within the view. Also improves performance.
        mRecyclerView.setHasFixedSize(true);

        mLayoutManager = new LinearLayoutManager(getActivity());
        mAdapter = new RecyclerAdaptor(trackList);

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);


        //Creating Addition Button
        FloatingActionButton fab = root.findViewById(R.id.dl_start_new);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                linkPopup.showDialog();
            }
        });


        return root;
    }


    private void changeVideoURL(int trackIndex, String newVideoId) {

        LoggingService.Logger.addRecordToLog("CHANGE VIDEO URL CALLED, Track Index: " + trackIndex + " LINK: " + newVideoId);
        // Verify youtube link is valid,
        final Observer<Track> observer = new Observer<Track>() {
            @Override
            // Verification query request complete, update GUI accordingly.
            public void onChanged(Track track) {
                // If the link validity was false, notify user
                if (track.getVideoURL().equals("null")) {
                    MessageDialog errorPopup = new MessageDialog("Error", "URL validity could not be determined, please double check the URL or the internet connection.");
                    errorPopup.buildDialog(getContext());
                    errorPopup.showDialog();
                }
                else {
                    removalList.remove(pendingList.items.get(trackIndex));
                    updateTrackList(track);
                    MessageDialog successPopup = new MessageDialog("Success", "URL for " + track.getName() + " was successfully changed!");
                    successPopup.buildDialog(getContext());
                    successPopup.showDialog();
                }

            }
        };

        searchService.updateVideoUrl(pendingList.items.get(trackIndex), newVideoId).observe(getViewLifecycleOwner(), observer);

    }

    @Override
    public boolean onContextItemSelected(@NotNull MenuItem item) {
        int trackIndex = 0;
        for (Track track : pendingList.items) {
            if (track.getName().equals(trackList.get(item.getGroupId()).getTrackTitle())) {
                break;
            }
            else {
                trackIndex ++;
            }
        }
        // If track was not found, something went very wrong.
        if (trackIndex == pendingList.items.size()-1 && !pendingList.items.get(trackIndex).getName().equals(trackList.get(item.getGroupId()).getTrackTitle())) {
            LoggingService.Logger.addRecordToLog("-- CRITICAL -- The track could not be found within the pending list.");
        }


        //Prompt user for new URL
        TextInputDialog ytLinkPopup = new TextInputDialog("New URL", "Please input the new YouTube link.", "DONE", "Cancel");
        int finalTrackIndex = trackIndex;
        ytLinkPopup.buildDialog(getContext(), (link) -> {
            try {
                String videoId = Parser.parseYoutubeLink(link);
                changeVideoURL(finalTrackIndex, videoId);
            }
            catch (IllegalArgumentException e) {
                MessageDialog errorPopup = new MessageDialog("Error", "Invalid URL. please retry with a valid YouTube URL.");
                errorPopup.buildDialog(getContext());
                errorPopup.showDialog();
            }
        });

        ytLinkPopup.showDialog();
        return true;
    }

}