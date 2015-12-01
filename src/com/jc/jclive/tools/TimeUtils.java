package com.jc.jclive.tools;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeUtils {

	public static final SimpleDateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");
	public static final SimpleDateFormat DATE_FORMAT_DATE = new SimpleDateFormat(
			"yyyy-MM-dd");

	private TimeUtils() {
		throw new AssertionError();
	}

	public static String getTime(long timeInMillis, SimpleDateFormat dateFormat) {
		return dateFormat.format(new Date(timeInMillis));
	}

	public static String getTime(long timeInMillis) {
		return getTime(timeInMillis, DEFAULT_DATE_FORMAT);
	}

	public static long getCurrentTimeInLong() {
		return System.currentTimeMillis();
	}

	public static String getCurrentTimeInString() {
		return getTime(getCurrentTimeInLong());
	}

	public static String getCurrentTimeInString(SimpleDateFormat dateFormat) {
		return getTime(getCurrentTimeInLong(), dateFormat);
	}
}
