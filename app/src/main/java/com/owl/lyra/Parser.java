package com.owl.lyra;

import com.owl.lyra.services.LoggingService;

import java.util.Arrays;

public class Parser {
    public static String[] parseSpotifyLink(String link) throws IllegalArgumentException {
        //Parsing input URL
        String requestType = null;
        String requestID = null;



        try {
            String[] splitResult = link.split("/");
            String mRequestType = splitResult[3];
            String mRequestID = splitResult[4].substring(0, 22);
            requestType = mRequestType;
            requestID = mRequestID;


            LoggingService.Logger.addRecordToLog("Request: ID:"+requestID + " Type: " + requestType);
            return new String[] {requestID, requestType};

        } catch (Exception e) {
            LoggingService.Logger.addRecordToLog("Invalid URL");
            throw new IllegalArgumentException("Invalid URL, please try again with a valid Spotify URL.");
        }
    }


    public static String parseYoutubeLink(String link) throws IllegalArgumentException {
        //Parsing input URL
        try {
            String[] splitResult = link.split("/");
            if (link.startsWith("https://youtu.be/") && link.length() == 28) {
                LoggingService.Logger.addRecordToLog(Arrays.toString(splitResult));
                String requestID = splitResult[3];
                return requestID;
            }
            else if(link.startsWith("https://www.youtube.com/watch?v=") && link.length() == 43) {
                String requestID = splitResult[3].substring(8, 19);
                return requestID;
            }
            else{
                throw new IllegalArgumentException("Invalid URL, please try again with a valid YouTube URL.");
            }

        } catch (Exception e) {
            LoggingService.Logger.addRecordToLog("Invalid URL");
            LoggingService.Logger.addRecordToLog(e.toString());
            throw new IllegalArgumentException("Invalid URL, please try again with a valid YouTube URL.");
        }
    }

}

