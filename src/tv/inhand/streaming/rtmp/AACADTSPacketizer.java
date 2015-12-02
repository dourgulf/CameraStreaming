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

package tv.inhand.streaming.rtmp;

import java.io.IOException;

import android.util.Log;

/**
 *   
 *   RFC 3640.  
 *
 *   This packetizer must be fed with an InputStream containing ADTS AAC. 
 *   AAC will basically be rewrapped in an RTP stream and sent over the network.
 *   This packetizer only implements the aac-hbr mode (High Bit-rate AAC) and
 *   each packet only carry a single and complete AAC access unit.
 * 
 */
public class AACADTSPacketizer extends BasePacketizer implements Runnable {

	private final static String TAG = "AACADTSPacketizer";

	private Thread t;
	private int samplingRate = 8000;

	public AACADTSPacketizer() throws IOException {
		super();
	}

	public void start() {
		if (t==null) {
			t = new Thread(this);
			t.start();
		}
	}

	public void stop() {
		if (t != null) {
			try {
				is.close();
			} catch (IOException ignore) {}
			t.interrupt();
			try {
				t.join();
			} catch (InterruptedException e) {}
			t = null;
		}
	}

	public void setSamplingRate(int samplingRate) {
		this.samplingRate = samplingRate;
	}

	public void run() {

		Log.d(TAG,"AAC ADTS packetizer started !");

		Log.d(TAG,"AAC ADTS packetizer stopped !");

	}

	private int fill(byte[] buffer, int offset,int length) throws IOException {
		int sum = 0, len;
		while (sum<length) {
			len = is.read(buffer, offset+sum, length-sum);
			if (len<0) {
				throw new IOException("End of stream");
			}
			else sum+=len;
		}
		return sum;
	}

}
