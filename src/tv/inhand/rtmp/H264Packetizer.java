package tv.inhand.rtmp;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.IoConstants;
import org.red5.io.flv.Tag;
import org.red5.server.messaging.IMessage;

import java.io.IOException;
import android.util.Log;

/**
 * 
 *   H.264 streaming over RTMP.
 *   
 *   Must be fed with an InputStream containing H.264 NAL units preceded by their length (4 bytes).
 *   The stream must start with mpeg4 or 3gpp header, it will be skipped.
 *   
 */
public class H264Packetizer extends BasePacketizer{

	public final static String TAG = "H264Packetizer";
	private final static int MAX_VALID_NALU_LENGTH = 100000;
	private final static int flagSize = 5;


	private int naluLength = 0;
	private byte[] sps = null, pps = null;
	private boolean sentConfig = false;
	
	public H264Packetizer() throws IOException {
		super();
	}

    public boolean skipHeader(){
		try {
			byte buffer[] = new byte[4];
			// Skip all atoms preceding mdat atom
			while (!Thread.interrupted()) {
				while (is.read() != 'm');
				is.read(buffer,0,3);
				if (buffer[0] == 'd' && buffer[1] == 'a' && buffer[2] == 't') {
					Log.i(TAG, "Skip MP4 header");
					return true;
				}
			}
		} catch (IOException e) {
			Log.e(TAG,"Couldn't skip MP4 header :/", e);
		}
        return false;
    }

    @Override
    public Packet fillPacket(){
        try {
            return new Packet(fillNalu(), System.currentTimeMillis());
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void sendPacket(Packet packet){
        try {
            processNalu(packet);
        } catch (IOException e) {
            Log.e(TAG, "Send packet exception", e);
        }
    }
	public void setStreamParameters(byte[] pps, byte[] sps) {
		this.pps = pps;
		this.sps = sps;
		Log.i(TAG, "PPS:" + printBuffer(pps, 0, pps.length));
		Log.i(TAG, "SPS:" + printBuffer(sps, 0, sps.length));
	}

	private byte[] fillNalu()  throws IOException{
		byte[] header = new byte[4];

		// Read NAL unit length
		fill(header, 0, 4);
		naluLength = be32(header);

		if (naluLength>MAX_VALID_NALU_LENGTH || naluLength<0)
			resync();

		byte[] nalu = new byte[naluLength + header.length];
		System.arraycopy(header, 0, nalu, 0, header.length);

		fill(nalu, header.length, naluLength);	// We had fill the first byte
		return nalu;
	}

	private void processNalu(Packet packet) throws IOException {
		// TODO: 还需要处理分片的情况
        byte[] nalu = packet.data;
		int nalType = nalu[4] & 0x1F;
		if (nalType == 5 && !sentConfig) {
			Log.i(TAG, "Send configuration one time");
			byte[] conf = configurationFromSpsAndPps();
			writeVideoNalu(conf, System.currentTimeMillis(), 0, true);
			sentConfig = true;
		}
        if (nalType == 7 || nalType == 8) {
            Log.w(TAG, "Received SPS/PPS frame, ignored");
        }
		writeVideoNalu(nalu, packet.timestamp, 1, (nalType == 5));
	}

	private void writeVideoNalu(byte[] nalu, long timestamp, int avctype, boolean keyframe) throws IOException {
		byte flag = IoConstants.FLAG_CODEC_H264;
		if (keyframe) {
			flag |= (IoConstants.FLAG_FRAMETYPE_KEYFRAME << 4);
		}else {
			flag |= (IoConstants.FLAG_FRAMETYPE_INTERFRAME << 4);
		}

		if (timeBase == 0) {
			timeBase = timestamp;
		}
		int currentTime = (int) (timestamp - timeBase);
		Tag tag = new Tag(IoConstants.TYPE_VIDEO, currentTime, nalu.length+flagSize, null, prevSize);
		prevSize = nalu.length+flagSize;

		IoBuffer body = IoBuffer.allocate(tag.getBodySize());

		body.setAutoExpand(true);
		body.put(flag);
		body.put((byte)avctype);

		int dts = 0; 	// TODO: 使用正确的DTS和PTS
		int pts = 0;	// TODO:
		// TODO: if x264 come with B-frame, delay must set to correct value.
		int delay = (pts-dts)/90;
		body.put(be24(delay));

		body.put(nalu);

		body.flip();
		body.limit(tag.getBodySize());
		tag.setBody(body);

		IMessage msg = makeMessageFromTag(tag);
		send(msg);
	}

	private byte[] configurationFromSpsAndPps() {
		if (sps == null || pps == null) {
			Log.e(TAG, "Invalid sps or pps");
			throw new IllegalStateException("SPS|PPS missed");
		}

		IoBuffer conf = IoBuffer.allocate(9);
		conf.setAutoExpand(true);
		conf.put((byte)1);	// version
		conf.put(sps[1]);	// profile
		conf.put(sps[2]);	// compat
		conf.put(sps[3]);	// level
		conf.put((byte)0xff);	// 6 bits reserved + 2 bits nal size length - 1 (11)
		conf.put((byte)0xe1); 	// 3 bits reserved + 5 bits number of sps (00001)

		conf.put(be16((short)sps.length));
		conf.put(sps);

		conf.put((byte)1);
		conf.put(be16((short)pps.length));
		conf.put(pps);

		return conf.array();
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

	private void resync() throws IOException {
		byte[] header = new byte[5];
		int type;

		Log.e(TAG,"Packetizer out of sync ! Let's try to fix that...");
		
		while (true) {

			header[0] = header[1];
			header[1] = header[2];
			header[2] = header[3];
			header[3] = header[4];
			header[4] = (byte) is.read();

			type = header[4]&0x1F;

			if (type == 5 || type == 1) {
                naluLength = be32(header);
				if (naluLength>0 && naluLength<MAX_VALID_NALU_LENGTH) {
					Log.e(TAG,"A NAL unit may have been found in the bit stream !");
					break;
				}
			}
		}
	}
}