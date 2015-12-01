package com.jc.jclive.tools;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import android.util.Base64;

public class StringCodec {

	public static String urlencode(String original) {
		try {
			return URLEncoder.encode(original, "utf-8").replace("+", "%20").replace("*", "%2A").replace("%7E", "~");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String urldecode(String encoded) {
		try {
			return URLDecoder.decode(encoded, "utf-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String hmacSha1Digest(String original, String key) {
		return hmacSha1Digest(original.getBytes(), key.getBytes());
	}

	public static String hmacSha1Digest(byte[] original, byte[] key) {
		try {
			Mac mac = Mac.getInstance("HmacSHA1");
			mac.init(new SecretKeySpec(key, "HmacSHA1"));
			byte[] rawHmac = mac.doFinal(original);
			return new String(Base64.encode(rawHmac, Base64.DEFAULT));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String md5sum(byte[] original) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(original, 0, original.length);
			StringBuffer md5sum = new StringBuffer(new BigInteger(1, md.digest()).toString(16));
			while (md5sum.length() < 32)
				md5sum.insert(0, "0");
			return md5sum.toString();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String md5sum(String original) {
		return md5sum(original.getBytes());
	}
}
