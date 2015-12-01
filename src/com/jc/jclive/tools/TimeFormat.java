package com.jc.jclive.tools;

public class TimeFormat {
	public static final long oneMinuteSeconds = 60;
	public static final long oneHourSeconds = oneMinuteSeconds * 60;
	public static final long oneDaySeconds = oneHourSeconds * 24;
	public static final long oneMonthSeconds = oneDaySeconds * 30;
	public static final long oneYearSeconds = oneMonthSeconds * 12;

	public static String formatRecent(long time) {
		long secondsTime = time / 1000;
		long now = System.currentTimeMillis() / 1000;
		if (now > secondsTime) {
			long interval = now - secondsTime;
			if (interval < oneMinuteSeconds) {
				return "1分钟前";
			} else if (interval < oneHourSeconds) {
				return String.format("%d分钟前", interval / oneMinuteSeconds);
			} else if (interval < oneDaySeconds) {
				return String.format("%d小时前", interval / oneHourSeconds);
			} else if (interval < oneMonthSeconds) {
				return String.format("%d天前", interval / oneDaySeconds);
			} else if (interval < oneYearSeconds) {
				return String.format("%d月前", interval / oneMonthSeconds);
			} else {
				return String.format("%d年前", interval / oneYearSeconds);
			}
		}
		return "";
	}
}
