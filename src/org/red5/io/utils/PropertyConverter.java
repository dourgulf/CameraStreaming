package org.red5.io.utils;

/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright (c) 2006-2010 by respective authors (see below). All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU Lesser General Public License as published by the Free Software 
 * Foundation; either version 2.1 of the License, or (at your option) any later 
 * version. 
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this library; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 */

import java.util.Calendar;

import org.apache.commons.lang.StringUtils;

/**
 * Converter for properties originating from properties files. Predetermined
 * string formats are converted into other usable types such as timestamps.
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class PropertyConverter {

	/**
	 * Converts a string denoting an amount of time into milliseconds and adds
	 * it to the current date. Strings are expected to follow this form where #
	 * equals a digit: #M The following are permitted for denoting time: H =
	 * hours, M = minutes, S = seconds
	 * 
	 * @param time time
	 * @return time in milliseconds
	 */
	public static long convertStringToFutureTimeMillis(String time) {
		Calendar exp = Calendar.getInstance();
		if (time.endsWith("H")) {
			exp.add(Calendar.HOUR, Integer.valueOf(StringUtils
					.remove(time, 'H')));
		} else if (time.endsWith("M")) {
			exp.add(Calendar.MINUTE, Integer.valueOf(StringUtils.remove(time,
					'M')));
		} else if (time.endsWith("S")) {
			exp.add(Calendar.MILLISECOND, Integer.valueOf(StringUtils.remove(
					time, 'S')) * 1000);
		}
		return exp.getTimeInMillis();
	}

	/**
	 * Converts a string denoting an amount of time into seconds. Strings are
	 * expected to follow this form where # equals a digit: #M The following are
	 * permitted for denoting time: H = hours, M = minutes, S = seconds
	 * 
	 * @param time time
	 * @return time in seconds
	 */
	public static int convertStringToTimeSeconds(String time) {
		int result = 0;
		if (time.endsWith("H")) {
			int hoursToAdd = Integer.valueOf(StringUtils.remove(time, 'H'));
			result = (60 * 60) * hoursToAdd;
		} else if (time.endsWith("M")) {
			int minsToAdd = Integer.valueOf(StringUtils.remove(time, 'M'));
			result = 60 * minsToAdd;
		} else if (time.endsWith("S")) {
			int secsToAdd = Integer.valueOf(StringUtils.remove(time, 'S'));
			result = secsToAdd;
		}
		return result;
	}

	/**
	 * Converts a string denoting an amount of time into milliseconds. Strings
	 * are expected to follow this form where # equals a digit: #M The following
	 * are permitted for denoting time: H = hours, M = minutes, S = seconds
	 * 
	 * @param time time
	 * @return time in milliseconds
	 */
	public static long convertStringToTimeMillis(String time) {
		long result = 0;
		if (time.endsWith("H")) {
			long hoursToAdd = Integer.valueOf(StringUtils.remove(time, 'H'));
			result = ((1000 * 60) * 60) * hoursToAdd;
		} else if (time.endsWith("M")) {
			long minsToAdd = Integer.valueOf(StringUtils.remove(time, 'M'));
			result = (1000 * 60) * minsToAdd;
		} else if (time.endsWith("S")) {
			long secsToAdd = Integer.valueOf(StringUtils.remove(time, 'S'));
			result = 1000 * secsToAdd;
		}
		return result;
	}

	/**
	 * Converts a string denoting an amount of bytes into an integer value.
	 * Strings are expected to follow this form where # equals a digit: #M The
	 * following are permitted for denoting binary size: K = kilobytes, M =
	 * megabytes, G = gigabytes
	 * 
	 * @param memSize memory
	 * @return size as an integer
	 */
	public static int convertStringToMemorySizeInt(String memSize) {
		int result = 0;
		if (memSize.endsWith("K")) {
			result = Integer.valueOf(StringUtils.remove(memSize, 'K')) * 1000;
		} else if (memSize.endsWith("M")) {
			result = Integer.valueOf(StringUtils.remove(memSize, 'M')) * 1000 * 1000;
		} else if (memSize.endsWith("G")) {
			result = Integer.valueOf(StringUtils.remove(memSize, 'G')) * 1000 * 1000 * 1000;
		}
		return result;
	}

	/**
	 * Converts a string denoting an amount of bytes into an long value. Strings
	 * are expected to follow this form where # equals a digit: #M The following
	 * are permitted for denoting binary size: K = kilobytes, M = megabytes, G =
	 * gigabytes
	 * 
	 * @param memSize memory size
	 * @return size as an long
	 */
	public static long convertStringToMemorySizeLong(String memSize) {
		long result = 0;
		if (memSize.endsWith("K")) {
			result = Long.valueOf(StringUtils.remove(memSize, 'K')) * 1000;
		} else if (memSize.endsWith("M")) {
			result = Long.valueOf(StringUtils.remove(memSize, 'M')) * 1000 * 1000;
		} else if (memSize.endsWith("G")) {
			result = Long.valueOf(StringUtils.remove(memSize, 'G')) * 1000 * 1000 * 1000;
		}
		return result;
	}

	/**
	 * Quick time converter to keep our timestamps compatible with PHP's time()
	 * (seconds)
	 * @return time in seconds
	 */
	public static Integer getCurrentTimeSeconds() {
		return convertMillisToSeconds(System.currentTimeMillis());
	}

	/**
	 * Quick time converter to keep our timestamps compatible with PHP's time()
	 * (seconds)
	 * @param millis milliseconds
	 * @return seconds
	 */
	public static Integer convertMillisToSeconds(Long millis) {
		return Long.valueOf(millis / 1000).intValue();
	}

}
