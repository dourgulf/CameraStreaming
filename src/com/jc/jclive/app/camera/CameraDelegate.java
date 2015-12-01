package com.jc.jclive.app.camera;

import java.io.IOException;

import android.annotation.TargetApi;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.AutoFocusMoveCallback;
import android.hardware.Camera.ErrorCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.ShutterCallback;
import android.os.Build;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.SurfaceHolder;

public class CameraDelegate {
	private static final int RELEASE = 1;
	private static final int RECONNECT = 2;
	private static final int UNLOCK = 3;
	private static final int LOCK = 4;
	private static final int SET_PREVIEW_DISPLAY = 5;
	private static final int START_PREVIEW = 6;
	private static final int STOP_PREVIEW = 7;
	private static final int SET_PREVIEW_CALLBACK_WITH_BUFFER = 8;
	private static final int ADD_CALLBACK_BUFFER = 9;
	private static final int AUTO_FOCUS = 10;
	private static final int CANCEL_AUTO_FOCUS = 11;
	private static final int SET_AUTO_FOCUS_MOVE_CALLBACK = 12;
	private static final int SET_DISPLAY_ORIENTATION = 13;
	private static final int SET_PARAMETERS = 14;
	private static final int GET_PARAMETERS = 15;
	private static final int SET_PREVIEW_CALLBACK = 16;
	private static final int SET_ERROR_CALLBACK = 17;
	private Camera mCamera;
	private Camera.Parameters mParameters;
	private Handler mCameraHandler;
	private ConditionVariable mSig = new ConditionVariable();

	public CameraDelegate(Camera camera) {
		mCamera = camera;
		HandlerThread handlerThread = new HandlerThread(
				"camera delegate handler thread");
		handlerThread.start();
		mCameraHandler = new CameraHandler(handlerThread.getLooper());
	}

	private class CameraHandler extends Handler {

		public CameraHandler(Looper looper) {
			super(looper);
		}

		@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
		private void setAutoFocusMoveCallback(Object cb) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
				mCamera.setAutoFocusMoveCallback((AutoFocusMoveCallback) cb);
			}
		}

		@Override
		public void handleMessage(Message msg) {
			try {
				switch (msg.what) {
				case RELEASE:
					mCamera.release();
					mCamera = null;
					break;
				case RECONNECT:
					try {
						mCamera.reconnect();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
					break;
				case LOCK:
					mCamera.lock();
					break;
				case UNLOCK:
					mCamera.unlock();
					break;
				case SET_PREVIEW_DISPLAY:
					try {
						mCamera.setPreviewDisplay((SurfaceHolder) msg.obj);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
					break;
				case START_PREVIEW:
					mCamera.startPreview();
					break;
				case STOP_PREVIEW:
					mCamera.stopPreview();
					break;
				case SET_PREVIEW_CALLBACK_WITH_BUFFER:
					mCamera.setPreviewCallbackWithBuffer((PreviewCallback) msg.obj);
					break;
				case ADD_CALLBACK_BUFFER:
					mCamera.addCallbackBuffer((byte[]) msg.obj);
					break;
				case AUTO_FOCUS:
					mCamera.autoFocus((AutoFocusCallback) msg.obj);
					break;
				case CANCEL_AUTO_FOCUS:
					mCamera.cancelAutoFocus();
					break;
				case SET_AUTO_FOCUS_MOVE_CALLBACK:
					setAutoFocusMoveCallback(msg.obj);
					break;
				case SET_DISPLAY_ORIENTATION:
					mCamera.setDisplayOrientation(msg.arg1);
					break;
				case SET_PARAMETERS:
					mCamera.setParameters((Parameters) msg.obj);
					break;
				case GET_PARAMETERS:
					mParameters = mCamera.getParameters();
					break;
				case SET_PREVIEW_CALLBACK:
					mCamera.setPreviewCallback((PreviewCallback) msg.obj);
					break;
				}
			} catch (RuntimeException e) {
				if (null != mCamera) {
					try {
						mCamera.release();
					} catch (Exception ex) {
						ex.printStackTrace();
					}
					mCamera = null;
				}
			}
			mSig.open();
		}
	}

	public Camera getCamera() {
		return mCamera;
	}

	public void release() {
		mSig.close();
		mCameraHandler.sendEmptyMessage(RELEASE);
		mSig.block();
	}

	public Parameters getParameters() {
		mSig.close();
		mCameraHandler.sendEmptyMessage(GET_PARAMETERS);
		mSig.block();
		return mParameters;
	}

	public void reconnect() throws IOException {
		mSig.close();
		mCameraHandler.sendEmptyMessage(RECONNECT);
		mSig.block();
	}

	public void unlock() {
		mSig.close();
		mCameraHandler.sendEmptyMessage(UNLOCK);
		mSig.block();
	}

	public void lock() {
		mSig.close();
		mCameraHandler.sendEmptyMessage(LOCK);
		mSig.block();
	}

	public void setParameters(Parameters params) {
		mSig.close();
		mCameraHandler.obtainMessage(SET_PARAMETERS, params).sendToTarget();
		mSig.block();
	}

	public void stopPreview() {
		mSig.close();
		mCameraHandler.sendEmptyMessage(STOP_PREVIEW);
		mSig.block();
	}

	public void startPreview() {
		mCameraHandler.sendEmptyMessage(START_PREVIEW);
	}

	public void setPreviewDisplay(final SurfaceHolder surfaceHolder) {
		mCameraHandler.obtainMessage(SET_PREVIEW_DISPLAY, surfaceHolder)
				.sendToTarget();
	}

	public void setAutoFocusMoveCallback(AutoFocusMoveCallback cb) {
		mSig.close();
		mCameraHandler.obtainMessage(SET_AUTO_FOCUS_MOVE_CALLBACK, cb)
				.sendToTarget();
		mSig.block();
	}

	public void setDisplayOrientation(int degrees) {
		mSig.close();
		mCameraHandler.obtainMessage(SET_DISPLAY_ORIENTATION, degrees, 0)
				.sendToTarget();
		mSig.block();
	}

	public void autoFocus(AutoFocusCallback cb) {
		mSig.close();
		mCameraHandler.obtainMessage(AUTO_FOCUS, cb).sendToTarget();
		mSig.block();
	}

	public void cancelAutoFocus() {
		mSig.close();
		mCameraHandler.sendEmptyMessage(CANCEL_AUTO_FOCUS);
		mSig.block();
	}

	public void takePicture(final ShutterCallback shutter,
			final PictureCallback raw, final PictureCallback postview,
			final PictureCallback jpeg) {
		mSig.close();
		mCameraHandler.post(new Runnable() {
			@Override
			public void run() {
				try {
					mCamera.takePicture(shutter, raw, postview, jpeg);
				} catch (RuntimeException e) {
					e.printStackTrace();
				}
				mSig.open();
			}
		});
		mSig.block();
	}

	public void setErrorCallback(ErrorCallback cb) {
		mSig.close();
		mCameraHandler.obtainMessage(SET_ERROR_CALLBACK, cb).sendToTarget();
		mSig.block();
	}
}
