package org.red5.server.net.rtmp.message;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents all the actions which may be permitted on a stream.
 * 
 * @author Paul Gregoire
 */
public enum StreamAction {
	CONNECT("connect"), DISCONNECT("disconnect"), CREATE_STREAM("createStream"), DELETE_STREAM("deleteStream"), CLOSE_STREAM(
			"closeStream"), INIT_STREAM("initStream"), RELEASE_STREAM("releaseStream"), PUBLISH("publish"), PAUSE(
			"pause"), PAUSE_RAW("pauseRaw"), SEEK("seek"), PLAY("play"), PLAY2("play2"), STOP("disconnect"), RECEIVE_VIDEO(
			"receiveVideo"), RECEIVE_AUDIO("receiveAudio"), CUSTOM("");

	//presize to fit all enums in
	private final static Map<String, StreamAction> map = new HashMap<String, StreamAction>(StreamAction.values().length);

	//the stream action this enum is for
	private final String actionString;

	StreamAction(String actionString) {
		this.actionString = actionString;
	}

	public String getActionString() {
		return actionString;
	}

	public static StreamAction getEnum(String actionString) {
		//fill the map if its empty
		if (map.isEmpty()) {
			//do this only once
			for (StreamAction action : values()) {
				map.put(action.getActionString(), action);
			}
		}
		//look up the action from the predefined set
		StreamAction match = map.get(actionString);
		if (match != null) {
			return match;
		}
		//return an action representing a custom type
		return CUSTOM;
	}

	public boolean equals(StreamAction action) {
		return action.getActionString().equals(actionString);
	}

	public boolean equals(String actionString) {
		return getActionString().equals(actionString);
	}

	public String toString() {
		return actionString;
	}

}
