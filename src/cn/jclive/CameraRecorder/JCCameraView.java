package cn.jclive.CameraRecorder;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.media.CameraProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;

import com.jc.jclive.app.camera.CameraControl;
import com.jc.jclive.app.camera.CameraDelegate;
import com.jc.jclive.app.camera.CameraDisabledException;
import com.jc.jclive.app.camera.CameraHardwareException;
import com.jc.jclive.app.camera.CameraHolder;
import com.jc.jclive.app.camera.Utils;
import com.jc.jclive.tools.FileUtils;
import com.jc.jclive.tools.Logger;
import com.jc.jclive.tools.ScreenUtils;

public class JCCameraView extends FrameLayout implements CameraControl {
	private static final String TAG = JCCameraView.class.getSimpleName();
	private static final int FOCUS_INTERVAL = 4500;
	private static final int MAX_FRAMES = 30;
	private static final int MESSAGE_TIMER_FOCUS = 1;
	private static final int MESSAGE_CAMERA_OPEN_FAILED = 3;
	private static final int MESSAGE_CAMERA_OPEN_SUCCESS = 4;
	private static final int MESSAGE_CAMERA_START_PERVIEW_DONE = 5;
	private static final int MESSAGE_CAMERA_DISABLE = 6;
	private static final int MESSAGE_CAMERA_RESUME = 7;
	private static final int MESSAGE_SWITCH_CAMERA = 8;
	private static final int MESSAGE_SWITCH_MODE = 9;
	private static final int MESSAGE_CAMERA_STOP = 10;
	private static final int MESSAGE_CAMERA_CLOSE = 11;
	private static final int MESSAGE_START_RECORDER = 12;
	private static final int STATE_PREVIEW_STOPPED = 1;
	private static final int STATE_CAMERA_IDLE = 2;
	private static final int STATE_CAMERA_FOCUS = 3;
	private static final int STATE_SWITCHING_CAMERA = 4;
	private static final int STATE_TAKING_PICTURE = 5;
	private static final int STATE_CAMERA_RECODING = 6;
	private static final int STATE_SWITCHING_MOED = 7;
	public static final double PREVIEW_RATIO_11 = 1.0d;
	public static final double PREVIEW_RATIO_43 = 4.0d / 3;
	public static final double PREVIEW_RATIO_169 = 16.0d / 9;
	public static final int PREVIEW_QUALITY_HIGHT = 1;
	public static final int PREVIEW_QUALITY_MIDDLE = 2;
	public static final int PREVIEW_QUALITY_LOW = 3;
	public static final int FOCUS_TOUCH = 1;
	public static final int FOCUS_AUTO = 2;

	private Context mContext;
	private CameraDelegate mCamera;
	private int mCurrentCameraId = -1;
	private int mCurrentCameraMode = CameraControl.PHOTO_MODE;
	private CameraHolder mCameraHolder;
	private Camera.Parameters mParameters;
	private Handler mEventHandler;
	private CameraSurfacePreview mSurfaceperview;
	private SurfaceHolder mSurfaceHolder;
	private MediaRecorder mMediaRecorder;
	private int mCameraState = STATE_PREVIEW_STOPPED;
	private Matrix mMatirx;
	private boolean mFocusAreaSupported;
	private boolean mMeteringAreaSupported;
	private boolean mAutoFocusSupported;
	private boolean mContinuosFocusSupported;
	private boolean mContinuosFocusVideoSupported;
	private boolean mEnableTouch;
	private boolean mMirror;
	private boolean mOpenCameraFailed;
	private boolean mCameraDisable;
	private int mFocusMode;
	private int mDisplayOrientation;
	private int mCameraDisplayOrientation;
	private String mSceneMode = Camera.Parameters.SCENE_MODE_AUTO;
	private String mFlashMode = Camera.Parameters.FLASH_MODE_OFF;
	private String mWhiteBalance = Camera.Parameters.WHITE_BALANCE_AUTO;
	private int mAutoFocusInterval = FOCUS_INTERVAL;
	private int mPreviewWidth;
	private int mPreviewHeight;
	private int mSurfaceWidth;
	private int mSurfaceHeight;
	private int mDisplayWidth;
	private int mDisplayHeight;
	private int mVideoWidth;
	private int mVideoHeight;
	private double mPreviewRatio;
	private int mFocusWidth;
	private int mFocusHeight;
	private int mOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
	private int mJpegRotation;
	private Camera.Area mFocusArea;
	private Camera.Area mMeteringArea;
	private Camera.AutoFocusCallback mFocusCallback;
	private Camera.AutoFocusMoveCallback mFocusMoveCallback;
	private Camera.ShutterCallback mShutterCallback;
	private Camera.PictureCallback mPostViewPictureCallback;
	private Camera.PictureCallback mRawPictureCallback;
	private Camera.PictureCallback mJpegPictureCallback;
	private Camera.ErrorCallback mErrorCallback;
	private MediaRecorder.OnErrorListener mMediaErrorListener;
	private OnCameraErrorListener mOnCameraErrorListener;
	private MediaRecorder.OnInfoListener mOnInfoListener;
	private TakePictureCallback mTakePictureCallback;
	private ConditionVariable mStartPreviewPrerequisiteReady = new ConditionVariable();
	private StartCameraThread mStartCameraThread;
	private CameraOrientationEventListener mOrientationListener;

