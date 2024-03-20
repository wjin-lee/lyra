package com.owl.lyra.objects;

public class Artist {
    public String id;
    public String name;
    public String parseSource;

    public Artist(String id, String name, String parseSource) {
        this.id = id;
        this.name = name;
        this.parseSource = parseSource;
    }

    // Getters
    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    // Setters
    public void setName(String newName) {
        this.name = newName;
    }

    public void setParseSource(String parseSource) {
        this.parseSource = parseSource;
    }
}
