package dev.geco.gmusic.model;

public enum PlayMode {

    DEFAULT(0),
    SHUFFLE(1),
    LOOP(2);

    private final int id;

    PlayMode(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static PlayMode byId(int id) {
        for(PlayMode playMode : values()) if(playMode.getId() == id) return playMode;
        return null;
    }

}