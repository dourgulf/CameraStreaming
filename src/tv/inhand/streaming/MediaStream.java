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

import android.os.ParcelFileDescriptor;
import tv.inhand.streaming.rtmp.BasePacketizer;
import android.annotation.SuppressLint;
import android.media.MediaRecorder;
import android.util.Log;

/**
 * A MediaRecorder that streams what it records using a packetizer.
 * You can't use this class directly !
 */
public abstract class MediaStream implements Stream {

	protected static final String TAG = "MediaStream";

	/** The packetizer that will read the output of the camera and send packets over the network. */
	protected BasePacketizer mPacketizer = null;

	protected MediaRecorder mMediaRecorder;

	protected boolean mStreaming = false;

	protected ParcelFileDescriptor mReceiver, mSender = null;

	
	public MediaStream() {
	}

	/**
	 * Returns the packetizer associated with the {@link MediaStream}.
	 * @return The packetizer
	 */
	public BasePacketizer getPacketizer() {
		return mPacketizer;
	}

	/**
	 * Returns an approximation of the bitrate of the stream in bit per seconde.
	 */
	public long getBitrate() {
		return 0;
	}

	/**
	 * Indicates if the {@link MediaStream} is streaming.
	 * @return A boolean indicating if the {@link MediaStream} is streaming
	 */
	public boolean isStreaming() {
		return mStreaming;
	}

	/** Starts the stream. */
	public synchronized void start() throws IllegalStateException, IOException {
		encodeWithMediaRecorder();
	}

	/** Stops the stream. */
	public synchronized  void stop() {
		if (mStreaming) {
			mPacketizer.stop();
			try {
				try {
					mMediaRecorder.stop();
				}catch (Exception e) {}
				mMediaRecorder.reset();
				mMediaRecorder.release();
				mMediaRecorder = null;
				closeSockets();
			} catch (Exception e) {
				Log.e(TAG, "stop", e);
			}
			mStreaming = false;
		}
	}

	protected abstract void encodeWithMediaRecorder() throws IOException;

	protected void createSockets() throws IOException {
		ParcelFileDescriptor[] parcelFileDescriptors =ParcelFileDescriptor.createPipe();
		mReceiver = new ParcelFileDescriptor(parcelFileDescriptors[0]);
		mSender  = new ParcelFileDescriptor(parcelFileDescriptors[1]);
	}

	protected void closeSockets() {
		try {
			mSender.close();
			mSender = null;
			mReceiver.close();
			mReceiver = null;
		} catch (Exception ignore) {}
	}

}
