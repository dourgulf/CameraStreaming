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
import java.io.InterruptedIOException;

import android.util.Log;
import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.IoConstants;
import org.red5.io.flv.Tag;
import org.red5.server.messaging.IMessage;
import org.red5.server.net.rtmp.event.*;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.stream.message.RTMPMessage;
import tv.inhand.streaming.audio.AACStream;

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
	private final static int flagSize = 2;

	private Thread t;
	private int samplingRate = 8000;
	private int samplingRateIndex;
	private boolean sendAsc = false;
    private int channelCount = 2;

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

		Log.i(TAG,"AAC ADTS packetizer started !");
		try {
			while (!Thread.interrupted()) {
				byte[] frameBuf = fillFrame();
				processFrame(frameBuf);
			}
		} catch (IOException e) {
			Log.e(TAG, "run IOException", e);
		} catch (Exception e) {
			Log.e(TAG, "run Exception", e);
		}

		Log.i(TAG,"AAC ADTS packetizer stopped !");

	}

	private byte[] fillFrame()  throws IOException{
		byte[] header = new byte[8];
		// Synchronisation: ADTS packet starts with 12bits set to 1
		while (true) {
			if ( (is.read()&0xFF) == 0xFF ) {
				header[1] = (byte) is.read();
				if ( (header[1]&0xF0) == 0xF0) break;
			}
		}

		// Parse adts header (ADTS packets start with a 7 or 9 byte long header)
		fill(header, 2, 5);

		// The protection bit indicates whether or not the header contains the two extra bytes
		boolean protection = (header[1]&0x01)>0 ? true : false;

		int frameLength = (header[3]&0x03) << 11 |
				(header[4]&0xFF) << 3 |
				(header[5]&0xFF) >> 5 ;
		frameLength -= (protection ? 7 : 9);

		// Number of AAC frames in the ADTS frame
		int nbau = (header[6]&0x03) + 1;

		// Read CRS if any
		if (!protection) {
			is.read(header, 0, 2);
		}
        else {
            // restore the header
            header[0] = (byte)0xFF;
            header[1] = (byte)0xF0;
        }

		samplingRateIndex = (header[2]&0x3C) >> 2;
		samplingRate = AACStream.AUDIO_SAMPLING_RATES[samplingRateIndex];

		int profile = ( (header[2]&0xC0) >> 6 ) + 1 ;

//		Log.i(TAG,"frameLength: "+frameLength+" protection: "+protection+" p: "+profile+" sr: "+samplingRate+ ", header:" + printBuffer(header, 0, header.length));

		byte[] frameBuf = new byte[frameLength];
//        System.arraycopy(header, 0, frameBuf, 0, 7);
		fill(frameBuf, 0, frameLength);
		return frameBuf;
	}

	private void processFrame(byte[] frame) throws IOException {
		if (!sendAsc) {
			writeAudioBuffer(makeAsc(samplingRateIndex), 0);
			sendAsc = true;
		}
		writeAudioBuffer(frame, 1);
	}
	private void writeAudioBuffer(byte[] buf, int avctype) throws IOException{
		long timestamp = System.currentTimeMillis();

		if (timeBase == 0) {
			timeBase = timestamp;
		}
		currentTime = (int) (timestamp - timeBase);
		Tag tag = new Tag(IoConstants.TYPE_AUDIO, currentTime, buf.length + flagSize, null,
				prevSize);
		prevSize = buf.length + flagSize;

		byte tagType = (byte) ((IoConstants.FLAG_FORMAT_AAC << 4))
				| (IoConstants.FLAG_SIZE_16_BIT << 1);

        // Only 44KHZ supported!
		tagType |= IoConstants.FLAG_RATE_44_KHZ << 2;
//        switch (samplingRate) {
//            case 44100:
//                tagType |= IoConstants.FLAG_RATE_44_KHZ << 2;
//                break;
//            case 22050:
//                tagType |= IoConstants.FLAG_RATE_22_KHZ << 2;
//                break;
//            case 11025:
//                tagType |= IoConstants.FLAG_RATE_11_KHZ << 2;
//                break;
//            default:
//                tagType |= IoConstants.FLAG_RATE_5_5_KHZ << 2;
//        }

		// FIXME: AudioStream already fixed the channel count is 1, so mono only!
		tagType |= (channelCount == 2 ? IoConstants.FLAG_TYPE_STEREO : IoConstants.FLAG_TYPE_MONO);

		IoBuffer body = IoBuffer.allocate(tag.getBodySize());
		body.setAutoExpand(true);
		body.put(tagType);
		body.put((byte)avctype);
		body.put(buf);
		body.flip();
		body.limit(tag.getBodySize());
		tag.setBody(body);

		byte[] bodyBuf = body.array();

//		Log.i(TAG, "frame buffer:" + printBuffer(bodyBuf, 0, tag.getBodySize()<64?tag.getBodySize():64));

		IMessage msg = makeMessageFromTag(tag);
		send(msg);
	}
	private byte[] makeAsc(int sampleRateIndex)
	{
		// http://wiki.multimedia.cx/index.php?title=MPEG-4_Audio#Audio_Specific_Config
		byte asc[] = new byte[2];
		asc[0] = (byte) ( 0x10 | ((sampleRateIndex>>1) & 0x3) );
		asc[1] = (byte) ( ((sampleRateIndex & 0x1)<<7) | ((channelCount & 0xF) << 3) );

		Log.i(TAG, "asc:" + printBuffer(asc, 0, asc.length));
		return asc;
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
