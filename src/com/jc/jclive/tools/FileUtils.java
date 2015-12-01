package com.jc.jclive.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import android.content.Context;
import android.os.Environment;
//import android.support.annotation.Nullable;

//import com.jc.jclive.app.JCLiveApplication;

/**
 * 
 * @author lim
 * @date 2015/09/16
 * 
 */
public class FileUtils {
	public static final String LOG_DIR = "log";
	public static final String LOG_FILE = "log.log";

	private FileUtils() {
		throw new AssertionError();
	}

	public static boolean hasExternalStorage() {
		return Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState());
	}

	public static File getLogFile(Context context) {
		return new File(getExternalLogDir(context), LOG_FILE);
	}

	public static File getExternalLogDir(Context context) {
		File file = new File(getExternalAppDir(context), LOG_DIR);
		if (!file.exists()) {
			file.mkdirs();
		}
		return file;
	}


	public static File getExternalAppDir(Context context) {
		File appDir = new File(Environment.getExternalStorageDirectory(),
				context.getApplicationInfo().packageName);
		if (!appDir.exists()) {
			appDir.mkdirs();
		}
		return appDir;
	}

	public static String readFile(String filePath, String charsetName) {
		File file = new File(filePath);
		StringBuilder fileContent = new StringBuilder("");
		if (file == null || !file.isFile()) {
			return null;
		}
		BufferedReader reader = null;
		try {
			InputStreamReader is = new InputStreamReader(new FileInputStream(
					file), charsetName);
			reader = new BufferedReader(is);
			String line = null;
			while ((line = reader.readLine()) != null) {
				if (!fileContent.toString().equals("")) {
					fileContent.append("\r\n");
				}
				fileContent.append(line);
			}
			return fileContent.toString();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			IOUtils.close(reader);
		}
		return null;
	}

	public static void writeFile(String filePath, String content, boolean append) {
		FileWriter fileWriter = null;
		try {
			makeDirs(filePath);
			fileWriter = new FileWriter(filePath, append);
			fileWriter.write(content);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			IOUtils.close(fileWriter);
		}
	}

	public static void writeFile(File file, InputStream stream, boolean append) {
		OutputStream o = null;
		try {
			makeDirs(file.getAbsolutePath());
			o = new FileOutputStream(file, append);
			byte data[] = new byte[1024];
			int length = -1;
			while ((length = stream.read(data)) != -1) {
				o.write(data, 0, length);
			}
			o.flush();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			IOUtils.close(o);
			IOUtils.close(stream);
		}
	}

	public static void wirteFile(String filepath, InputStream stream) {
		writeFile(new File(filepath), stream, false);
	}

	public static void copyFile(String sourceFilePath, String destFilePath) {
		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(sourceFilePath);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		wirteFile(destFilePath, inputStream);
	}

	public static void moveFile(File srcFile, File destFile) {
		boolean rename = srcFile.renameTo(destFile);
		if (!rename) {
			copyFile(srcFile.getAbsolutePath(), destFile.getAbsolutePath());
			deleteFile(srcFile.getAbsolutePath());
		}
	}

	public static void moveFile(String sourceFilePath, String destFilePath) {
		moveFile(new File(sourceFilePath), new File(destFilePath));
	}

	public static void makeDirs(String filePath) {
		File file = new File(filePath);
		if (file.isDirectory()) {
			if (!file.exists()) {
				file.mkdirs();
			}
		} else if (file.isFile()) {
			if (!file.exists()) {
				File parentFile = file.getParentFile();
				if (!parentFile.exists()) {
					parentFile.mkdirs();
				}
			}
		}
	}

	public static boolean deleteFile(String path) {
		File file = new File(path);
		if (!file.exists()) {
			return true;
		}
		if (file.isFile()) {
			return file.delete();
		}
		if (!file.isDirectory()) {
			return false;
		}
		for (File f : file.listFiles()) {
			if (f.isFile()) {
				f.delete();
			} else if (f.isDirectory()) {
				deleteFile(f.getAbsolutePath());
			}
		}
		return file.delete();
	}
}
