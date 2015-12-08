package tv.inhand.rtmp;

import android.util.Log;
import org.red5.io.flv.Tag;
import org.red5.server.messaging.IMessage;
import org.red5.server.net.rtmp.event.*;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.stream.message.RTMPMessage;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import android.os.ParcelFileDescriptor;

//class Packet{
//    public byte[] data;
//    public long timestamp;
//}

/**
 * Created by jinchudarwin on 15/12/2.
 */
abstract public class BasePacketizer {
    private final static String TAG = "BasePacketizer";
    // TODO: maybe give a valid value will be better.
    private final static int MAX_QUEUE_SIZE = Integer.MAX_VALUE;
    private LocalSocket localSocket;
    private BlockingQueue<Packet> dataQueue;
    private Thread producer;
    private Thread consumeer;

    protected InputStream is;
    protected Publisher publisher;

    protected int currentTime = 0;
    protected long timeBase = 0;
    protected int prevSize = 0;


    public BasePacketizer() throws IOException {
        localSocket = new LocalSocket();
        dataQueue = new LinkedBlockingDeque<>(MAX_QUEUE_SIZE);
        this.is = new ParcelFileDescriptor.AutoCloseInputStream(localSocket.getReceiver());
    }

    public FileDescriptor getWriteFileDescriptor() {
        return localSocket.getSender().getFileDescriptor();
    }

    public void setPublisher(Publisher publisher) {
        this.publisher = publisher;
    }

    public abstract boolean skipHeader();
    public abstract Packet fillPacket();
    public abstract void sendPacket(Packet packet);

    /** Starts the packetizer. */
    public void start() throws IOException{
        final String clsName = this.getClass().getSimpleName();

        producer = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, clsName + " producer running");
                boolean skipOK = skipHeader();
                if (!skipOK) {
                    Log.e(TAG, "Can't skip header, logical error");
                    throw new IllegalStateException("Can't skip header");
                }

                while (true) {
                    Packet p = fillPacket();
                    if (p == null) {
                        Log.w(TAG, clsName +" producer normal exit");
                        break;
                    }
                    try {
                        dataQueue.put(p);
                    } catch (InterruptedException e) {
                        Log.e(TAG, clsName +" producer thread interrupted");
                        break;
                    }
                }

                Log.i(TAG, clsName + " producer exited.");
            }
        });
        producer.start();

        consumeer = new Thread(new Runnable() {
            @Override
            public void run() {

                Log.i(TAG, clsName + " consumer normal exit");

                while(true) {
                    try {
                        Log.i(TAG, clsName + ": queue size:" + dataQueue.size());
                        Packet p = dataQueue.take();
                        if (p.data == null) {
                            Log.w(TAG, clsName + " consumer signal exit");
                            break;
                        }
                        sendPacket(p);
                    } catch (InterruptedException e) {
                        Log.e(TAG, clsName + " thread interrupted");
                        break;
                    }
                }

                Log.i(TAG, clsName + " consumer exited.");
            }
        });
        consumeer.start();
    }

    /** Stops the packetizer. */
    public void stop(){
        if (producer != null) {
            try {
                this.is.close();
            } catch (IOException e) {
            }
            producer.interrupt();
            try {
                producer.join(1000);
            } catch (InterruptedException e) {}
            producer = null;
        }
        if (consumeer != null) {
            try {
                dataQueue.put(new Packet(null, 0));
                consumeer.join(1000);
            } catch (InterruptedException e) {
                Log.e(TAG, "Try exit", e);
            }
            consumeer = null;
        }
    }

    /**
     * Send a message to publisher
     * @param message
     * @throws IOException
     */
    protected void send(IMessage message) throws IOException {
        publisher.pushMessage(message);
    }

    /**
     * Make a RTMP message from a tag.
     * @param tag
     * @return
     */
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

    /**
     * Encode a short value with big endian
     * @param val
     * @return
     */
    public static byte[] be16(short val)
    {
        byte[] buf = new byte[2];
        buf[1] = (byte)(val & 0xff);
        buf[0] = (byte)((val >> 8) & 0xff);

        return buf;
    }

    /**
     * Decode a big endian buffer to short value
     * @param buf
     * @return
     */
    public static short be16(byte[] buf) {
        int value = (buf[1] & 0xFF) | ((buf[0] & 0xFF) << 8);
        return (short)value;
    }

    /**
     * Encode a 24bit int value with big endian
     * @param val
     * @return
     */
    public static byte[] be24(int val)
    {
        byte[] buf = new byte[3];

        buf[2] = (byte)(val & 0xff);
        buf[1] = (byte)((val >> 8) & 0xff);
        buf[0] = (byte)((val >> 16) & 0xff);
        return buf;
    }

    /**
     * Decode a big endian buffer to 24bit int value
     * @param buf
     * @return
     */
    public static int be24(byte[] buf) {
        return (buf[2] & 0xFF) | ((buf[1] & 0xFF) << 8) | ((buf[0] & 0xFF) << 16);
    }

    /**
     * Encode a int value with big endian
     * @param val
     * @return
     */
    public static byte[] be32(int val) {
        byte[] buf = new byte[4];

        buf[4] = (byte)(val & 0xff);
        buf[2] = (byte)((val >> 8) & 0xff);
        buf[1] = (byte)((val >> 16) & 0xff);
        buf[0] = (byte)((val >> 24) & 0xff);
        return buf;
    }

    /**
     * Decode a big endian buffer to int value
     * @param buf
     * @return
     */
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
