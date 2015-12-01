package cn.jclive.CameraRecorder;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.PixelFormat;
import android.media.MediaRecorder;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import cn.jclive.CameraRecorder.camera.CameraUtils;
import cn.jclive.CameraRecorder.camera.FileVideoConsumer;
import cn.jclive.CameraRecorder.camera.VideoConsumer;
import com.jc.jclive.tools.FileUtils;

/**
 * class name：VideoCameraActivity<BR>
 * class description：CATCH THE VIDEODATA SEND TO RED5<BR>
 * PS： <BR>
 *
 * @version 1.00 2011/11/05
 * @author CODYY)peijiangping
 */
public class VideoCameraActivity extends Activity implements
        SurfaceHolder.Callback, MediaRecorder.OnErrorListener,
        MediaRecorder.OnInfoListener {
    private static final String TAG = "JCameara";

    private static final int mVideoEncoder = MediaRecorder.VideoEncoder.H264;
    private LocalSocket receiver, sender;
    private LocalServerSocket lss;
    private MediaRecorder mMediaRecorder = null;
    private boolean mMediaRecorderRecording = false;
    private SurfaceView mSurfaceView = null;
    private SurfaceHolder mSurfaceHolder = null;
    private Thread t;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.main2);
        mSurfaceView = (SurfaceView) this.findViewById(R.id.surfaceview);
        SurfaceHolder holder = mSurfaceView.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceView.setVisibility(View.VISIBLE);
        receiver = new LocalSocket();
        try {
            lss = new LocalServerSocket("VideoCamera");
            receiver.connect(new LocalSocketAddress("VideoCamera"));
            receiver.setReceiveBufferSize(500000);
            receiver.setSendBufferSize(500000);
            sender = lss.accept();
            sender.setReceiveBufferSize(500000);
            sender.setSendBufferSize(500000);
            Log.d(TAG, "local socket ok");
        } catch (IOException e) {
            Log.e(TAG, "local socket error");
            e.printStackTrace();
            finish();
            return;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mMediaRecorderRecording) {
            stopVideoRecording();
            try {
                lss.close();
                receiver.close();
                sender.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        finish();
    }

    private void stopVideoRecording() {
        Log.d(TAG, "stopVideoRecording");
        if (mMediaRecorderRecording || mMediaRecorder != null) {
            if (t != null)
                t.interrupt();
            releaseMediaRecorder();
        }
    }
    private File getOutputMediaFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String name = "record_" + timeStamp + ".mp4";
        return new File(FileUtils.getExternalAppDir(getApplicationContext()), name);
    }
    private void startVideoRecording() {
        Log.d(TAG, "startVideoRecording");

        (t = new Thread() {
            public void run() {
                int frame_size = 20000;
                byte[] buffer = new byte[1024 * 64];
                int num, number = 0;
                InputStream fis = null;
                try {
                    fis = receiver.getInputStream();
                } catch (IOException e1) {
                    Log.d(TAG, "receiver.getInputStream() exception");
                    return;
                }
                number = 0;
                releaseMediaRecorder();
                while (true) {
                    try {
                        num = fis.read(buffer, number, frame_size);
                        number += num;
                        if (num < frame_size) {
                            Log.d(TAG, "recoend break");
                            break;
                        }
                    } catch (IOException e) {
                        Log.d(TAG, "exception break");
                        break;
                    }
                }
                initializeVideo();
                number = 0;
                VideoConsumer consumer = null;
                try {
                    consumer = new FileVideoConsumer(getOutputMediaFile().getAbsolutePath());
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                Thread consumerThread = new Thread((Runnable) consumer);
                consumer.begin();
                DataInputStream dis = new DataInputStream(fis);
                try {
                    dis.read(buffer, 0, 32);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                byte[] aa = { 0x01, 0x42, (byte) 0x80, 0x0A, (byte) 0xFF,
                        (byte) 0xE1, 0x00, 0x12, 0x67, 0x42, (byte) 0x80, 0x0A,
                        (byte) 0xE9, 0x02, (byte) 0xC1, 0x29, 0x08, 0x00, 0x00,
                        0x1F, 0x40, 0x00, 0x04, (byte) 0xE2, 0x00, 0x20, 0x01,
                        0x00, 0x04, 0x68, (byte) 0xCE, 0x3C, (byte) 0x80 };
                consumer.pushBuffer(System.currentTimeMillis(), aa, 33);
                while (true) {
                    try {
                        int h264length = dis.readInt();
                        number = 0;
                        while (number < h264length) {
                            int lost = h264length - number;
                            num = fis.read(buffer, 0,
                                    frame_size < lost ? frame_size : lost);
                            number += num;
                            consumer.pushBuffer(System.currentTimeMillis(),
                                    buffer, num);
                        }
                    } catch (IOException e) {
                        break;
                    }
                }
                consumer.end();
            }
        }).start();
    }

    private boolean initializeVideo() {

        Log.d(TAG, "initializeVideo");
        if (mSurfaceHolder == null)
            return false;

        mMediaRecorderRecording = true;
        if (mMediaRecorder == null)
            mMediaRecorder = new MediaRecorder();
        else
            mMediaRecorder.reset();

        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());

        mMediaRecorder.setOutputFile(sender.getFileDescriptor());
        try {
            mMediaRecorder.prepare();
            Log.d(TAG, "bf start");
            mMediaRecorder.start();
            Log.d(TAG, "be start");

        } catch (Exception exception) {
            Log.d(TAG, "initializeVideo exception:" + exception);
            releaseMediaRecorder();
            finish();

        }
        Log.d(TAG, "initializeVideo ok");


        return true;
    }

    private void releaseMediaRecorder() {
        Log.d(TAG, "Releasing media recorder.");
        if (mMediaRecorder != null) {
            if (mMediaRecorderRecording) {
                try {
                    mMediaRecorder.setOnErrorListener(null);
                    mMediaRecorder.setOnInfoListener(null);
                    mMediaRecorder.stop();
                } catch (RuntimeException e) {
                    Log.d(TAG, "stop fail: " + e.getMessage());
                }
                mMediaRecorderRecording = false;
            }
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        Log.d(TAG, "surfaceChanged");
        mSurfaceHolder = holder;
        if (!mMediaRecorderRecording) {
            initializeVideo();
            startVideoRecording();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated");
        mSurfaceHolder = holder;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed");
        mSurfaceView = null;
        mSurfaceHolder = null;
        mMediaRecorder = null;
        if (t != null) {
            t.interrupt();
        }
    }

    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        switch (what) {
            case MediaRecorder.MEDIA_RECORDER_INFO_UNKNOWN:
                Log.d(TAG, "MEDIA_RECORDER_INFO_UNKNOWN");
                break;
            case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED:
                Log.d(TAG, "MEDIA_RECORDER_INFO_MAX_DURATION_REACHED");
                break;
            case MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED:
                Log.d(TAG, "MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED");
                break;
        }
    }

    @Override
    public void onError(MediaRecorder mr, int what, int extra) {
        if (what == MediaRecorder.MEDIA_RECORDER_ERROR_UNKNOWN) {
            Log.d(TAG, "MEDIA_RECORDER_ERROR_UNKNOWN");
            finish();
        }
    }

}