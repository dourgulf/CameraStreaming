package com.jc.jclive.app.camera;

import java.io.IOException;

import android.annotation.TargetApi;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.Build;

@SuppressWarnings("deprecation")
public class CameraHolder {
	private static final String TAG = CameraHolder.class.getSimpleName();
	private Context mContext;
	private static CameraHolder mHolder;
	private CameraDelegate mCamera;
	private Camera.Parameters mParameters;
	private final int mNumberOfCameras;
	private int mCameraId = -1;
	private int mBackCameraId = -1;
	private int mFrontCameraId = -1;
	private final CameraInfo[] mInfo;

	private CameraHolder(Context context) {
		mContext = context;
		mNumberOfCameras = android.hardware.Camera.getNumberOfCameras();
		mInfo = new CameraInfo[mNumberOfCameras];
		for (int i = 0; i < mNumberOfCameras; i++) {
			mInfo[i] = new CameraInfo();
			android.hardware.Camera.getCameraInfo(i, mInfo[i]);
			if (mInfo[i].facing == CameraInfo.CAMERA_FACING_BACK) {
				mBackCameraId = i;
			} else if (mInfo[i].facing == CameraInfo.CAMERA_FACING_FRONT) {
				mFrontCameraId = i;
			}
		}
	}

	public static synchronized CameraHolder getInstance(Context context) {
		if (null == mHolder) {
			mHolder = new CameraHolder(context);
		}
		return mHolder;
	}

	public synchronized CameraDelegate openCamera(int cameraId)
			throws CameraHardwareException, CameraDisabledException {
		throwIfCameraDisabled(mContext);
		if (null != mCamera && mCameraId != cameraId) {
			mCamera.release();
			mCamera = null;
			mCameraId = -1;
		}
		if (null == mCamera) {
			try {
				Camera camera = android.hardware.Camera.open(cameraId);
				mCamera = new CameraDelegate(camera);
				mCameraId = cameraId;
				mParameters = mCamera.getParameters();
			} catch (RuntimeException e) {
				throw new CameraHardwareException(e);
			}
		} else {
			try {
				mCamera.reconnect();
				mCamera.setParameters(mParameters); // reset params
			} catch (IOException e) {
				throw new CameraHardwareException(e);
			}
		}
		return mCamera;
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private void throwIfCameraDisabled(Context context) throws CameraDisabledException {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			return;
		}
		DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
		if (dpm.getCameraDisabled(null)) {
			throw new CameraDisabledException();
		}
	}

	public synchronized void closeCamera() {
		if (null == mCamera) {
			return;
		}
		mCamera.stopPreview();
		mCamera.release();
		mCamera = null;
		mCameraId = -1;
		mParameters = null;
	}

	public int getNumberOfCameras() {
		return mNumberOfCameras;
	}

	public CameraInfo[] getCameraInfo() {
		return mInfo;
	}

	public int getBackCameraId() {
		return mBackCameraId;
	}

	public int getFrontCameraId() {
		return mFrontCameraId;
	}

	public boolean hasCamera(int cameraid) {
		return cameraid != -1;
	}
}
