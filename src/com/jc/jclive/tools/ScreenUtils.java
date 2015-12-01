package com.jc.jclive.tools;

import android.content.Context;
import android.util.DisplayMetrics;

/**
 * 
 * @author lim
 * 
 */
public class ScreenUtils {

	private ScreenUtils() {
		throw new AssertionError();
	}

	public static int getWidth(Context context) {
		DisplayMetrics displaymetrics = context.getApplicationContext()
				.getResources().getDisplayMetrics();
		return displaymetrics.widthPixels;
	}

	public static int getHeight(Context context) {
		DisplayMetrics displaymetrics = context.getApplicationContext()
				.getResources().getDisplayMetrics();
		return displaymetrics.heightPixels;
	}

	public static int getDpi(Context context) {
		DisplayMetrics displaymetrics = context.getApplicationContext()
				.getResources().getDisplayMetrics();
		return displaymetrics.densityDpi;
	}

	public static float dpToPx(Context context, float dp) {
		if (context == null) {
			return -1;
		}
		return dp * context.getResources().getDisplayMetrics().density;
	}

	public static float pxToDp(Context context, float px) {
		if (context == null) {
			return -1;
		}
		return px / context.getResources().getDisplayMetrics().density;
	}

	public static int dpToPxInt(Context context, float dp) {
		return (int) (dpToPx(context, dp) + 0.5f);
	}

	public static int pxToDpCeilInt(Context context, float px) {
		return (int) (pxToDp(context, px) + 0.5f);
	}
}
