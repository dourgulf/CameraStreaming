package cn.jclive.CameraRecorder;

import android.app.Activity;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.*;
import android.view.*;
import android.widget.*;
import cn.jclive.CameraRecorder.camera.*;
import com.jc.jclive.tools.FileUtils;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by jinchudarwin on 15/11/27.
 */
public class CameraTest extends Activity {
    private static final String TAG = "JCameara";

    private Camera mCamera;
    private CameraPreview mPreview;
    private MediaRecorder mMediaRecorder;
    private boolean isRecording = false;

    ParcelFileDescriptor parcelRead, parcelWrite;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cameratest);

        // Create an instance of Camera
        mCamera = CameraUtils.getCameraInstance();

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        // Add a listener to the Capture button
        Button captureButton = (Button) findViewById(R.id.button_capture);
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, "onClick");
                        if (isRecording) {
                            // local socket must be release before stopping MediaRecorder.
                            releaseLocalSocket();
                            // stop recording and release camera
                            mMediaRecorder.stop();  // stop the recording
                            releaseMediaRecorder(); // release the MediaRecorder object
                            mCamera.lock();         // take camera access back from MediaRecorder
                            // inform the user that recording has stopped
                            setCaptureButtonText("Capture");
                            isRecording = false;
                        } else {
                            if (prepareLocalSocket()) {
                                // initialize video camera
                                if (prepareVideoRecorder()) {
                                    // Camera is available and unlocked, MediaRecorder is prepared,
                                    // now you can start recording
                                    try {
                                        mMediaRecorder.start();
                                    }catch (Exception e) {
                                        Log.e(TAG, "Start record exception:" + e + ", " + e.getMessage());
                                        e.printStackTrace();
                                        return ;
                                    }
                                    runConsumerThread();
                                    // inform the user that recording has started
                                    setCaptureButtonText("Stop");
                                    isRecording = true;
                                } else {
                                    // prepare didn't work, release the camera
                                    releaseMediaRecorder();
                                    // inform user
                                    Log.e(TAG, "ERROR prepare video recorder");
                                }
                            }
                            else {
                                releaseLocalSocket();
                                Log.e(TAG, "ERROR prepare local socket");
                            }
                        }
                    }
                }
        );
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
        releaseCamera();              // release the camera immediately on pause event
    }

    private void setCaptureButtonText(String text) {
        Button captureButton = (Button) findViewById(R.id.button_capture);
        captureButton.setText(text);
    }

    private boolean prepareVideoRecorder() {
        mMediaRecorder = new MediaRecorder();

        // Step 1: Unlock and set camera to MediaRecorder
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        // Step 2: Set sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
//        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

        // Step 3: Set output format and encoding (for versions prior to API Level 8)
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioChannels(1);
        mMediaRecorder.setAudioEncodingBitRate(64000);
        mMediaRecorder.setAudioSamplingRate(44100);

        // Step 4: Set output file
        mMediaRecorder.setOutputFile(parcelWrite.getFileDescriptor());

        // Step 5: Set the preview output
        mMediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());

        // Set event listener
        mMediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
            @Override
            public void onInfo(MediaRecorder mr, int what, int extra) {
                switch (what) {
                    case MediaRecorder.MEDIA_RECORDER_INFO_UNKNOWN:
                        Log.i(TAG, "MEDIA_RECORDER_INFO_UNKNOWN, " + extra);
                        break;
                    case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED:
                        Log.i(TAG, "MEDIA_RECORDER_INFO_MAX_DURATION_REACHED, " + extra);
                        break;
                    case MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED:
                        Log.i(TAG, "MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED, " + extra);
                        break;
                }
            }
        });
        mMediaRecorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
            @Override
            public void onError(MediaRecorder mr, int what, int extra) {
                switch (what) {
                    case MediaRecorder.MEDIA_RECORDER_ERROR_UNKNOWN:
                        Log.e(TAG, "MEDIA_RECORDER_ERROR_UNKNOWN, " + extra);
                        break;
                    case MediaRecorder.MEDIA_ERROR_SERVER_DIED:
                        Log.e(TAG, "MEDIA_ERROR_SERVER_DIED, " + extra);
                        break;
                }
            }
        });
        // Step 6: Prepare configured MediaRecorder
        try {
            mMediaRecorder.prepare();
            Log.d(TAG, "Prepare OK");
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e + "," + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e + "," + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (Exception e) {
            Log.d(TAG, "Exception preparing:" + e + "," + e.getMessage());
            return false;
        }
        return true;
    }

    private void releaseMediaRecorder(){
        if (mMediaRecorder != null) {
            mMediaRecorder.setOnErrorListener(null);
            mMediaRecorder.setOnInfoListener(null);

            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            mCamera.lock();           // lock camera for later use
        }
    }

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    private File getOutputMediaFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String name = "record_" + timeStamp + ".mp4";
        return new File(FileUtils.getExternalAppDir(getApplicationContext()), name);
    }

    private boolean prepareLocalSocket() {
        try {

            ParcelFileDescriptor[] parcelFileDescriptors =ParcelFileDescriptor.createPipe();
            parcelRead = new ParcelFileDescriptor(parcelFileDescriptors[0]);
            parcelWrite  = new ParcelFileDescriptor(parcelFileDescriptors[1]);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Can't create local socket");
            return false;
        }

        return true;
    }
    private void runConsumerThread() {
        (new Thread() {
            public void run() {
                int frame_size = 20000;
                byte[] buffer = new byte[1024 * 64];
                int num, number = 0;
                InputStream fis = null;
                try {
                    fis = new ParcelFileDescriptor.AutoCloseInputStream(parcelRead);
                } catch (Exception e) {
                    Log.e(TAG, "Cant create input stream", e);
                    return;
                }
                number = 0;
                while (true) {
                    Log.d(TAG, "Thread running");
                    try {
                        num = fis.read(buffer, number, frame_size);
                        number += num;
                        if (num < frame_size) {
                            Log.e(TAG, "recorder break");
                            break;
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "exception break", e);
                        break;
                    }
                }
                number = 0;
                VideoConsumer consumer = null;
                try {
                    consumer = new FileVideoConsumer(getOutputMediaFile().toString());
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return;
                }
                consumer.begin();
                DataInputStream dis = new DataInputStream(fis);
                try {
                    dis.read(buffer, 0, 32);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                // FIXME: 写死的一个头, 十有八九不正确!
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
    private void releaseLocalSocket() {
        try {
            if (parcelRead != null) {
                parcelRead.close();
                parcelRead = null;
            }
            if (parcelWrite != null) {
                parcelWrite.close();
                parcelWrite = null;
            }
        }catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Can't close local socket");
        }
    }
}