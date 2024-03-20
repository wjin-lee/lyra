package com.owl.lyra.services;

import android.app.DownloadManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.owl.lyra.ConcurrentTask;
import com.owl.lyra.objects.Album;
import com.owl.lyra.objects.Artist;
import com.owl.lyra.objects.Track;
import com.owl.lyra.objects.TrackList;
import com.owl.lyra.ui.dialog.MessageDialog;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;
import com.owl.lyra.ui.dialog.OkTextCallbackInterface;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldDataInvalidException;
import org.jaudiotagger.tag.FieldKey;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import at.huber.youtubeExtractor.VideoMeta;
import at.huber.youtubeExtractor.YouTubeExtractor;
import at.huber.youtubeExtractor.YtFile;
//
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagOptionSingleton;
import org.jaudiotagger.tag.id3.ID3v23Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.ArtworkFactory;

import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;

public class DownloadService {

    private final ConcurrentTask concurrency;
    private File requestDirectory;
    private final ConversionQueueManager cqManager;

    public DownloadService() {
        this.concurrency = ConcurrentTask.getInstance();
        this.cqManager = new ConversionQueueManager();
    }

    class queueDownload implements Callable<Entry<Long, String>> {
        private final Context context;
        private final Track track;
        private final String requestPath;

        // Constructor class defines the trackId as input.
        public queueDownload(Context context, Track track, String requestPath) {
            this.context = context;
            this.track = track;
            this.requestPath = requestPath;
        }

        @Override
        public Entry<Long, String> call() {
            MutableLiveData<String[]> downloadUrlInfo = new MutableLiveData<String[]>(new String[]{"", ""});
            new downloadLinkExtractor(context, downloadUrlInfo).extract(track.getVideoURL(), true, true);

            // Absolutely horrible way to wait for AsyncTask to be finished.
            while (downloadUrlInfo.getValue() != null && downloadUrlInfo.getValue()[0].equals("")) {

            }
            LoggingService.Logger.addRecordToLog("Suitable dl link found for " + track.getName() + " with file ext " + downloadUrlInfo.getValue()[0]);

            if (downloadUrlInfo.getValue() == null) {
                return null;
            }
            else {
                long downloadId = downloadFromUrl(context, downloadUrlInfo.getValue()[0], "[CACHE]"+track.getName()+"."+downloadUrlInfo.getValue()[1], false);
                return new AbstractMap.SimpleEntry<>(downloadId, downloadUrlInfo.getValue()[1]);
            }
        }
    }

    private static class downloadLinkExtractor extends YouTubeExtractor {

        private final MutableLiveData<String[]> outputString;

        public downloadLinkExtractor(@NonNull Context con, MutableLiveData<String[]> outputString) {
            super(con);
            this.outputString = outputString;
        }

