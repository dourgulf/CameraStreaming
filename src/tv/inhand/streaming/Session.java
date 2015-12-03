/*
 * Copyright (C) 2011-2013 GUIGUI Simon, fyhertz@gmail.com
 * 
 * This file is part of Spydroid (http://code.google.com/p/spydroid-ipcamera/)
 * 
 * Spydroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package tv.inhand.streaming;

import java.io.IOException;

import tv.inhand.streaming.audio.AudioStream;
import tv.inhand.streaming.video.VideoStream;
import android.content.Context;
import android.net.wifi.WifiManager;

/**
 * This class makes use of all the streaming package.
 * It represents a streaming session between a client and the phone.
 * A stream is designated by the word "track" in this class.
 * To add tracks to the session you need to call addVideoTrack() or addAudioTrack().
 */
public class Session {

	public final static String TAG = "Session";
	
	// Prevents threads from modifying two sessions simultaneously
	private static Object sLock = new Object();

	private Context mContext = null;
	private WifiManager.MulticastLock mLock = null;
	
	private AudioStream mAudioStream = null;
	private VideoStream mVideoStream = null;

	private Publisher mPublisher;

	/**
	 * Creates a streaming session that can be customized by adding tracks.
	 */
	public Session() {
	}


	public void addPublisheer(Publisher publisher) {
		mPublisher = publisher;
		mAudioStream.getPacketizer().setPublisher(mPublisher);
		mVideoStream.getPacketizer().setPublisher(mPublisher);
	}

	public void addAudioTrack(AudioStream track) {
		mAudioStream = track;
	}

	public void addVideoTrack(VideoStream track) {
		mVideoStream = track;
	}

	public void removeAudioTrack() {
		mAudioStream = null;
	}

	public void removeVideoTrack() {
		mVideoStream = null;
	}

	public AudioStream getAudioTrack() {
		return mAudioStream;
	}
	
	public VideoStream getVideoTrack() {
		return mVideoStream;
	}	
	
	/** 
	 * Reference to the context is needed to aquire a MulticastLock. 
	 * If the Session has a multicast destination is address such a lock will be aquired.
	 * @param context reference to the application context 
	 **/
	public void setContext(Context context) {
		mContext = context;
	}

	/**
	 * Returns an approximation of the bandwidth consumed by the session in bit per seconde. 
	 */
	public long getBitrate() {
		long sum = 0;
		if (mAudioStream != null) sum += mAudioStream.getBitrate();
		if (mVideoStream != null) sum += mVideoStream.getBitrate();
		return sum;
	}
	
	/** Indicates if a track is currently running. */
	public boolean isStreaming() {
		if ( (mAudioStream!=null && mAudioStream.isStreaming()) || (mVideoStream!=null && mVideoStream.isStreaming()) )
			return true;
		else 
			return false;
	}
	
	/** 
	 * Starts one stream.
	 * @param id The id of the stream to start
	 **/
	public void start(int id) throws IllegalStateException, IOException {
		synchronized (sLock) {
			Stream stream = id==0 ? mAudioStream : mVideoStream;
			if (stream!=null && !stream.isStreaming()) {
				stream.start();
			}
		}
	}

	/** Starts all streams. */
	public void start() throws IllegalStateException, IOException {
		start(0);
		start(1);
	}

	/** 
	 * Stops one stream.
	 * @param id The id of the stream to stop
	 **/	
	public void stop(int id) {
		synchronized (sLock) {
			// Release the MulticastLock if one was previous acquired
			if (mLock != null) {
				if (mLock.isHeld()) {
					mLock.release();
				}
				mLock = null;
			}
			Stream stream = id==0 ? mAudioStream : mVideoStream;
			if (stream!=null) {
				stream.stop();
			}
		}
	}	
	
	/** Stops all existing streams. */
	public void stop() {
		stop(0);
		stop(1);
	}

	public void startPublisher(String streamName) throws IOException {
		new Thread(new Runnable() {
			@Override
			public void run() {
				mPublisher.start(streamName, "live", null);
			}
		}).start();
	}

	public void stopPublisher() {
		mPublisher.stop();
	}
	/** Deletes all existing tracks & release associated resources. */
	public void flush() {
		synchronized (sLock) {
			if (mVideoStream!=null) {
				mVideoStream.stop();
				mVideoStream = null;
			}
			if (mAudioStream!=null) {
				mAudioStream.stop();
				mAudioStream = null;
			}
		}
	}

}
