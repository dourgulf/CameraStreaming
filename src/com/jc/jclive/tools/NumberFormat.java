package com.jc.jclive.tools;

public class NumberFormat {

	public static String formatKW(long number) {
		if (number < 1000) {
			return String.format("%s", number);
		} else if (number < 10000) {
			return String.format("%.1fK", number / 1000.0f);
		} else if (number < 1000000) {
			return String.format("%.1fW", number / 10000.0f);
		} else {
			return String.format("%.0fW", number / 10000.0f);
		}
	}
}
