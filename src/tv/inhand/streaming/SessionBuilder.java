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

import android.content.Context;
import android.hardware.Camera.CameraInfo;
import android.preference.PreferenceManager;
import android.view.SurfaceHolder;
import tv.inhand.streaming.audio.AACStream;
import tv.inhand.streaming.audio.AudioQuality;
import tv.inhand.streaming.audio.AudioStream;
import tv.inhand.streaming.video.H264Stream;
import tv.inhand.streaming.video.VideoQuality;
import tv.inhand.streaming.video.VideoStream;

/**
 * Call {@link #getInstance()} to get access to the SessionBuilder.
 */
public class SessionBuilder {

	public final static String TAG = "SessionBuilder";

	private String mHost;
	private int mPort = 1935;
	private String mAppName;
	private String mStringName;

	// Default configuration
	private VideoQuality mVideoQuality = new VideoQuality();
	private AudioQuality mAudioQuality = new AudioQuality();
	private Context mContext;
	private int mCamera = CameraInfo.CAMERA_FACING_BACK;
	private boolean mFlash = false;
	private SurfaceHolder mSurfaceHolder = null;

	// Removes the default public constructor
	private SessionBuilder() {}

	// The SessionManager implements the singleton pattern
	private static volatile SessionBuilder sInstance = null;

	/**
	 * Returns a reference to the {@link SessionBuilder}.
	 * @return The reference to the {@link SessionBuilder}
	 */
	public final static SessionBuilder getInstance() {
		if (sInstance == null) {
			synchronized (SessionBuilder.class) {
				if (sInstance == null) {
					SessionBuilder.sInstance = new SessionBuilder();
				}
			}
		}
		return sInstance;
	}

	/**
	 * Creates a new {@link Session}.
	 * @return The new Session
	 * @throws IOException
	 */
	public Session build() throws IOException {
		Session session;

		session = new Session();
		session.setContext(mContext);

		{
			AACStream stream = new AACStream();
			session.addAudioTrack(stream);
			if (mContext != null)
				stream.setPreferences(PreferenceManager.getDefaultSharedPreferences(mContext));
		}

		{
			H264Stream stream = new H264Stream(mCamera);
			if (mContext!=null)
				stream.setPreferences(PreferenceManager.getDefaultSharedPreferences(mContext));
			session.addVideoTrack(stream);
		}

		if (session.getVideoTrack()!=null) {
			VideoStream video = session.getVideoTrack();
			video.setFlashState(mFlash);
			video.setVideoQuality(VideoQuality.merge(mVideoQuality,video.getVideoQuality()));
			video.setPreviewDisplay(mSurfaceHolder);
		}

		if (session.getAudioTrack()!=null) {
			AudioStream audio = session.getAudioTrack();
			audio.setAudioQuality(AudioQuality.merge(mAudioQuality,audio.getAudioQuality()));
		}

		{
			Publisher publisher = new Publisher();
			publisher.setHost(mHost);
			publisher.setPort(mPort);
			publisher.setApp(mAppName);
			session.addPublisheer(publisher);
		}
		return session;

	}

	public SessionBuilder setHost(String host) {
		mHost = host;
		return this;
	}

	public SessionBuilder setPort(int port) {
		mPort = port;
		return this;
	}

	public SessionBuilder setAppName(String app) {
		mAppName = app;
		return this;
	}

	/**
	 * Access to the context is needed for the H264Stream class to store some stuff in the SharedPreferences.
	 * Note that you should pass the Application context, not the context of an Activity.
	 **/
	public SessionBuilder setContext(Context context) {
		mContext = context;
		return this;
	}

	/** Sets the video stream quality. */
	public SessionBuilder setVideoQuality(VideoQuality quality) {
		mVideoQuality = VideoQuality.merge(quality, mVideoQuality);
		return this;
	}


	/** Sets the audio quality. */
	public SessionBuilder setAudioQuality(AudioQuality quality) {
		mAudioQuality = AudioQuality.merge(quality, mAudioQuality);
		return this;
	}


	public SessionBuilder setFlashEnabled(boolean enabled) {
		mFlash = enabled;
		return this;
	}

	public SessionBuilder setCamera(int camera) {
		mCamera = camera;
		return this;
	}

	/**
	 * Sets the Surface required by MediaRecorder to record video.
	 * @param surfaceHolder A SurfaceHolder wrapping a valid surface
	 **/
	public SessionBuilder setSurfaceHolder(SurfaceHolder surfaceHolder) {
		mSurfaceHolder = surfaceHolder;
		return this;
	}

	/** Returns the context set with {@link #setContext(Context)}*/
	public Context getContext() {
		return mContext;
	}

	/** Returns the id of the {@link android.hardware.Camera} set with {@link #setCamera(int)}. */
	public int getCamera() {
		return mCamera;
	}

	/** Returns the VideoQuality set with {@link #setVideoQuality(VideoQuality)}. */
	public VideoQuality getVideoQuality() {
		return mVideoQuality;
	}

	/** Returns the AudioQuality set with {@link #setAudioQuality(AudioQuality)}. */
	public AudioQuality getAudioQuality() {
		return mAudioQuality;
	}

	/** Returns the flash state set with {@link #setFlashEnabled(boolean)}. */
	public boolean getFlashState() {
		return mFlash;
	}

	/** Returns the SurfaceHolder set with {@link #setSurfaceHolder(SurfaceHolder)}. */
	public SurfaceHolder getSurfaceHolder() {
		return mSurfaceHolder;
	}

	/** Returns a new {@link SessionBuilder} with the same configuration. */
	public SessionBuilder clone() {
		return new SessionBuilder()
				.setSurfaceHolder(mSurfaceHolder)
				.setHost(mHost)
				.setPort(mPort)
				.setAppName(mAppName)
				.setVideoQuality(mVideoQuality)
				.setFlashEnabled(mFlash)
				.setCamera(mCamera)
				.setAudioQuality(mAudioQuality)
				.setContext(mContext);
	}
}