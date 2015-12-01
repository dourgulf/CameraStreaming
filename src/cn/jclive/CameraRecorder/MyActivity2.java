package cn.jclive.CameraRecorder;

import java.io.File;
import java.io.IOException;
  
import android.app.Activity;  
import android.content.pm.ActivityInfo;  
import android.graphics.PixelFormat;  
import android.media.MediaRecorder;  
import android.os.Bundle;  
import android.view.SurfaceHolder;  
import android.view.SurfaceView;  
import android.view.View;  
import android.view.View.OnClickListener;  
import android.view.Window;  
import android.view.WindowManager;  
import android.widget.Button;
import com.jc.jclive.tools.FileUtils;
import com.jc.jclive.tools.Logger;

public class MyActivity2 extends Activity implements SurfaceHolder.Callback {  
    private Button start;// 开始录制按钮  
    private Button stop;// 停止录制按钮  
    private MediaRecorder mediarecorder;// 录制视频的类  
    private SurfaceView surfaceview;// 显示视频的控件  
    // 用来显示视频的一个接口，我靠不用还不行，也就是说用mediarecorder录制视频还得给个界面看  
    // 想偷偷录视频的同学可以考虑别的办法。。嗯需要实现这个接口的Callback接口  
    private SurfaceHolder surfaceHolder;  
  
    public void onCreate(Bundle savedInstanceState) {  
        super.onCreate(savedInstanceState);  
        requestWindowFeature(Window.FEATURE_NO_TITLE);// 去掉标题栏  
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,  
                WindowManager.LayoutParams.FLAG_FULLSCREEN);// 设置全屏  
        // 设置横屏显示  
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);  
        // 选择支持半透明模式,在有surfaceview的activity中使用。  
        getWindow().setFormat(PixelFormat.TRANSLUCENT);  
        setContentView(R.layout.main2);
        init();  
    }  
  
    private void init() {  
        start = (Button) this.findViewById(R.id.start);  
        stop = (Button) this.findViewById(R.id.stop);  
        start.setOnClickListener(new TestVideoListener());  
        stop.setOnClickListener(new TestVideoListener());  
        surfaceview = (SurfaceView) this.findViewById(R.id.surfaceview);  
        SurfaceHolder holder = surfaceview.getHolder();// 取得holder  
        holder.addCallback(this); // holder加入回调接口  
        // setType必须设置，要不出错.  
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);  
    }  
  
    class TestVideoListener implements OnClickListener {  
  
        @Override  
        public void onClick(View v) {  
            if (v == start) {  
                mediarecorder = new MediaRecorder();// 创建mediarecorder对象  
                // 设置录制视频源为Camera(相机)  
                mediarecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);  
                // 设置录制完成后视频的封装格式THREE_GPP为3gp.MPEG_4为mp4  
                mediarecorder  
                        .setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);  
                // 设置录制的视频编码h263 h264  
                mediarecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);  
                // 设置视频录制的分辨率。必须放在设置编码和格式的后面，否则报错  
                mediarecorder.setVideoSize(320, 240);
                // 设置录制的视频帧率。必须放在设置编码和格式的后面，否则报错  
                mediarecorder.setVideoFrameRate(20);  
                mediarecorder.setPreviewDisplay(surfaceHolder.getSurface());  
                // 设置视频文件输出的路径
                String path = new File(FileUtils
                        .getExternalAppDir(getApplicationContext()), "1234.mp4").getAbsolutePath();
                Logger.d("record path:" + path);
                mediarecorder.setOutputFile(path);
                try {  
                    // 准备录制  
                    mediarecorder.prepare();  
                    // 开始录制  
                    mediarecorder.start();  
                } catch (IllegalStateException e) {  
                    // TODO Auto-generated catch block  
                    e.printStackTrace();  
                } catch (IOException e) {  
                    // TODO Auto-generated catch block  
                    e.printStackTrace();  
                }  
            }  
            if (v == stop) {  
                if (mediarecorder != null) {  
                    // 停止录制  
                    mediarecorder.stop();  
                    // 释放资源  
                    mediarecorder.release();  
                    mediarecorder = null;  
                }  
            }  
  
        }  
  
    }  
  
    @Override  
    public void surfaceChanged(SurfaceHolder holder, int format, int width,  
            int height) {  
        // 将holder，这个holder为开始在oncreat里面取得的holder，将它赋给surfaceHolder  
        surfaceHolder = holder;  
    }  
  
    @Override  
    public void surfaceCreated(SurfaceHolder holder) {  
        // 将holder，这个holder为开始在oncreat里面取得的holder，将它赋给surfaceHolder  
        surfaceHolder = holder;  
    }  
  
    @Override  
    public void surfaceDestroyed(SurfaceHolder holder) {  
        // surfaceDestroyed的时候同时对象设置为null  
        surfaceview = null;  
        surfaceHolder = null;  
        mediarecorder = null;  
    }  
}  