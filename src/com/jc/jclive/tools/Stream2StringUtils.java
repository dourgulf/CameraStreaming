package com.jc.jclive.tools;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

public class Stream2StringUtils {

	public static byte[] getByte(InputStream is) {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		BufferedInputStream in = new BufferedInputStream(is);
		byte[] buffer = new byte[1024];
		int len = 0;
		try {
			while ((len = in.read(buffer)) != -1) {
				os.write(buffer, 0, len);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				os.close();
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return os.toByteArray();
	}

	public static String getString(InputStream is) {
		return new String(getByte(is));
	}

	public static String getStringUtf8(InputStream is)
			throws UnsupportedEncodingException {
		return new String(getByte(is), "UTF-8");
	}

}
