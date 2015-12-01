package com.jc.jclive.tools;

import android.content.Context;
import android.telephony.TelephonyManager;

public class PhoneInfoUtils {

	public static String getDeviceId(Context context) {
		String id = null;
		try {
			TelephonyManager tm = (TelephonyManager) context
					.getSystemService(Context.TELEPHONY_SERVICE);
			id = tm.getDeviceId();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return id;
	}
	
}
