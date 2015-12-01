package com.jc.jclive.tools;

import java.io.InputStream;

import javax.microedition.khronos.opengles.GL10;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.opengl.GLES10;

public class BitmapUtils {
	private static final int DEFAULT_MAX_BITMAP_DIMENSION = 1024;
	private static int mMaxBitmapDimension;

	public static class ScaleType {
		public static final int FIT_INSIDE = 1;
		public static final int CROP = 2;
	}

	static {
		int[] maxTextureSize = new int[1];
		GLES10.glGetIntegerv(GL10.GL_MAX_TEXTURE_SIZE, maxTextureSize, 0);
		mMaxBitmapDimension = Math.max(maxTextureSize[0],
				DEFAULT_MAX_BITMAP_DIMENSION);
	}

	public static Bitmap decodeResource(Resources res, int resId, int dstWidth,
			int dstHeight, int scaleType, boolean powerOf2Scale) {
		Options options = new Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeResource(res, resId, options);
		options.inJustDecodeBounds = false;
		options.inSampleSize = computeImageSampleSize(options.outWidth,
				options.outHeight, dstWidth, dstHeight, scaleType,
				powerOf2Scale);
		Bitmap unscaledBitmap = BitmapFactory.decodeResource(res, resId,
				options);
		return unscaledBitmap;
	}

	public static Bitmap decodeFile(String path, int dstWidth, int dstHeight,
			int scaleType, boolean powerOf2Scale) {
		Options options = new Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(path, options);
		options.inJustDecodeBounds = false;
		options.inSampleSize = computeImageSampleSize(options.outWidth,
				options.outHeight, dstWidth, dstHeight, scaleType,
				powerOf2Scale);
		Bitmap unscaledBitmap = BitmapFactory.decodeFile(path, options);
		return unscaledBitmap;
	}

	public static Bitmap decodeByteArray(byte[] data, int offset, int length,
			int dstWidth, int dstHeight, int scaleType, boolean powerOf2Scale) {
		Options options = new Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeByteArray(data, offset, length);
		options.inJustDecodeBounds = false;
		options.inSampleSize = computeImageSampleSize(options.outWidth,
				options.outHeight, dstWidth, dstHeight, scaleType,
				powerOf2Scale);
		Bitmap unscaledBitmap = BitmapFactory.decodeByteArray(data, offset,
				length, options);
		return unscaledBitmap;
	}

	public static Bitmap decodeStream(InputStream is, int dstWidth,
			int dstHeight, int scaleType, boolean powerOf2Scale) {
		Options options = new Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeStream(is, null, options);
		options.inJustDecodeBounds = false;
		options.inSampleSize = computeImageSampleSize(options.outWidth,
				options.outHeight, dstWidth, dstHeight, scaleType,
				powerOf2Scale);
		Bitmap unscaledBitmap = BitmapFactory.decodeStream(is, null, options);
		return unscaledBitmap;
	}

	private static int computeImageSampleSize(int srcWidth, int srcHeight,
			int targetWidth, int targetHeight, int scaleType,
			boolean powerOf2Scale) {
		int scale = 1;
		switch (scaleType) {
		case ScaleType.FIT_INSIDE:
			if (powerOf2Scale) {
				final int halfWidth = srcWidth / 2;
				final int halfHeight = srcHeight / 2;
				while ((halfWidth / scale) > targetWidth
						|| (halfHeight / scale) > targetHeight) {
					scale *= 2;
				}
			} else {
				scale = Math.max(srcWidth / targetWidth, srcHeight
						/ targetHeight);
			}
			break;
		case ScaleType.CROP:
			if (powerOf2Scale) {
				final int halfWidth = srcWidth / 2;
				final int halfHeight = srcHeight / 2;
				while ((halfWidth / scale) > targetWidth
						&& (halfHeight / scale) > targetHeight) {
					scale *= 2;
				}
			} else {
				scale = Math.min(srcWidth / targetWidth, srcHeight
						/ targetHeight);
			}
			break;
		}
		if (scale < 1) {
			scale = 1;
		}
		scale = considerMaxTextureSize(srcWidth, srcHeight, scale,
				powerOf2Scale);
		return scale;
	}

	private static int considerMaxTextureSize(int srcWidth, int srcHeight,
			int scale, boolean powerOf2) {
		final int maxWidth = mMaxBitmapDimension;
		final int maxHeight = mMaxBitmapDimension;
		while ((srcWidth / scale) > maxWidth || (srcHeight / scale) > maxHeight) {
			if (powerOf2) {
				scale *= 2;
			} else {
				scale++;
			}
		}
		return scale;
	}

}
