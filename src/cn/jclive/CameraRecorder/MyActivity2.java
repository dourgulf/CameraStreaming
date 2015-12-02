package cn.jclive.CameraRecorder;

import java.net.InetAddress;
import java.net.UnknownHostException;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;  
import android.view.View;  
import android.view.View.OnClickListener;
import android.widget.Button;
import tv.inhand.streaming.Session;
import tv.inhand.streaming.SessionBuilder;

public class MyActivity2 extends Activity{
    private static final String TAG = "JCameara";

    private Button start;
    private Button stop;
    private Session videoSession;
    private SurfaceView surfaceview;
    private SurfaceHolder surfaceHolder;
  
    public void onCreate(Bundle savedInstanceState) {  
        super.onCreate(savedInstanceState);  
//        requestWindowFeature(Window.FEATURE_NO_TITLE);// 去掉标题栏
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                WindowManager.LayoutParams.FLAG_FULLSCREEN);// 设置全屏
//        // 设置横屏显示
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//        // 选择支持半透明模式,在有surfaceview的activity中使用。
//        getWindow().setFormat(PixelFormat.TRANSLUCENT);
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
        try {
            InetAddress addr = InetAddress.getByName("192.168.50.19");
            SessionBuilder.getInstance()
                    .setSurfaceHolder(holder)
                    .setDestination(addr);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
  
    class TestVideoListener implements OnClickListener {  
        @Override
        public void onClick(View v) {  
            if (v == start) {
                try {
                    videoSession = SessionBuilder.getInstance().build();
                    videoSession.start();
                } catch (Exception e) {
                    Log.e(TAG, "video session", e);
                }

            }  
            if (v == stop) {
                if (videoSession != null) {
                    videoSession.stop();
                }
            }
        }
    }
}  