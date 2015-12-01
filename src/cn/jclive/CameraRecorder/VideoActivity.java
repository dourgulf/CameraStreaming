package cn.jclive.CameraRecorder;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import com.jc.jclive.tools.Logger;

public class VideoActivity extends Activity implements SurfaceHolder.Callback {

    private File myRecVideoFile;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private TextView tvTime;
    private TextView tvSize;
    private Button btnStart;
    private Button btnStop;
    private Button btnCancel;
    private MediaRecorder recorder;
    private Handler handler;
    private Camera camera;
    private boolean recording; // 记录是否正在录像,fasle为未录像, true 为正在录像
    private int minute = 0;
    private int second = 0;
    private String time="";
    private String size="";
    private String fileName;
    private String name="";

    /**
     * 录制过程中,时间变化,大小变化
     */
    private Runnable timeRun = new Runnable() {
        @Override
        public void run() {
            long fileLength=myRecVideoFile.length();
            if(fileLength<1024 && fileLength>0){
                size=String.format("%dB/10M", fileLength);
            }else if(fileLength>=1024 && fileLength<(1024*1024)){
                fileLength=fileLength/1024;
                size=String.format("%dK/10M", fileLength);
            }else if(fileLength>(1024*1024*1024)){
                fileLength=(fileLength/1024)/1024;
                size=String.format("%dM/10M", fileLength);
            }
            second++;
            if (second == 60) {
                minute++;
                second = 0;
            }
            time = String.format("%02d:%02d", minute, second);
            tvSize.setText(size);
            tvTime.setText(time);
            handler.postDelayed(timeRun, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Logger.init(getApplicationContext(), Logger.LOG_BOTH);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main3);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mSurfaceView = (SurfaceView) findViewById(R.id.videoView);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceHolder.setKeepScreenOn(true);

        handler = new Handler();
        tvTime = (TextView) findViewById(R.id.tv_video_time);
        tvSize=(TextView)findViewById(R.id.tv_video_size);
        btnStop = (Button) findViewById(R.id.btn_video_stop);
        btnStart = (Button) findViewById(R.id.btn_video_start);
        btnCancel = (Button) findViewById(R.id.btn_video_cancel);
        btnCancel.setOnClickListener(listener);
        btnStart.setOnClickListener(listener);
        btnStop.setOnClickListener(listener);
        // 设置sdcard的路径

        fileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        name="video_" +System.currentTimeMillis() + ".mp4";
        fileName += File.separator + File.separator+"android_recorder"+File.separator+name;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // 开启相机
        if (camera == null) {
            int CammeraIndex=FindFrontCamera();
            if(CammeraIndex==-1){
                Logger.d("您的手机不支持前置摄像头");
                CammeraIndex=FindBackCamera();
            }
            camera = Camera.open(CammeraIndex);
            try {
                camera.setPreviewDisplay(mSurfaceHolder);
                camera.setDisplayOrientation(90);
            } catch (IOException e) {
                e.printStackTrace();
                camera.release();
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // 开始预览
        camera.startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // 关闭预览并释放资源
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    private OnClickListener listener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_video_stop:
                    if(recorder!=null){
                        releaseMediaRecorder();
                        minute = 0;
                        second = 0;
                        handler.removeCallbacks(timeRun);
                        recording = false;
                    }
                    btnStart.setEnabled(true);
                    break;
                case R.id.btn_video_start:
                    if(recorder!=null){
                        releaseMediaRecorder();
                        minute = 0;
                        second = 0;
                        handler.removeCallbacks(timeRun);
                        recording = false;
                    }
                    startRecorder();
                    btnStart.setEnabled(false);
                    break;
                case R.id.btn_video_cancel:
                    releaseMediaRecorder();
                    handler.removeCallbacks(timeRun);
                    minute=0;
                    second=0;
                    recording = false;
                    VideoActivity.this.finish();
                    break;
            }
        }
    };

    //判断前置摄像头是否存在
    private int FindFrontCamera(){
        int cameraCount = 0;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras(); // get cameras number

        for ( int camIdx = 0; camIdx < cameraCount;camIdx++ ) {
            Camera.getCameraInfo( camIdx, cameraInfo ); // get camerainfo
            if ( cameraInfo.facing ==Camera.CameraInfo.CAMERA_FACING_FRONT ) {
                // 代表摄像头的方位，目前有定义值两个分别为CAMERA_FACING_FRONT前置和CAMERA_FACING_BACK后置
                return camIdx;
            }
        }
        return -1;
    }

    //判断后置摄像头是否存在
    private int FindBackCamera(){
        int cameraCount = 0;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras(); // get cameras number

        for ( int camIdx = 0; camIdx < cameraCount;camIdx++ ) {
            Camera.getCameraInfo( camIdx, cameraInfo ); // get camerainfo
            if ( cameraInfo.facing ==Camera.CameraInfo.CAMERA_FACING_BACK ) {
                // 代表摄像头的方位，目前有定义值两个分别为CAMERA_FACING_FRONT前置和CAMERA_FACING_BACK后置
                return camIdx;
            }
        }
        return -1;
    }

    //释放recorder资源
    private void releaseMediaRecorder(){
        if (recorder != null) {
            recorder.stop();
            recorder.release();
            recorder = null;
        }
    }

    //开始录像
    public void startRecorder() {
        if (!recording) {
            try {
                // 关闭预览并释放资源
                if(camera!=null){
                    camera.stopPreview();
                    camera.release();
                    camera = null;
                }
                recorder = new MediaRecorder();
                // 声明视频文件对象
                myRecVideoFile = new File(fileName);
                if(!myRecVideoFile.exists()){
                    myRecVideoFile.getParentFile().mkdirs();
                    myRecVideoFile.createNewFile();
                }

                recorder.reset();
                // 判断是否有前置摄像头，若有则打开，否则打开后置摄像头
                int CammeraIndex=FindFrontCamera();
                if(CammeraIndex==-1){
                    Logger.d("您的手机不支持前置摄像头");
                    CammeraIndex=FindBackCamera();
                }
                camera = Camera.open(CammeraIndex);
                // 设置摄像头预览顺时针旋转90度，才能使预览图像显示为正确的，而不是逆时针旋转90度的。
                camera.setDisplayOrientation(90);
                camera.unlock();
                recorder.setCamera(camera); //设置摄像头为相机

                recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);//视频源
                recorder.setAudioSource(MediaRecorder.AudioSource.MIC); // 录音源为麦克风
                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
//                startRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);

                recorder.setOutputFile(myRecVideoFile.getAbsolutePath());
                recorder.setPreviewDisplay(mSurfaceHolder.getSurface()); // 预览
                recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                recorder.setAudioChannels(1);
                recorder.setAudioEncodingBitRate(64000);
                recorder.setAudioSamplingRate(44100);

                recorder.prepare();     // 准备录像
                recorder.start();       // 开始录像
                handler.post(timeRun);
                recording = true; // 改变录制状态为正在录制
            } catch (IOException e1) {
                e1.printStackTrace();
                Logger.e(e1.getMessage());
                releaseMediaRecorder();
                handler.removeCallbacks(timeRun);
                minute = 0;
                second = 0;
                recording = false;
                btnStart.setEnabled(true);
            } catch (IllegalStateException e) {
                e.printStackTrace();
                releaseMediaRecorder();
                handler.removeCallbacks(timeRun);
                minute = 0;
                second = 0;
                recording = false;
                btnStart.setEnabled(true);
            }
        } else
            Logger.d("Recording...");
    }
}