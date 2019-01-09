package com.unisky.live.mlive;

import android.content.Context;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import java.security.MessageDigest;
import java.util.Locale;

/**
 * 
 * @author kenping.liu
 * @date 2014-8-20
 */
public class KPKit {
	public static String tweakString(String str) {
		return (null == str || str.length() == 0) ? "" : str.trim();
	}

	public static String getPhoneID(Context context) {
		TelephonyManager telephonemanage = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		return telephonemanage.getDeviceId();
	}
	public static String getPhoneNUM(Context context) {
		TelephonyManager telephonemanage = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		return telephonemanage.getLine1Number();
	}

	public static String getDevID(Context context) {
		context = context.getApplicationContext();
		StringBuilder sb = new StringBuilder();
		sb.append(context.getPackageName()).append("_");
		sb.append(Settings.Secure.getString(context.getContentResolver(),
				Settings.Secure.ANDROID_ID));
		return "android_" + md5(sb.toString());
	}

	public static String md5(String str) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(str.getBytes());
			return byte2hex(md.digest()).toLowerCase(Locale.ENGLISH);
		} catch (Exception ex) {
		}
		return "";
	}

	public static String byte2hex(byte[] b) {
		StringBuilder sb = new StringBuilder();
		String stmp = "";
		for (int n = 0; n < b.length; n++) {
			stmp = Integer.toHexString(b[n] & 0XFF);
			if (stmp.length() == 1) {
				sb.append("0");
			}
			sb.append(stmp);
			// if (n < b.length - 1){sb.append(":");}
		}
		return sb.toString();
	}

	public static void terminalThread(Thread thread) {
		if (null != thread) {
			try {
				thread.join(100);
			} catch (Exception ex) {
			}
			if (thread.isAlive()) {
				try {
					thread.interrupt();
				} catch (Exception ex) {
				}
			}
		}
	}
}
