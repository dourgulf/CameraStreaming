package cn.jclive.CameraRecorder;

import android.app.*;
import android.content.pm.ActivityInfo;
import android.graphics.*;
import android.media.*;
import android.os.*;
import android.util.Log;
import android.view.*;
import android.widget.*;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import android.hardware.Camera;
import com.jc.jclive.app.camera.CameraControl;
import com.jc.jclive.tools.Logger;


public class MyActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "MainActivity";
    JCCameraView mCameraView;
    boolean mRecording;
    Button mRecordButton;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Logger.init(getApplicationContext(), Logger.LOG_BOTH);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mCameraView = (JCCameraView)findViewById(R.id.surfaceview);

        mRecordButton = (Button)findViewById(R.id.btnStartStop);
        mRecordButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (!mRecording) {
//            mCameraView.switchMode(CameraControl.VIDEO_MOED);
            mCameraView.startVideoRecording();
            mRecording = true;
            mRecordButton.setText("Stop");
        }
        else {
            mCameraView.stopVideoRecording();
            mRecording = false;
            mRecordButton.setText("Start");

        }
    }
}