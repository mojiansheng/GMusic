package dev.geco.gmusic.model;

public enum PlayListMode {

    DEFAULT(0),
    FAVORITES(1),
    RADIO(2);

    private final int id;

    PlayListMode(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static PlayListMode byId(int id) {
        for(PlayListMode playListMode : values()) if(playListMode.getId() == id) return playListMode;
        return null;
    }

}