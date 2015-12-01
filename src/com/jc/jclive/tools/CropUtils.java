package com.jc.jclive.tools;

import java.io.File;

import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

public class CropUtils {
	public static final String TYPE_IMAGE = "image/*";

	public static Uri generateUri(File file) {
		if (!file.getParentFile().exists()) {
			file.getParentFile().mkdir();
		}
		return Uri.fromFile(file.getParentFile()).buildUpon()
				.appendPath(file.getName()).build();
	}

	public static Intent newCropIntent(Uri datauri, Uri saveuri, int cropX,
			int cropY) {
		return new Intent("com.android.camera.action.CROP")
				.setDataAndType(datauri, TYPE_IMAGE)
				.putExtra("crop", "true")
				.putExtra("scale", true)
				.putExtra("aspectX", 1)
				.putExtra("aspectY", 1)
				.putExtra("outputX", cropX)
				.putExtra("outputY", cropY)
				.putExtra("return-data", true)
				.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString())
				.putExtra("noFaceDetection", true)
				.putExtra("scaleUpIfNeeded", true)
				.putExtra(MediaStore.EXTRA_OUTPUT, saveuri);
	}

	public static Intent newCameraCaptureIntent() {
		return new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
	}

	public static Intent newCameraCaptureIntent(Uri saveuri) {
		return newCameraCaptureIntent().putExtra(MediaStore.EXTRA_OUTPUT,
				saveuri);
	}

	@TargetApi(Build.VERSION_CODES.KITKAT)
	public static Intent newPhotoAblumIntent() {
		Intent intent = new Intent(Intent.ACTION_PICK);
		intent.setType(TYPE_IMAGE);
		return intent;
	}

	public static Intent newPhotoAblumIntent(Uri saveuri) {
		return newPhotoAblumIntent().putExtra(MediaStore.EXTRA_OUTPUT, saveuri);
	}
}
