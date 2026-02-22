package dev.geco.gmusic.model;

import java.util.Timer;

public class PlayState {

	private final Song song;
	private Timer timer;
	private long tickPosition;
	private boolean paused = false;

	public PlayState(Song song, Timer timer, long tickPosition) {
		this.song = song;
		this.timer = timer;
		this.tickPosition = tickPosition;
	}

	public Song getSong() { return song; }

	public Timer getTimer() { return timer; }

	public void setTimer(Timer timer) { this.timer = timer; }

	public long getTickPosition() { return tickPosition; }

	public void setTickPosition(long tickPosition) { this.tickPosition = tickPosition; }

	public boolean isPaused() { return paused; }

	public void setPaused(boolean paused) { this.paused = paused; }

}