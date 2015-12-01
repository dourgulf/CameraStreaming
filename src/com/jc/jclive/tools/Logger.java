package com.jc.jclive.tools;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

//import com.jc.jclive.app.DEBUG;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
//import android.support.annotation.NonNull;
import android.util.Log;

public class Logger {
	private static final int LOG_DEBUG = Log.DEBUG;
	private static final int LOG_VERBOSE = Log.VERBOSE;
	private static final int LOG_INFO = Log.INFO;
	private static final int LOG_WARN = Log.WARN;
	private static final int LOG_ERROR = Log.ERROR;
	private static final int LOG_ASSERT = Log.ASSERT;
	private static final long MAX_FILE_SIZE = 10485760L;
	private static FileLogHandler fileLogHandler;
	private static Context mContext;
	public static final int LOG_FILE = 1;
	public static final int LOG_CONSOLE = 2;
	public static final int LOG_BOTH = LOG_FILE + LOG_CONSOLE;
	private static int defaultDevice = LOG_CONSOLE;
	public static final String DEFALUT_TAG = "JCLive";

	private static class FileLogHandler extends Handler {
		private boolean hasSDCard = true;
		private BufferedOutputStream logOutput;
		private File logFile;

		public FileLogHandler() {
			hasSDCard = FileUtils.hasExternalStorage();
			if (hasSDCard) {
				try {
					logFile = FileUtils.getLogFile(mContext);
					if (!logFile.exists()) {
						logFile.createNewFile();
					} else {
						long fileSize = logFile.length();
						if (fileSize > MAX_FILE_SIZE) {
							logFile.delete();
							logFile.createNewFile();
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if (msg.what == 0x001 && hasSDCard && null != msg.obj) {
				try {
					logOutput = new BufferedOutputStream(new FileOutputStream(
							logFile, true));
					String log = (String) msg.obj;
					byte[] logData = log.getBytes();
					logOutput.write(logData, 0, logData.length);
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					if (null != logOutput) {
						try {
							logOutput.flush();
							logOutput.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}

	}

	public static void init(Context context, int logType) {
		mContext = context;
		defaultDevice = logType;
		fileLogHandler = new FileLogHandler();
	}

	private static String getTag(int index) {
		String tag = "";
		StackTraceElement[] traces = Thread.currentThread().getStackTrace();
		if (index < 0 || index > traces.length) {
			return tag;
		}
		String clsName = traces[index].getClassName();
		String methodName = traces[index].getMethodName();
		String shortClsName = "";
		int lastIndex = clsName.lastIndexOf(".");
		if (lastIndex != -1) {
			shortClsName = clsName.substring(lastIndex + 1);
		}
		return DEFALUT_TAG + " " + shortClsName + "." + methodName;
	}

	private static void showLog(int device, int logType, String tag, String msg) {
//		if (!DEBUG.IS_DEBUG) {
//			return;
//		}
		switch (device) {
		case LOG_CONSOLE:
			showLogInConsole(logType, tag, msg);
			break;
		case LOG_FILE:
			writeLogToFile(tag + "\t" + msg);
			break;
		case LOG_BOTH:
			showLogInConsole(logType, tag, msg);
			writeLogToFile(tag + "\t" + msg);
			break;
		}
	}

	private static void writeLogToFile(String log) {
		Message msg = fileLogHandler.obtainMessage();
		if (null == msg) {
			msg = new Message();
		}
		msg.what = 0x001;
		msg.obj = TimeUtils.getCurrentTimeInString() + "\t" + log + "\n";
		fileLogHandler.sendMessage(msg);
	}

	private static void showLogInConsole(int logType, String tag, String msg) {
		switch (logType) {
		case LOG_DEBUG:
			Log.d(tag, msg);
			break;
		case LOG_ERROR:
			Log.e(tag, msg);
			break;
		case LOG_INFO:
			Log.e(msg, msg);
			break;
		case LOG_VERBOSE:
			Log.v(tag, msg);
			break;
		case LOG_WARN:
			Log.w(tag, msg);
			break;
		}
	}

	public static void d(String msg) {
		showLog(defaultDevice, LOG_DEBUG, getTag(4), msg);
	}

	public static void d(String tag, String msg) {
		showLog(defaultDevice, LOG_DEBUG, tag, msg);
	}

	public static void v(String msg) {
		showLog(defaultDevice, LOG_VERBOSE, getTag(4), msg);
	}

	public static void v(String tag, String msg) {
		showLog(defaultDevice, LOG_VERBOSE, tag, msg);
	}

	public static void e(String msg) {
		showLog(defaultDevice, LOG_ERROR, getTag(4), msg);
	}

	public static void e(String tag, String msg) {
		showLog(defaultDevice, LOG_ERROR, tag, msg);
	}

	public static void i(String msg) {
		showLog(defaultDevice, LOG_INFO, getTag(4), msg);
	}

	public static void i(String tag, String msg) {
		showLog(defaultDevice, LOG_INFO, tag, msg);
	}

	public static void w(String msg) {
		showLog(defaultDevice, LOG_WARN, getTag(4), msg);
	}

	public static void w(String tag, String msg) {
		showLog(defaultDevice, LOG_WARN, tag, msg);
	}

}
