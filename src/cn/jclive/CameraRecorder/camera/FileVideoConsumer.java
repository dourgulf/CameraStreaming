package cn.jclive.CameraRecorder.camera;

import android.util.Log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by jinchudarwin on 15/11/27.
 */
public class FileVideoConsumer implements VideoConsumer {
    private static String TAG = "JCameara";

    private FileOutputStream mFileOutput;

    public FileVideoConsumer(String filepath) throws FileNotFoundException {
        mFileOutput = new FileOutputStream(filepath);
        Log.d(TAG, "File output:" + filepath);
    }

    @Override
    public void begin() {
        Log.d(TAG, "begin");

    }

    @Override
    public void pushBuffer(long timestamp, byte[] buffer, int size) {
        Log.d(TAG, "push buffer, timestamp=" + timestamp + ", size=" + size + ", buffer size=" + buffer.length);
        try {
            mFileOutput.write(buffer, 0, size);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Write buffer exception:" + e.getMessage());
        }
    }

    @Override
    public void end() {
        Log.d(TAG, "end");
    }
}
