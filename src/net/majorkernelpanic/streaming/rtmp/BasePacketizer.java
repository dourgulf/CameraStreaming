package net.majorkernelpanic.streaming.rtmp;

import org.red5.server.stream.message.RTMPMessage;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by jinchudarwin on 15/12/2.
 */
abstract public class BasePacketizer {
    protected InputStream is = null;
    protected byte[] buffer;

    protected long ts = 0, intervalBetweenReports = 5000, delta = 0;

    public BasePacketizer() throws IOException {
    }

    public void setInputStream(InputStream is) {
        this.is = is;
    }


    /** Starts the packetizer. */
    public abstract void start() throws IOException;

    /** Stops the packetizer. */
    public abstract void stop();

    /** Updates data for RTCP SR and sends the packet. */
    protected void send(RTMPMessage message) throws IOException {
    }

    /** For debugging purposes. */
    protected static String printBuffer(byte[] buffer, int start,int end) {
        String str = "";
        for (int i=start;i<end;i++) str+=","+Integer.toHexString(buffer[i]&0xFF);
        return str;
    }
}
