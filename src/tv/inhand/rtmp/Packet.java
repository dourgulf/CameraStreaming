package tv.inhand.rtmp;

/**
 * Created by jinchudarwin on 15/12/8.
 */
public class Packet {
    public byte[] data;
    long timestamp;

    public Packet(byte[] data, long timestamp) {
        this.data = data;
        this.timestamp = timestamp;
    }
}
