package tv.inhand.streaming.rtmp;

//import net.majorkernelpanic.streaming.rtp.RtpSocket;
import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.IoConstants;
import org.red5.io.flv.Tag;
import org.red5.server.messaging.IMessage;
import org.red5.server.net.rtmp.event.*;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.stream.message.RTMPMessage;
import tv.inhand.streaming.Publisher;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by jinchudarwin on 15/12/2.
 */
abstract public class BasePacketizer {
    protected InputStream is = null;
    protected Publisher publisher;
    protected byte[] buffer;

    protected int currentTime = 0;
    protected long timeBase = 0;
    protected int prevSize = 0;


    public BasePacketizer() throws IOException {
    }

    public void setPublisher(Publisher publisher) {
        this.publisher = publisher;
    }

    public void setInputStream(InputStream is) {
        this.is = is;
    }


    /** Starts the packetizer. */
    public abstract void start() throws IOException;

    /** Stops the packetizer. */
    public abstract void stop();

    protected void send(IMessage message) throws IOException {
        publisher.pushMessage(message);
    }
    public void writeAudioBuffer(byte[] buf, int size, long ts) {
        if (timeBase == 0) {
            timeBase = ts;
        }
        currentTime = (int) (ts - timeBase);
        Tag tag = new Tag(IoConstants.TYPE_AUDIO, currentTime, size + 1, null,
                prevSize);
        prevSize = size + 1;

        byte tagType = (byte) ((IoConstants.FLAG_FORMAT_AAC << 4))
                | (IoConstants.FLAG_SIZE_16_BIT << 1);

        tagType |= IoConstants.FLAG_RATE_44_KHZ << 2;
//        switch (sampleRate) {
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

        tagType |= IoConstants.FLAG_TYPE_STEREO;
        IoBuffer body = IoBuffer.allocate(tag.getBodySize());
        body.setAutoExpand(true);
        body.put(tagType);
        body.put(buf);
        body.flip();
        body.limit(tag.getBodySize());
        tag.setBody(body);

        IMessage msg = makeMessageFromTag(tag);
        try {
            publisher.pushMessage(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public IMessage makeMessageFromTag(Tag tag) {
        IRTMPEvent msg = null;
        switch (tag.getDataType()) {
            case Constants.TYPE_AUDIO_DATA:
                msg = new AudioData(tag.getBody());
                break;
            case Constants.TYPE_VIDEO_DATA:
                msg = new VideoData(tag.getBody());
                break;
            case Constants.TYPE_INVOKE:
                msg = new Invoke(tag.getBody());
                break;
            case Constants.TYPE_NOTIFY:
                msg = new Notify(tag.getBody());
                break;
            case Constants.TYPE_FLEX_STREAM_SEND:
                msg = new FlexStreamSend(tag.getBody());
                break;
            default:
                msg = new Unknown(tag.getDataType(), tag.getBody());
        }
        msg.setTimestamp(tag.getTimestamp());
        RTMPMessage rtmpMsg = new RTMPMessage();
        rtmpMsg.setBody(msg);
        rtmpMsg.getBody();
        return rtmpMsg;
    }

    public static byte[] be16(short val)
    {
        byte[] buf = new byte[2];
        buf[1] = (byte)(val & 0xff);
        buf[0] = (byte)((val >> 8) & 0xff);

        return buf;
    }

    public static byte[] be24(int val)
    {
        byte[] buf = new byte[3];

        buf[2] = (byte)(val & 0xff);
        buf[1] = (byte)((val >> 8) & 0xff);
        buf[0] = (byte)((val >> 16) & 0xff);
        return buf;
    }
    public static byte[] be32(int val) {
        byte[] buf = new byte[4];

        buf[4] = (byte)(val & 0xff);
        buf[2] = (byte)((val >> 8) & 0xff);
        buf[1] = (byte)((val >> 16) & 0xff);
        buf[0] = (byte)((val >> 24) & 0xff);
        return buf;
    }

    /** For debugging purposes. */
    protected static String printBuffer(byte[] buffer, int start,int end) {
        StringBuilder str = new StringBuilder();
        for (int i=start;i<end;i++) {
            str.append(Integer.toHexString(buffer[i]&0xFF)).append(",");
        }
        return str.toString();
    }
}