        @Override
        protected void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta videoMeta) {
            if (ytFiles != null) {
                //---------------
                //171 webm 128k
                //251 webm 160k
                //250 webm 70k
                //140 m4a 128k
                //141 m4a 256k
                //---------------
//                List<Integer> iTagPreferenceList = Arrays.asList(140, 141);
                // iTag preference list only including webm formats.
//                List<Integer> iTagPreferenceList = Arrays.asList(171, 251, 250);
                // iTag preference list including m4a formats
                List<Integer> iTagPreferenceList = Arrays.asList(171, 251, 250, 140, 141);
                // -----------------------DEBUG-------------------------
                LoggingService.Logger.addRecordToLog("AVAILABLE iTAGS");
                for (int i = 0; i < ytFiles.size(); i++) {
                    YtFile file = ytFiles.get(ytFiles.keyAt(i));
                    LoggingService.Logger.addRecordToLog(file.getFormat().toString());
                }
                //------------------------------------------------------


                boolean iTagFound = false;
                for (int itag : iTagPreferenceList) {
                    if (ytFiles.get(itag) != null) {
                        // If iTag is valid, use it.
                        LoggingService.Logger.addRecordToLog("VALID iTAG FOUND: " + itag);
                        String fileExt;
                        if (itag == 140 || itag == 141) {
                            fileExt = "m4a";
                        }
                        else{
                            fileExt = "webm";
                        }
                        outputString.postValue(new String[]{ytFiles.get(itag).getUrl(), fileExt});
                        iTagFound = true;
                        break;
                    }
                }
                if (!iTagFound) {
                    LoggingService.Logger.addRecordToLog("--CRITICAL-- Suitable iTag Not found.");
                    outputString.postValue(null);
                }
            }
        }
    }

    private static File getSDCardMusicDirectory(Context context) {
        // Returns all external file directories that this app owns.
        // E.g./storage/emulated/0/Android/data/com.owl.lyra/files, /storage/458D-65FD/Android/data/com.owl.lyra/files
        File[] dirList = context.getExternalFilesDirs(null);
        // Takes the first root directory that is not 'emulated'.
        // In my case there are only two possible directories, emulated and SD card but this method breaks if there is anything less/more than 2.
        for (File dir : dirList) {
            String[] directoryComponents = dir.toString().split("/");
            if (!directoryComponents[2].equals("emulated")) {
                File possibleDirectory = new File("/storage/"+directoryComponents[2]+"/Music");
                if (possibleDirectory.exists() && possibleDirectory.isDirectory()) {
                    return possibleDirectory;
                }
            }
        }
        // It should never get here
        LoggingService.Logger.addRecordToLog("--CRITICAL-- SD card location was not found. defaulting to emulated.");
        return new File("/storage/emulated");
    }


    public MutableLiveData<ArrayList<Track>> beginDownload(Context context, TrackList trackList, String requestTitle) {
        MutableLiveData<ArrayList<Track>> liveTrackList = new MutableLiveData<ArrayList<Track>>();
        ArrayList<Track> internalResults = new ArrayList<Track>();

        File musicDirectory = getSDCardMusicDirectory(context);

        requestDirectory = new File(musicDirectory, requestTitle);

        // Ensuring the target download folder with the correct name exists,
        if (requestDirectory.exists()) {
            if (!requestDirectory.isDirectory()) {
                // If there is a file named something we need the directory to be named, inform user.
                MessageDialog errorPopup = new MessageDialog("Error", "Target directory could not be created due to name collision. Please check the file location.");
                errorPopup.buildDialog(context);
                errorPopup.showDialog();
            }
            // Directory was already created before
            else{
                // If the cache file was not deleted during a previous run for whatever reason, delete it now.
                for (File file : Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).listFiles()) {
                    if (file.getName().startsWith("[CACHE]")) {
                        if(!file.delete()) {
                            MessageDialog errorPopup = new MessageDialog("Warning", "Cached files could not be deleted. Please verify permissions.");
                            errorPopup.buildDialog(context);
                            errorPopup.showDialog();
                        }
                    }
                }
            }
        }
        else{
            // Create directory
            LoggingService.Logger.addRecordToLog("TARGET DIRECTORY: " + requestDirectory.toString());
            if(!requestDirectory.mkdir()) {
                // Directory creation failed, inform user.
                MessageDialog errorPopup = new MessageDialog("Error", "Target directory creation failed. Please check the file location.");
                errorPopup.buildDialog(context);
                errorPopup.showDialog();
            }
        }


        for (Track track : trackList.items) {
            File targetFile = new File(requestDirectory, track.getName() + ".mp3");
            if (targetFile.exists()) {
                if (!targetFile.isFile()) {
                    // This shouldn't happen.
                    MessageDialog errorPopup = new MessageDialog("Error", "A directory by the same name as the song exists.");
                    errorPopup.buildDialog(context);
                    errorPopup.showDialog();
                }
                else {
                    // Test if the file was edited after its download date
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.ENGLISH);
                    Date dlLastModDate = new Date(targetFile.lastModified());
                    Date cachedLastModDate = new Date(0);
                    try {
                        cachedLastModDate = formatter.parse(track.getLastModified());
                    }
                    catch (java.text.ParseException e) {
                        LoggingService.Logger.addRecordToLog("Date Parse Exception, targetFile: " + targetFile.lastModified() + " cached: " + track.getLastModified());
                    }
                    LoggingService.Logger.addRecordToLog(TimeService.now() + " | " + "Comparing cached and dl dates. " + track.getLastModified() + " " + dlLastModDate.toString());
                    if (dlLastModDate.before(cachedLastModDate)) {
                        LoggingService.Logger.addRecordToLog(TimeService.now() + " | " + "Cached data for track was updated since last download, updating ");
                        concurrency.executeAsync(new queueDownload(context, track, requestDirectory.toString()), result -> {
                            if (result != null) {
                                track.setStatus("download_queued");
                                track.setCacheFileExt(result.getValue());
                                track.setDownloadId(result.getKey());

                            } else {
                                track.setStatus("link_extraction_failed");
                            }

                            internalResults.add(track);
                            LoggingService.Logger.addRecordToLog(TimeService.now() + " | " + "POSTING DOWNLOADING LIST");
                            liveTrackList.postValue(internalResults);
                        });
                    }
                    else {
                        LoggingService.Logger.addRecordToLog(TimeService.now() + " | " + track.getName() + " was already found in the download location. Skipping download." + dlLastModDate.toString());
                        track.setStatus("success");
                        track.setDownloadId(-100);
                        internalResults.add(track);
                        LoggingService.Logger.addRecordToLog(TimeService.now() + " | " + "POSTING DOWNLOADING LIST");
                        liveTrackList.postValue(internalResults);
                    }
                }
            }
            else {
                concurrency.executeAsync(new queueDownload(context, track, requestDirectory.toString()), result -> {
                    if (result != null) {
                        track.setStatus("download_queued");
                        track.setCacheFileExt(result.getValue());
                        track.setDownloadId(result.getKey());

                    } else {
                        track.setStatus("link_extraction_failed");
                    }

                    internalResults.add(track);
                    LoggingService.Logger.addRecordToLog(TimeService.now() + " | " + "POSTING DOWNLOADING LIST");
                    liveTrackList.postValue(internalResults);
                });
            }
        }

        return liveTrackList;
    }

    //make a beginDownload() function that controls the download process.
    // File management,
    // Checking if the file already exists,
    // Queueing the downloads thyrough downloadFromUrl function.

    private long downloadFromUrl(Context context, String youtubeDlUrl, String downloadTitle, boolean hide) {
        Uri uri = Uri.parse(youtubeDlUrl);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setTitle(downloadTitle);
        if (hide) {
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
            request.setVisibleInDownloadsUi(false);
        } else
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

//        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, downloadDirectory + "/" + downloadTitle);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, downloadTitle);


        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        return manager.enqueue(request);
    }


    class convert implements Callable<String> {
        private final Track track;
        private final String filePath;
        private Context context;

        // Constructor class defines the trackId as input.
        public convert(Track track, String filePath, Context context) {
            this.track = track;
            this.filePath = filePath;
            this.context = context;
        }

        @Override
        public String call() {
            String outputPath = filePath + "/" + track.getName() + ".mp3";
            String command = "-i \"" + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/[CACHE]" + track.getName() + "." + track.getCacheFileExt() + "\" " + "-y \"" + outputPath + "\"";
            LoggingService.Logger.addRecordToLog("COMMAND" + command);
            int rc = FFmpeg.execute(command);

            if (rc == RETURN_CODE_SUCCESS) {
                Log.i(Config.TAG, "Command execution completed successfully.");
            } else if (rc == RETURN_CODE_CANCEL) {
                Log.i(Config.TAG, "Command execution cancelled by user.");
                return "conversion_failed";
            } else {
                Log.i(Config.TAG, String.format("Command execution failed with rc=%d and the output below.", rc));
                Config.printLastCommandOutput(Log.INFO);
                return "conversion_failed";
            }

            File cacheFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/[CACHE]" + track.getName() + "." + track.getCacheFileExt());
            if (!cacheFile.delete()) {
                LoggingService.Logger.addRecordToLog("Failed to delete cached file.");
            }


            if(setId3Tags(outputPath, track)) {
                return "success";
            }
            else{
                return "metadata_assignment_failure";
            }
        }
    }

    public static Bitmap getBitmapFromURL(String src) {
        try {
            java.net.URL url = new java.net.URL(src);
            HttpURLConnection connection = (HttpURLConnection) url
                    .openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    private boolean setId3Tags(String filePath, Track track) {
        File targetFile = new File(filePath);
        if (targetFile.exists() && targetFile.isFile()) {
            AudioFile file;
            TagOptionSingleton.getInstance().setAndroid(true);
            try {
                file = AudioFileIO.read(targetFile);
                Album album = track.getAlbum();
                file.setTag(new ID3v23Tag());
                Tag tag = file.getTag();

                // Setting Artists
                StringBuilder artistString = new StringBuilder();
                for (Artist artist : track.artists) {
                    artistString.append(artist.name).append("/");
                }
                tag.addField(FieldKey.ARTIST, artistString.toString().substring(0, artistString.length()-1));

                // Setting Album Name
                tag.addField(FieldKey.ALBUM, album.getName());

                // Setting Album Artists
                artistString = new StringBuilder();
                for (Artist artist : album.artists) {
                    artistString.append(artist.name).append("/");
                }
                tag.addField(FieldKey.ALBUM_ARTIST, artistString.toString().substring(0, artistString.length()-1));

                // Setting Album Art

                Bitmap bmp = getBitmapFromURL(album.getAlbumArtURL());

                //Getting save path and save name
                String[] pathComponents = filePath.split("/");
                StringBuilder artCacheFileName = new StringBuilder();
                for (String component : pathComponents) {
                    // If File name, change it
                    if (component.equals(pathComponents[pathComponents.length - 1])) {
                        artCacheFileName.append("/[CACHE]").append(component.substring(0, component.length() - 4)).append(".jpg");
                    }
                    else{
                        artCacheFileName.append("/").append(component);
                    }
                }



                try (FileOutputStream out = new FileOutputStream(artCacheFileName.toString())) {
                    bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    File artworkFile = new File(artCacheFileName.toString());
                    Artwork artwork = ArtworkFactory.createArtworkFromFile(artworkFile);

                    tag.addField(artwork);
                    tag.setField(artwork);

                    if (!artworkFile.delete()) {
                        LoggingService.Logger.addRecordToLog("Failed to delete cached file.");
                    }

                } catch (IOException e) {
                    e.printStackTrace();

                }

                file.commit();
                return true;
            }
            catch (FieldDataInvalidException e) {
                LoggingService.Logger.addRecordToLog("Field data invalid.");
                e.printStackTrace();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        else{
            LoggingService.Logger.addRecordToLog("-- Critical -- Resultant mp3 file not found.");
        }
        return false;
    }

    private class ConversionQueueManager {
        public boolean converting;
        public ArrayList<Entry<convert, OkTextCallbackInterface>> queue;

        public ConversionQueueManager() {
            this.queue = new ArrayList<>();
        }

        private void convertNext() {
            Entry<convert, OkTextCallbackInterface> request = queue.get(0);
            queue.remove(request);

            concurrency.executeAsync(request.getKey(), (result) -> {
                request.getValue().call(result);

                if (queue.size() != 0) {
                    convertNext();
                }
                else{
                    converting = false;
                }
            });

        }

        public void queue(Entry<convert, OkTextCallbackInterface> request) {
            queue.add(request);
            if (!converting && queue.size() != 0) {
                converting = true;
                convertNext();
            }
        }

    }

    public void convertToMp3(Track track, String dstFilePath, Context context, OkTextCallbackInterface callback) {
        cqManager.queue(new AbstractMap.SimpleEntry<>(new convert(track, dstFilePath, context), callback));
    }


    public File getRequestDirectory() {
        return requestDirectory;
    }
}
