package com.jc.jclive.tools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;

public class IOUtils {

	private IOUtils() {
		throw new AssertionError();
	}

	public static void close(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void closeQuietly(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void saveBitmap(Bitmap bitmap, File tragetFile,
			CompressFormat format, int quality) throws IOException {
		if (!tragetFile.getParentFile().exists()) {
			tragetFile.getParentFile().mkdirs();
		}
		BufferedOutputStream bos = new BufferedOutputStream(
				new FileOutputStream(tragetFile));
		try {
			bitmap.compress(format, quality, bos);
		} finally {
			bos.flush();
			closeQuietly(bos);
		}
	}

	public static byte[] ins2bytes(InputStream is) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		BufferedInputStream bis = new BufferedInputStream(is);
		byte[] buffer = new byte[4 * 1024];
		int len = 0;
		try {
			while ((len = bis.read(buffer)) != -1) {
				bos.write(buffer, 0, len);
			}
		} finally {
			bos.flush();
			closeQuietly(bos);
		}
		return bos.toByteArray();
	}

	public static void copyStream(InputStream is, OutputStream os)
			throws IOException {
		BufferedInputStream bis = new BufferedInputStream(is);
		BufferedOutputStream bos = new BufferedOutputStream(os);
		byte[] buffer = new byte[4 * 1024];
		int len = 0;
		try {
			while ((len = bis.read(buffer)) != -1) {
				bos.write(buffer, 0, len);
			}
		} finally {
			bos.flush();
		}
	}
	
	public static void copyFile(File form, File to) throws IOException {
		try {
			copyStream(new FileInputStream(form), new FileOutputStream(to));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

}
