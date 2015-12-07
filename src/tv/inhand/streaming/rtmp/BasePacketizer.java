package tv.inhand.streaming.rtmp;

//import net.majorkernelpanic.streaming.rtp.RtpSocket;
import org.red5.io.flv.Tag;
import org.red5.server.messaging.IMessage;
import org.red5.server.net.rtmp.event.*;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.stream.message.RTMPMessage;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by jinchudarwin on 15/12/2.
 */
abstract public class BasePacketizer {
    protected InputStream is = null;
    protected Publisher publisher;

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

    /**
     * Send a message to publisher
     * @param message
     * @throws IOException
     */
    protected void send(IMessage message) throws IOException {
        publisher.pushMessage(message);
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

    // big endian encode & decode method.
    public static byte[] be16(short val)
    {
        byte[] buf = new byte[2];
        buf[1] = (byte)(val & 0xff);
        buf[0] = (byte)((val >> 8) & 0xff);

        return buf;
    }

    public static short be16(byte[] buf) {
        int value = (buf[1] & 0xFF) | ((buf[0] & 0xFF) << 8);
        return (short)value;
    }

    public static byte[] be24(int val)
    {
        byte[] buf = new byte[3];

        buf[2] = (byte)(val & 0xff);
        buf[1] = (byte)((val >> 8) & 0xff);
        buf[0] = (byte)((val >> 16) & 0xff);
        return buf;
    }

    public static int be24(byte[] buf) {
        return (buf[2] & 0xFF) | ((buf[1] & 0xFF) << 8) | ((buf[0] & 0xFF) << 16);
    }

    public static byte[] be32(int val) {
        byte[] buf = new byte[4];

        buf[4] = (byte)(val & 0xff);
        buf[2] = (byte)((val >> 8) & 0xff);
        buf[1] = (byte)((val >> 16) & 0xff);
        buf[0] = (byte)((val >> 24) & 0xff);
        return buf;
    }

    public static int be32(byte[] buf) {
        return (buf[3] & 0xFF) | ((buf[2] & 0xFF) << 8) | ((buf[1] & 0xFF) << 16) | ((buf[0] & 0xFF) << 24);
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
