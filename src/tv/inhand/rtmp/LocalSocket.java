package tv.inhand.rtmp;

import android.os.ParcelFileDescriptor;

import java.io.IOException;

/**
 * Created by jinchudarwin on 15/12/8.
 */
public class LocalSocket {
    protected ParcelFileDescriptor receiver, sender = null;
    public LocalSocket() throws IOException {
        ParcelFileDescriptor[] parcelFileDescriptors = ParcelFileDescriptor.createPipe();
        receiver = new ParcelFileDescriptor(parcelFileDescriptors[0]);
        sender  = new ParcelFileDescriptor(parcelFileDescriptors[1]);
    }

    public ParcelFileDescriptor getReceiver() {
        return receiver;
    }

    public ParcelFileDescriptor getSender() {
        return sender;
    }

    public void close() {
        try {
            sender.close();
            sender = null;
            receiver.close();
            receiver = null;
        } catch (Exception ignore) {}
    }
}
