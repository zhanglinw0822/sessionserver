package com.happyinsource.tradetest.util;

import java.io.IOException;

public class BASE64 {
	@SuppressWarnings("restriction")
	public static byte[] decode(String s) throws IOException {
		if (s == null)
			return null;
		return (new sun.misc.BASE64Decoder()).decodeBuffer(s);
	}
	
	@SuppressWarnings("restriction")
	public static String encode(byte [] value) throws IOException {
		if (value == null)
			return null;
		return (new sun.misc.BASE64Encoder()).encode(value);
	}
}
