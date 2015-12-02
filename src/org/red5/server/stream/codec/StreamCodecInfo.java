package org.red5.server.stream.codec;

/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright (c) 2006-2010 by respective authors (see below). All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU Lesser General Public License as published by the Free Software 
 * Foundation; either version 2.1 of the License, or (at your option) any later 
 * version. 
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this library; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 */


public class StreamCodecInfo implements IStreamCodecInfo {
	
	/**
	 * Audio support flag
	 */
	private boolean audio;

	/**
	 * Video support flag
	 */
	private boolean video;

	/**
	 * Audio codec
	 */
	private IAudioStreamCodec audioCodec;

	/**
	 * Video codec
	 */
	private IVideoStreamCodec videoCodec;	
	
	/** {@inheritDoc} */
	public boolean hasAudio() {
		return audio;
	}

	/**
	 * New value for audio support
	 *
	 * @param value Audio support
	 */
	public void setHasAudio(boolean value) {
		this.audio = value;
	}
	
	/** {@inheritDoc} */
	public String getAudioCodecName() {
		if (audioCodec == null) {
			return null;
		}
		return audioCodec.getName();
	}	
	
	/** {@inheritDoc} */
	public IAudioStreamCodec getAudioCodec() {
		return audioCodec;
	}

	/**
	 * Setter for audio codec
	 *
	 * @param codec Audio codec
	 */
	public void setAudioCodec(IAudioStreamCodec codec) {
		this.audioCodec = codec;
	}	

	/** {@inheritDoc} */
	public boolean hasVideo() {
		return video;
	}

	/**
	 * New value for video support
	 *
	 * @param value Video support
	 */
	public void setHasVideo(boolean value) {
		this.video = value;
	}

	/** {@inheritDoc} */
	public String getVideoCodecName() {
		if (videoCodec == null) {
			return null;
		}
		return videoCodec.getName();
	}

	/** {@inheritDoc} */
	public IVideoStreamCodec getVideoCodec() {
		return videoCodec;
	}

	/**
	 * Setter for video codec
	 *
	 * @param codec  Video codec
	 */
	public void setVideoCodec(IVideoStreamCodec codec) {
		this.videoCodec = codec;
	}
	
}
