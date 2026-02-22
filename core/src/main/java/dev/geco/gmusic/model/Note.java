package dev.geco.gmusic.model;

import java.util.ArrayList;
import java.util.List;

public class Note {

	/*
	 tickdelay!
	 ;amount
	 ?ref
	 -stop
	 _parts
	 (instrument : volume : pitch (#X) : ?distance) _ (instrument : volume : pitch (#X) : ?distance)
	 */

	private static final String DELAY = "!";
	private static final String TICKDELAY = "t";
	private static final String AMOUNT = ";";
	private static final String REF = "?";
	private static final String PARTS = "_";
	private final Song song;
	private long delay = 0;
	private long amount = 1;
	private final List<NotePart> parts = new ArrayList<>();
	private List<Note> references = new ArrayList<>();

	public Note(Song song, String noteString) {
		this.song = song;

		if(noteString.contains(DELAY)) {
			try {
				delay = (noteString.contains(TICKDELAY) ? 50 : 1) * Long.parseLong(noteString.split(DELAY)[0].replace(TICKDELAY, ""));
				if(delay < 0) delay = 0;
			} catch(NumberFormatException ignored) { }
			noteString = noteString.split(DELAY)[1];
		}

		if(noteString.contains(AMOUNT)) {
			try {
				long nodeAmount = Long.parseLong(noteString.split(AMOUNT)[1]);
				if(nodeAmount > 0) amount += nodeAmount;
			} catch(NumberFormatException ignored) { }
			noteString = noteString.split(AMOUNT)[0];
		}

		if(noteString.startsWith(REF)) {
			List<Note> noteReferences = this.song.getParts().get(noteString.replace(REF, ""));
			if(noteReferences != null) references = noteReferences;
		} else for(String i : noteString.split(PARTS)) parts.add(new NotePart(this, i));
	}

	public Song getSong() { return song; }

	public long getDelay() { return delay; }

	public long getAmount() { return amount; }

	public List<NotePart> getNoteParts() { return parts; }

	public List<Note> getReferences() { return references; }

	public boolean isReference() { return !references.isEmpty(); }

}