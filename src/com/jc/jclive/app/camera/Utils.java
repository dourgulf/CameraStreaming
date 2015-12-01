package com.jc.jclive.app.camera;

import java.util.List;
import java.util.logging.Logger;

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.media.MediaRecorder;
import android.os.Build;
import android.view.OrientationEventListener;
import android.view.Surface;

public class Utils {
	private static final String TAG = "Camera.Utils";
	public static final int ORIENTATION_HYSTERESIS = 5;

	public static boolean isSupported(String value, List<String> supported) {
		return supported == null ? false : supported.contains(value);
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	public static boolean isMeteringAreaSupported(Parameters params) {
		return params.getMaxNumMeteringAreas() > 0;
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	public static boolean isFocusAreaSupported(Parameters params) {
		return (params.getMaxNumFocusAreas() > 0 && isSupported(
				Parameters.FOCUS_MODE_AUTO, params.getSupportedFocusModes()));
	}

	public static void rectFToRect(RectF rectF, Rect rect) {
		rect.left = Math.round(rectF.left);
		rect.top = Math.round(rectF.top);
		rect.right = Math.round(rectF.right);
		rect.bottom = Math.round(rectF.bottom);

	}

	public static RectF calculateTapArea(int focusWidth, int focusHeight,
			float areaMultiple, int x, int y, int previewWidth,
			int previewHeight) {
		int areaWidth = (int) (focusWidth * areaMultiple);
		int areaHeight = (int) (focusHeight * areaMultiple);
		int left = clamp(x - areaWidth / 2, 0, previewWidth - areaWidth);
		int top = clamp(y - areaHeight / 2, 0, previewHeight - areaHeight);
		return new RectF(left, top, left + areaWidth, top + areaHeight);
	}

	public static int clamp(int x, int min, int max) {
		if (x > max)
			return max;
		if (x < min)
			return min;
		return x;
	}

	public static int getDisplayRotation(Activity activity) {
		int rotation = activity.getWindowManager().getDefaultDisplay()
				.getRotation();
		switch (rotation) {
		case Surface.ROTATION_0:
			return 0;
		case Surface.ROTATION_90:
			return 90;
		case Surface.ROTATION_180:
			return 180;
		case Surface.ROTATION_270:
			return 270;
		}
		return 0;
	}

	public static int getDisplayOrientation(int degrees, int cameraId) {
		Camera.CameraInfo info = new Camera.CameraInfo();
		Camera.getCameraInfo(cameraId, info);
		int result;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360;
		} else {
			result = (info.orientation - degrees + 360) % 360;
		}
		return result;
	}

	public static int getJpegRotation(int cameraId, int orientation) {
		int rotation = 0;
		Camera.CameraInfo info = new Camera.CameraInfo();
		Camera.getCameraInfo(cameraId, info);
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			rotation = (info.orientation - orientation + 360) % 360;
		} else {
			rotation = (info.orientation + orientation) % 360;
		}

		return rotation;
	}

	public static int getVideoRotation(int cameraId, int orientation) {
		int rotation = 0;
		if (orientation != OrientationEventListener.ORIENTATION_UNKNOWN) {
			Camera.CameraInfo info = new Camera.CameraInfo();
			Camera.getCameraInfo(cameraId, info);
			if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
				rotation = (info.orientation - orientation + 360) % 360;
			} else {
				rotation = (info.orientation + orientation) % 360;
			}
		}
		return rotation;
	}

	public static int roundOrientation(int orientation, int orientationHistory) {
		boolean changeOrientation = false;
		if (orientationHistory == OrientationEventListener.ORIENTATION_UNKNOWN) {
			changeOrientation = true;
		} else {
			int dist = Math.abs(orientation - orientationHistory);
			dist = Math.min(dist, 360 - dist);
			changeOrientation = (dist >= 45 + ORIENTATION_HYSTERESIS);
		}
		if (changeOrientation) {
			return ((orientation + 45) / 90 * 90) % 360;
		}
		return orientationHistory;
	}

	public static int getCameraOrientation(int cameraId) {
		Camera.CameraInfo info = new Camera.CameraInfo();
		Camera.getCameraInfo(cameraId, info);
		return info.orientation;
	}

	public static Size getOptimalSize(List<Size> sizes, int width, int height,
			double targetRatio) {
		final double ASPECT_TOLERANCE = 0.001;
		if (sizes == null)
			return null;
		Size optimalSize = null;
		double minDiff = Double.MAX_VALUE;
		// float targetRatio = (float) width / height;
		if (targetRatio == 0) {
			targetRatio = (float) width / height;
		}

		int targetHeight = height;
		for (Size size : sizes) {
			double ratio = (double) size.width / size.height;
			if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
				continue;
			if (Math.abs(size.height - targetHeight) < minDiff) {
				optimalSize = size;
				minDiff = Math.abs(size.height - targetHeight);
			}
		}
		if (optimalSize == null) {
//			Logger.w(TAG, "No preview size match the aspect ratio");
			minDiff = Double.MAX_VALUE;
			for (Size size : sizes) {
				if (Math.abs(size.height - targetHeight) < minDiff) {
					optimalSize = size;
					minDiff = Math.abs(size.height - targetHeight);
				}
			}
		}
		return optimalSize;
	}

	public static Size getOptimalVideoSnapshotPictureSize(List<Size> sizes,
			double targetRatio) {
		final double ASPECT_TOLERANCE = 0.001;
		if (sizes == null)
			return null;
		Size optimalSize = null;
		for (Size size : sizes) {
			double ratio = (double) size.width / size.height;
			if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
				continue;
			if (optimalSize == null || size.width > optimalSize.width) {
				optimalSize = size;
			}
		}
		if (optimalSize == null) {
//			Logger.w(TAG, "No picture size match the aspect ratio");
			for (Size size : sizes) {
				if (optimalSize == null || size.width > optimalSize.width) {
					optimalSize = size;
				}
			}
		}
		return optimalSize;
	}

	public static void prepareMatrix(Matrix matrix, boolean mirror,
			int displayOrientation, int previewWidth, int previewHeight) {
		matrix.setScale(mirror ? -1 : 1, 1);
		matrix.postRotate(displayOrientation);
		matrix.postScale(previewWidth / 2000f, previewHeight / 2000f);
		matrix.postTranslate(previewWidth / 2f, previewHeight / 2f);
	}

	public static String convertOutputFormatToMimeType(int outputFileFormat) {
		if (outputFileFormat == MediaRecorder.OutputFormat.MPEG_4) {
			return "video/mp4";
		}
		return "video/3gpp";
	}

	public static String convertOutputFormatToFileExt(int outputFileFormat) {
		if (outputFileFormat == MediaRecorder.OutputFormat.MPEG_4) {
			return ".mp4";
		}
		return ".3gp";
	}
}
