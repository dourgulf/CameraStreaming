package com.jc.jclive.app.camera;

//import com.jc.jclive.view.JCCameraView.TakePictureCallback;

public interface CameraControl {
	public static final int PHOTO_MODE = 1;
	public static final int VIDEO_MOED = 2;

//	void takePicture(TakePictureCallback callback);

	void startCamera(int cameraid);

	void switchMode(int cameramode);

	void closeCamera();

	void enableFlash(boolean enable);

	boolean hasBackCamera();

	boolean hasFrontCamera();

	int getBackCameraId();

	int getFrontCameraId();

	void startVideoRecording();

	void stopVideoRecording();

}
