package cn.jclive.CameraRecorder.camera;

/**
 * Created by jinchudarwin on 15/11/27.
 */
public interface VideoConsumer {
    void begin();
    void pushBuffer(long timestamp, byte[] buffer, int size);
    void end();
}
