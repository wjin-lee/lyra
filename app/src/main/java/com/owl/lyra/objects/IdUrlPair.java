package com.owl.lyra.objects;

import org.jetbrains.annotations.NotNull;

public class IdUrlPair {
    @NotNull
    public String trackId;
    public String videoUrl;

    public IdUrlPair(String trackId, String videoUrl) {
        this.trackId = trackId;
        this.videoUrl = videoUrl;
    }
}