	public JCCameraView(Context context) {
		this(context, null);
	}

	public JCCameraView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		Looper looper;
		if ((looper = Looper.myLooper()) != null) {
			mEventHandler = new EventHandler(this, looper);
		} else if ((looper = Looper.getMainLooper()) != null) {
			mEventHandler = new EventHandler(this, looper);
		} else {
			mEventHandler = null;
		}
		initView();
	}

	public interface OnCameraErrorListener {
		void onOpenError();

		void onDisableError();

		void onRuntimeError();
	}

	public void setOnErrorListener(
			OnCameraErrorListener onCameraErrorListener) {
		mOnCameraErrorListener = onCameraErrorListener;
	}

	private void initView() {
		mCameraHolder = CameraHolder.getInstance(mContext);
		if (mCurrentCameraId == -1) {
			if (hasBackCamera()) {
				mCurrentCameraId = mCameraHolder.getBackCameraId();
			} else if (hasFrontCamera()) {
				mCurrentCameraId = mCameraHolder.getFrontCameraId();
			}
		}

		mMatirx = new Matrix();
		mDisplayWidth = ScreenUtils.getWidth(mContext);
		mDisplayHeight = ScreenUtils.getHeight(mContext);
		mFocusWidth = 75;
		mFocusHeight = 75;
		mFocusMode = FOCUS_TOUCH;

		mFocusCallback = new AutoFocusCallback();
		mFocusMoveCallback = new AutoFocusMoveCallback();
		mShutterCallback = new ShutterCallback();
		mPostViewPictureCallback = new PostViewPictureCallback();
		mRawPictureCallback = new RawPictureCallback();
		mJpegPictureCallback = new JpegPictureCallback();
		mOrientationListener = new CameraOrientationEventListener(mContext);
		mErrorCallback = new CameraErrorCallback();
		mStartCameraThread = new StartCameraThread();
		mStartCameraThread.start();

		mSurfaceperview = new CameraSurfacePreview(mContext);
		mSurfaceperview.setLayoutParams(new FrameLayout.LayoutParams(
				FrameLayout.LayoutParams.WRAP_CONTENT,
				FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
		addView(mSurfaceperview.getView());
		setFocusable(true);
		setFocusableInTouchMode(true);
		requestFocus();
	}

	private class AutoFocusCallback implements Camera.AutoFocusCallback {

		@Override
		public void onAutoFocus(boolean success, Camera camera) {
			// Logger.d(TAG, "call onAutoFocus.");
			if (mCameraState == STATE_PREVIEW_STOPPED
					|| mCameraState == STATE_SWITCHING_CAMERA
					|| mCameraState == STATE_TAKING_PICTURE) {
				mCamera.cancelAutoFocus();
				mFocusArea = null;
				mMeteringArea = null;
				return;
			}
			if (success) {
				Logger.d(TAG, "focus cussess!");
				clearAutoFocus();
			}
			mEventHandler.sendEmptyMessageDelayed(MESSAGE_TIMER_FOCUS,
					mAutoFocusInterval);
		}

	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private class AutoFocusMoveCallback implements Camera.AutoFocusMoveCallback {

		@Override
		public void onAutoFocusMoving(boolean start, Camera camera) {
			Logger.d(TAG, "call onAutoFocusMoving");
		}

	}

	private class CameraErrorCallback implements Camera.ErrorCallback {
		@Override
		public void onError(int error, Camera camera) {
			Logger.d(TAG, "camera errorCode : " + error);
			if (error == Camera.CAMERA_ERROR_SERVER_DIED) {
				throw new RuntimeException("oh,no! camera has died!");
			}
		}
	}

	private class MediaOnErrorListener implements MediaRecorder.OnErrorListener {

		@Override
		public void onError(MediaRecorder mr, int what, int extra) {
			Logger.d(TAG, "MeidaRecorder errorCode :" + what);
			if (what == MediaRecorder.MEDIA_RECORDER_ERROR_UNKNOWN) {
				stopVideoRecording();
			}
		}
	}

	private class MediaOnInfoListener implements MediaRecorder.OnInfoListener {

		@Override
		public void onInfo(MediaRecorder mr, int what, int extra) {
			// TODO Auto-generated method stub

		}

	}

	private class CameraOrientationEventListener extends
			OrientationEventListener {

		public CameraOrientationEventListener(Context context) {
			super(context);
		}

		@Override
		public void onOrientationChanged(int orientation) {
			if (orientation == ORIENTATION_UNKNOWN) {
				return;
			}
			mOrientation = Utils.roundOrientation(orientation, mOrientation);
		}
	}

	private class EventHandler extends Handler {
		private WeakReference<JCCameraView> mPreview;

		public EventHandler(JCCameraView preview, Looper looper) {
			super(looper);
			mPreview = new WeakReference<JCCameraView>(preview);
		}

		@Override
		public void handleMessage(Message msg) {
			JCCameraView perview = mPreview.get();
			super.handleMessage(msg);
			switch (msg.what) {
			case MESSAGE_TIMER_FOCUS:
				if (null != perview) {
					removeMessages(MESSAGE_TIMER_FOCUS);
					perview.autoFocus();
				}
				break;
			case MESSAGE_CAMERA_OPEN_FAILED:
				mStartCameraThread = null;
				mOpenCameraFailed = true;
				Logger.d(TAG, "open camera failed");
				doCameraFailed();
				break;
			case MESSAGE_CAMERA_DISABLE:
				mStartCameraThread = null;
				mCameraDisable = true;
				Logger.d(TAG, "camera disable.");
				doCameraDisable();
				break;
			case MESSAGE_CAMERA_OPEN_SUCCESS:
				initializeAfterCameraOpen();
				break;
			case MESSAGE_CAMERA_START_PERVIEW_DONE:
				Logger.d(TAG, "startCameraThread has been finished!");
				mStartCameraThread = null;
				mOpenCameraFailed = false;
				cameraStateTo(STATE_CAMERA_IDLE);
				autoFocus();
				break;
			case MESSAGE_CAMERA_RESUME:
				// setupPreview();
				if (null == mCamera) {
					return;
				}
				mCamera.startPreview();
				cameraStateTo(STATE_CAMERA_IDLE);
				sendEmptyMessageDelayed(MESSAGE_TIMER_FOCUS, FOCUS_INTERVAL);
				break;
			case MESSAGE_SWITCH_CAMERA:
				switchCamera();
				break;
			case MESSAGE_SWITCH_MODE:
//				 switchCamera();
//				initMediaRecorder();
				break;
				case MESSAGE_START_RECORDER:
					startMediaRecorder();
					break;
			case MESSAGE_CAMERA_STOP:
				stopPreview();
				break;
			case MESSAGE_CAMERA_CLOSE:
				releasePreview();
				mCurrentCameraId = -1;
				break;
			}
		}
	}

	private class StartCameraThread extends Thread {
		private volatile boolean mCancelled;

		@Override
		public void run() {
			if (mCancelled) {
				return;
			}
			Logger.d(TAG, "startCameraThread start.");
			try {
				mCamera = mCameraHolder.openCamera(mCurrentCameraId);
				mParameters = mCamera.getParameters();
				mMirror = isCurrentFrontCamera();
				initCameraSupprot();
				mEventHandler.sendEmptyMessage(MESSAGE_CAMERA_OPEN_SUCCESS);
				Logger.d(TAG, "startCameraThread block");
				if (mCancelled) {
					return;
				}
				mStartPreviewPrerequisiteReady.block();
				Logger.d(TAG, "startCameraThread continue");
				if (mCancelled) {
					return;
				}
				startPreivew();
				mEventHandler
						.sendEmptyMessage(MESSAGE_CAMERA_START_PERVIEW_DONE);
			} catch (CameraHardwareException e) {
				mEventHandler.sendEmptyMessage(MESSAGE_CAMERA_OPEN_FAILED);
			} catch (CameraDisabledException e) {
				mEventHandler.sendEmptyMessage(MESSAGE_CAMERA_DISABLE);
			}
		}

		public void cancel() {
			mCancelled = true;
			interrupt();
		}

		public boolean isCanceled() {
			return mCancelled;
		}
	}

	private void initializeAfterCameraOpen() {
		// TODO Auto-generated method stub

	}

	public void doCameraDisable() {
		// TODO Auto-generated method stub

	}

	public void doCameraFailed() {
		// TODO Auto-generated method stub

	}

	public boolean isCurrentFrontCamera() {
		return mCurrentCameraId == mCameraHolder.getFrontCameraId();
	}

	public boolean isCurrentBackCamera() {
		return mCurrentCameraId == mCameraHolder.getBackCameraId();
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private void initCameraSupprot() {
		mAutoFocusSupported = Utils.isSupported(
				Camera.Parameters.FOCUS_MODE_AUTO,
				mParameters.getSupportedFocusModes());
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			mContinuosFocusSupported = Utils.isSupported(
					Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE,
					mParameters.getSupportedFocusModes());
		}
		mContinuosFocusVideoSupported = Utils.isSupported(
				Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO,
				mParameters.getSupportedFocusModes());
		mFocusAreaSupported = Utils.isFocusAreaSupported(mParameters);
		mMeteringAreaSupported = Utils.isMeteringAreaSupported(mParameters);
		mEnableTouch = !mMirror && mAutoFocusSupported && mFocusAreaSupported
				&& mMeteringAreaSupported && mCurrentCameraMode != VIDEO_MOED;
	}

	private void updateCameraParams() {
		List<Integer> frameRates = mParameters.getSupportedPreviewFrameRates();
		if (null != frameRates) {
			Integer max = Collections.max(frameRates);
			max = max > MAX_FRAMES ? MAX_FRAMES : max;
			mParameters.setPreviewFrameRate(max);
			Logger.d(TAG, "updateCameraParams->maxFrame :" + max);
		}

		mParameters.setPictureFormat(ImageFormat.JPEG);
		// mParameters.setJpegQuality(99);
		// mParameters.setJpegQuality(jpegQuality);
		int jpegQuality = CameraProfile.getJpegEncodingQualityParameter(
				mCurrentCameraId, CameraProfile.QUALITY_HIGH);
		mParameters.setJpegQuality(jpegQuality);

		initCameraPerviewSize();
		initCameraPictureSize();
		initCameraFocusMode();
		resetCameraZoomIfSupproted();
		String sceneMode = mParameters.getSceneMode();
		if (!mSceneMode.equals(sceneMode)) {
			if (Utils.isSupported(mSceneMode,
					mParameters.getSupportedSceneModes())) {
				mParameters.setSceneMode(mSceneMode);
			}
		}
		if (Utils.isSupported(mFlashMode, mParameters.getSupportedFlashModes())) {
			mParameters.setFlashMode(mFlashMode);
		}

		if (Utils.isSupported(mWhiteBalance,
				mParameters.getSupportedWhiteBalance())) {
			mParameters.setWhiteBalance(mWhiteBalance);
		}
		if (mContinuosFocusSupported) {
			updateAutoFocusMoveCallback();
		}
		mCamera.setParameters(mParameters);
		mParameters = mCamera.getParameters();
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void updateAutoFocusMoveCallback() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
			return;
		}
		if (mParameters.getFocusMode().equals(
				Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
			mCamera.setAutoFocusMoveCallback(mFocusMoveCallback);
		} else {
			mCamera.setAutoFocusMoveCallback(null);
		}
	}

	private void initCameraFocusMode() {
		if (mMirror) {
			mFocusMode = FOCUS_AUTO;
		}
		if (mAutoFocusSupported) {
			mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
		}
		if (mFocusMode == FOCUS_AUTO && mContinuosFocusSupported) {
			mParameters
					.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
		}
		if (mCurrentCameraMode == VIDEO_MOED && mContinuosFocusVideoSupported) {
			mParameters
					.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
		}
		mCamera.setParameters(mParameters);
		mParameters = mCamera.getParameters();
	}

	private void initCameraPerviewSize() {
		List<Camera.Size> sizes = mParameters.getSupportedPreviewSizes();
		if (null != sizes) {
			for (Size size : sizes) {
				// Logger.d(TAG, "supprot preiview size width :" + size.width +
				// " height :" + size.height);
			}
			Camera.Size optimalSize = Utils.getOptimalSize(sizes,
					Math.max(mDisplayWidth, mDisplayHeight),
					Math.min(mDisplayWidth, mDisplayHeight), mPreviewRatio);
			Camera.Size original = mParameters.getPreviewSize();
			mPreviewWidth = optimalSize.width;
			mPreviewHeight = optimalSize.height;
			Logger.d(TAG, "preiview optimalSize->width: " + mPreviewWidth
					+ " height: " + mPreviewHeight);
			if (!original.equals(optimalSize)) {
				mParameters.setPreviewSize(mPreviewWidth, mPreviewHeight);
				mCamera.setParameters(mParameters);
				mParameters = mCamera.getParameters();
			}
		}
	}

	private void initCameraPictureSize() {
		List<Camera.Size> sizes = mParameters.getSupportedPictureSizes();
		if (null != sizes) {
			for (Size size : sizes) {
				// Logger.d(TAG, "pcitrue supprot size width :" + size.width + "
				// height :" + size.height);
			}
			Camera.Size optimalSize = Utils.getOptimalSize(sizes,
					Math.max(mDisplayHeight, mDisplayWidth),
					Math.min(mDisplayHeight, mDisplayWidth), mPreviewRatio);
			Logger.d(TAG, "picture optimalSize->width: " + optimalSize.width
					+ " height: " + optimalSize.height);
			Camera.Size original = mParameters.getPictureSize();
			if (!original.equals(optimalSize)) {
				mParameters.setPictureSize(optimalSize.width,
						optimalSize.height);
				mCamera.setParameters(mParameters);
				mParameters = mCamera.getParameters();
			}
		}
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private void setFocusAreasIfSupported() {
		if (mFocusAreaSupported && null != mFocusArea) {
			mParameters.setFocusAreas(Arrays.asList(mFocusArea));
		}
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private void setMeteringAreasIfSupported() {
		if (mMeteringAreaSupported && null != mMeteringArea) {
			mParameters.setMeteringAreas(Arrays.asList(mMeteringArea));
		}
	}

	private void resetCameraZoomIfSupproted() {
		if (mParameters.isZoomSupported()) {
			mParameters.setZoom(0);
		}
	}

	public class CameraSurfacePreview extends SurfaceView implements
			SurfaceHolder.Callback {
		private GestureDetector mGestureDetector;

		public CameraSurfacePreview(Context context) {
			super(context);
			mSurfaceHolder = getHolder();
			mSurfaceHolder.setKeepScreenOn(true);
			setZOrderMediaOverlay(true);
			mSurfaceHolder.addCallback(this);
			mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
			mGestureDetector = new GestureDetector(mContext,
					new SingleTapListener());
		}

		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			if (null != mEventHandler) {
				mEventHandler.removeMessages(MESSAGE_TIMER_FOCUS);
			}
			if (!mEnableTouch) {
				return false;
			}
			mGestureDetector.onTouchEvent(event);
			return true;
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			Logger.d(TAG, "camera surface created");
			mSurfaceHolder = holder;
			mStartPreviewPrerequisiteReady.open();
			mOrientationListener.enable();
			if (null == mCamera || null != mStartCameraThread) {
				return;
			}
			Logger.d(TAG, "camera state :" + mCameraState);
			if (mCameraState == STATE_PREVIEW_STOPPED) {
				setupPreview();
			}
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
			Logger.d(TAG, "camera surface changed");
			mSurfaceHeight = height;
			mSurfaceWidth = width;
			if (null == mCamera || null != mStartCameraThread) {
				return;
			}
			if (mCameraState == STATE_CAMERA_IDLE) {
				// setDisplayOrientation();
			}
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			Logger.d(TAG, "camera surface destoryed");
			mSurfaceHolder = null;
			mOrientationListener.disable();
			if (null == mCamera) {
				return;
			}
			if (null != mEventHandler) {
				mEventHandler.removeMessages(MESSAGE_TIMER_FOCUS);
			}
			if (null != mStartCameraThread) {
				mStartCameraThread.cancel();
			}
			clearAutoFocus();
			stopPreview();
		}

		public View getView() {
			return this;
		}

	}

	private void setupPreview() {
		startPreivew();
		cameraStateTo(STATE_CAMERA_IDLE);
		autoFocus();
	}

	private void startPreivew() {
		mCamera.setErrorCallback(mErrorCallback);
		if (mCameraState != STATE_PREVIEW_STOPPED) {
			stopPreview();
		}
		setDisplayOrientation();
		updateCameraParams();
		setMatrix();
		mCamera.setPreviewDisplay(mSurfaceHolder);
		mCamera.startPreview();
		Logger.d(TAG, "start camera preview.");
	}

	private void stopPreview() {
		if (null != mCamera) {
			mCamera.stopPreview();
		}
		cameraStateTo(STATE_PREVIEW_STOPPED);
		Logger.d(TAG, "stop camera preview.");
	}

	private void setDisplayOrientation() {
		int displayRotation = Utils.getDisplayRotation((Activity) mContext);
		mDisplayOrientation = Utils.getDisplayOrientation(displayRotation,
				mCurrentCameraId);
		mCameraDisplayOrientation = Utils.getDisplayOrientation(
				displayRotation, mCurrentCameraId);
		Logger.d(TAG, "displayRotation :" + displayRotation
				+ " displayOrientation :" + mDisplayOrientation
				+ " cameraDisplayOrientation :" + mCameraDisplayOrientation);
		mCamera.setDisplayOrientation(mCameraDisplayOrientation);
	}

	private void setMatrix() {
		if (mSurfaceWidth != 0 && mSurfaceWidth != 0) {
			Matrix matrix = new Matrix();
			Utils.prepareMatrix(matrix, mMirror, mDisplayOrientation,
					mSurfaceWidth, mSurfaceHeight);
			matrix.invert(mMatirx);
		}
	}

	private void cameraStateTo(int state) {
		mCameraState = state;
	}

	private class SingleTapListener extends
			GestureDetector.SimpleOnGestureListener {

		@Override
		public boolean onSingleTapUp(MotionEvent event) {
			touchFocus((int) event.getX(), (int) event.getY());
			// Logger.d(TAG, "event x :" + event.getX() + " y:" + event.getY());
			return true;
		}
	}

	private void autoFocus() {
		if (mCameraState == STATE_PREVIEW_STOPPED
				|| mCameraState == STATE_SWITCHING_CAMERA
				|| mCameraState == STATE_TAKING_PICTURE) {
			return;
		}
		if (null != mCamera) {
			if (mFocusMode == FOCUS_TOUCH
					&& mCamera.getParameters().getFocusMode()
							.equals(Camera.Parameters.FOCUS_MODE_AUTO)) {
				mEventHandler.removeMessages(MESSAGE_TIMER_FOCUS);
				mCamera.autoFocus(mFocusCallback);
				cameraStateTo(STATE_CAMERA_FOCUS);
			}
		}
	}

	public void touchFocus(int x, int y) {
		if (mCameraState == STATE_PREVIEW_STOPPED
				|| mCameraState == STATE_SWITCHING_CAMERA
				|| mCameraState == STATE_TAKING_PICTURE) {
			return;
		}
		clearAutoFocus();
		onTouchFocus(x, y);
		setFocusAreasIfSupported();
		setMeteringAreasIfSupported();
		mCamera.setParameters(mParameters);
		autoFocus();
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private void clearAutoFocus() {
		if (null == mCamera) {
			return;
		}
		Logger.d(TAG, "clear auto focus");
		cameraStateTo(STATE_CAMERA_IDLE);
		mCamera.cancelAutoFocus();
		mFocusArea = null;
		mMeteringArea = null;
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	public void onTouchFocus(int x, int y) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			return;
		}
		if (mFocusHeight == 0 || mFocusWidth == 0 || mSurfaceHeight == 0
				|| mSurfaceWidth == 0) {
			return;
		}
		if (mFocusAreaSupported) {
			Rect focusRect = new Rect();
			RectF rectF = Utils.calculateTapArea(mFocusWidth, mFocusHeight,
					1.0f, x, y, mSurfaceWidth, mSurfaceHeight);
			mMatirx.mapRect(rectF);
			Utils.rectFToRect(rectF, focusRect);
			mFocusArea = new Camera.Area(focusRect, 800);
		}
		if (mMeteringAreaSupported) {
			Rect meteringRect = new Rect();
			RectF rectF = Utils.calculateTapArea(mFocusWidth, mFocusHeight,
					1.5f, x, y, mSurfaceWidth, mSurfaceHeight);
			mMatirx.mapRect(rectF);
			Utils.rectFToRect(rectF, meteringRect);
			mMeteringArea = new Camera.Area(meteringRect, 800);
		}
	}

	private void releasePreview() {
		if (null == mCamera) {
			return;
		}
		try {
			if (null != mEventHandler) {
				mEventHandler.removeMessages(MESSAGE_TIMER_FOCUS);
			}
			clearAutoFocus();
			stopPreview();
			mCamera.setErrorCallback(null);
			mSurfaceperview.destroyDrawingCache();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			Logger.d(TAG, "close camera");
			mCameraHolder.closeCamera();
		}
	}

	@Override
	public void closeCamera() {
		mEventHandler.sendEmptyMessage(MESSAGE_CAMERA_CLOSE);
	}

	public void resumeCamera() {
		mEventHandler.sendEmptyMessage(MESSAGE_CAMERA_RESUME);
	}

	public void stopCamera() {
		mEventHandler.sendEmptyMessage(MESSAGE_CAMERA_STOP);
	}

	public int getCurrentCameraId() {
		return mCurrentCameraId;
	}

	@Override
	public void enableFlash(boolean enable) {
		List<String> supportedFlash = mParameters.getSupportedFlashModes();
		String flashMode = enable ? Camera.Parameters.FLASH_MODE_ON
				: Camera.Parameters.FLASH_MODE_OFF;
		String curFlashMode = mParameters.getFlashMode();
		if (null != curFlashMode && !curFlashMode.equals(flashMode)) {
			if (Utils.isSupported(flashMode, supportedFlash)) {
				Logger.d(TAG, "current flash mode :" + flashMode);
				mFlashMode = flashMode;
				mParameters.setFlashMode(mFlashMode);
				mCamera.setParameters(mParameters);
				mParameters = mCamera.getParameters();
			}
		}
	}

	private class ShutterCallback implements Camera.ShutterCallback {
		@Override
		public void onShutter() {
		}
	}

	private class PostViewPictureCallback implements Camera.PictureCallback {
		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
		}
	}

	private class RawPictureCallback implements Camera.PictureCallback {
		@Override
		public void onPictureTaken(byte[] rawData, Camera camera) {
		}
	}

	private class JpegPictureCallback implements Camera.PictureCallback {

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			if (null != mTakePictureCallback) {
				mTakePictureCallback.onPictureTaken(data);
			}
			cameraStateTo(STATE_CAMERA_IDLE);
			// mEventHandler.sendEmptyMessageDelayed(MESSAGE_CAMERA_RESUME,
			// 300);
		}

	}

	public interface TakePictureCallback {
		public void onPictureTaken(byte[] data);
	}

//	@Override
//	public void takePicture(TakePictureCallback callback) {
//		if (mCameraState == STATE_PREVIEW_STOPPED
//				|| mCameraState == STATE_SWITCHING_CAMERA
//				|| mCameraState == STATE_TAKING_PICTURE || null == callback) {
//			return;
//		}
//		int displayRotation = Utils.getDisplayRotation((Activity) mContext);
//		mJpegRotation = Utils
//				.getJpegRotation(mCurrentCameraId, displayRotation);
//		mParameters.setRotation(mJpegRotation);
//		mCamera.setParameters(mParameters);
//		mParameters = mCamera.getParameters();
//		mTakePictureCallback = callback;
//		clearAutoFocus();
//		cameraStateTo(STATE_TAKING_PICTURE);
//		mEventHandler.removeMessages(MESSAGE_TIMER_FOCUS);
//		if (null != mCamera) {
//			mCamera.takePicture(mShutterCallback, mRawPictureCallback,
//					mPostViewPictureCallback, mJpegPictureCallback);
//		}
//	}

	@Override
	public void startCamera(int cameraid) {
		releasePreview();
		mCurrentCameraId = cameraid;
		cameraStateTo(STATE_SWITCHING_CAMERA);
		mEventHandler.sendEmptyMessage(MESSAGE_SWITCH_CAMERA);
	}

	private void switchCamera() {
		try {
			mCamera = mCameraHolder.openCamera(mCurrentCameraId);
			mParameters = mCamera.getParameters();
			mMirror = isCurrentFrontCamera();
			initCameraSupprot();
			mEventHandler.sendEmptyMessage(MESSAGE_CAMERA_OPEN_SUCCESS);
			startPreivew();
			mEventHandler.sendEmptyMessage(MESSAGE_CAMERA_START_PERVIEW_DONE);
		} catch (CameraHardwareException e) {
			mEventHandler.sendEmptyMessage(MESSAGE_CAMERA_OPEN_FAILED);
		} catch (CameraDisabledException e) {
			mEventHandler.sendEmptyMessage(MESSAGE_CAMERA_DISABLE);
		}
	}

	@Override
	public boolean hasBackCamera() {
		return mCameraHolder.hasCamera(mCameraHolder.getBackCameraId());
	}

	@Override
	public boolean hasFrontCamera() {
		return mCameraHolder.hasCamera(mCameraHolder.getFrontCameraId());
	}

	@Override
	public int getBackCameraId() {
		return mCameraHolder.getBackCameraId();
	}

	@Override
	public int getFrontCameraId() {
		return mCameraHolder.getFrontCameraId();
	}

	private void startMediaRecorder() {
		releaseMediaRecorder();
		if (null == mCamera.getCamera()
				|| null == mSurfaceHolder
				|| null == mSurfaceHolder.getSurface())
		{
			return;
		}
		// try {
		// mCamera = mCameraHolder.openCamera(mCurrentCameraId);
		// mParameters = mCamera.getParameters();
		// } catch (CameraHardwareException e1) {
		// mOpenCameraFailed = true;
		// e1.printStackTrace();
		// } catch (CameraDisabledException e1) {
		// mCameraDisable = true;
		// e1.printStackTrace();
		// }
		mMirror = isCurrentFrontCamera();
		initCameraSupprot();
		stopPreview();
		startPreivew();
		mCamera.unlock();
		mMediaRecorder = new MediaRecorder();
		mMediaRecorder.setCamera(mCamera.getCamera());
		mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
		mMediaRecorder.setOrientationHint(Utils.getVideoRotation(
				mCurrentCameraId, mOrientation));
		setLiveRecordingProfile();
		File videoFile = new File(FileUtils.getExternalAppDir(mContext), "1234.mp4");
//		if(!videoFile.exists()){
//			videoFile.getParentFile().mkdirs();
//			try {
//				videoFile.createNewFile();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
		mMediaRecorder.setOutputFile(videoFile.getAbsolutePath());
		Logger.d("record path:" + videoFile.getAbsolutePath());
		try {
			mMediaRecorder.prepare();
			mMediaRecorder.start();
			cameraStateTo(STATE_CAMERA_RECODING);
		} catch (IOException e) {
			Logger.d("media recorder exception:" + e.getMessage());
			releaseMediaRecorder();
			throw new RuntimeException(e);
		}
		mMediaErrorListener = new MediaOnErrorListener();
		mOnInfoListener = new MediaOnInfoListener();
		mMediaRecorder.setOnErrorListener(mMediaErrorListener);
		mMediaRecorder.setOnInfoListener(mOnInfoListener);
	}

	private void stopMediaRecorder() {
		Logger.d(TAG, "stop video recording OK.");
		try {
			mMediaRecorder.stop();
		} catch (Exception e) {
			releaseMediaRecorder();
			mCamera.lock();
			e.printStackTrace();
		}

	}
	private void setLiveRecordingProfile() {
		final int videoWidth = 720;
		final int videoHeight = 480;
		mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
//		mMediaRecorder.setOutputFormat(8);
//		mMediaRecorder.setVideoFrameRate(30);
//		List<Camera.Size> videoSizes = mParameters.getSupportedVideoSizes();
//		Camera.Size optimalSize = Utils.getOptimalSize(videoSizes, videoWidth,
//				videoHeight, (double) videoWidth / videoHeight);
//		mMediaRecorder.setVideoSize(optimalSize.width, optimalSize.height);
//		Logger.d(TAG, "video size width :" + optimalSize.width + " height :" + optimalSize.height);

//		mMediaRecorder.setVideoEncodingBitRate(optimalSize.width * optimalSize.height * 2);
		mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
		mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
		mMediaRecorder.setAudioChannels(1);
		mMediaRecorder.setAudioEncodingBitRate(64000);
		mMediaRecorder.setAudioSamplingRate(44100);
	}

	private void releaseMediaRecorder() {
		if (null != mMediaRecorder) {
			Logger.d(TAG, "release media startRecorder.");
			mMediaRecorder.reset();
			mMediaRecorder.release();
			mMediaRecorder = null;
		}
	}

	@Override
	public void switchMode(int cameramode) {
		if (mCurrentCameraMode == cameramode) {
			return;
		}
		mCurrentCameraMode = cameramode;
		if (mCurrentCameraMode == CameraControl.VIDEO_MOED) {
			// closeCamera();
			cameraStateTo(STATE_SWITCHING_MOED);
			mEventHandler.sendEmptyMessage(MESSAGE_SWITCH_MODE);
		}
	}

	private void pauseAudioPlayback() {
		Intent intent = new Intent("com.android.music.musicservicecommand");
		intent.putExtra("command", "pause");
		mContext.sendBroadcast(intent);
	}

	@Override
	public void startVideoRecording() {
		startMediaRecorder();
//		mEventHandler.sendEmptyMessage(MESSAGE_START_RECORDER);
//		if (null == mMediaRecorder) {
//			return;
//		}
//		Logger.d(TAG, "start video recording OK.");
//		pauseAudioPlayback();
//		try {
//			mMediaRecorder.start();
//		} catch (Exception e) {
//			releaseMediaRecorder();
//			mCamera.lock();
//			e.printStackTrace();
//		}
	}

	@Override
	public void stopVideoRecording() {
		if (null == mMediaRecorder) {
			return;
		}
		Logger.d(TAG, "stop video recording OK.");
		try {
			mMediaRecorder.stop();
		} catch (Exception e) {
			releaseMediaRecorder();
			mCamera.lock();
			e.printStackTrace();
		}
	}

}
