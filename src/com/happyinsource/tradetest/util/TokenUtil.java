package com.happyinsource.tradetest.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class TokenUtil {
	public static String getCLT_Token(long curtime) {
		try {
			String userid = "";
			String password = "";
			ByteBuffer buffer = ByteBuffer.allocate(100);
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			buffer.putShort((short) userid.length());
			buffer.put(userid.getBytes());
			buffer.putLong(curtime);
			buffer.flip();
			buffer = ByteBuffer.wrap(buffer.array(), 0, buffer.limit());
			byte[] value = new byte[buffer.limit()];
			buffer.get(value);

			byte[] strLoginKey = SecurityUtil.getmd5((userid + password).getBytes());
			return BASE64.encode(SecurityUtil.AESEncrypt(value,strLoginKey));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}
	
	public static void main(String [] args) throws IOException
	{
		String userid = "998445563625632";
		String password = "111111";
		byte[] strLoginKey = SecurityUtil.getmd5((userid + password).getBytes());
		for(int i = 0;i < strLoginKey.length;i++)
		{
			System.out.println(strLoginKey[i]);
		}
		
		
		ByteBuffer buffer = ByteBuffer.allocate(100);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.putShort((short) userid.length());
		buffer.put(userid.getBytes());
		buffer.putLong(1446174561000L);
		buffer.flip();
		buffer = ByteBuffer.wrap(buffer.array(), 0, buffer.limit());
		byte[] value = new byte[buffer.limit()];
		buffer.get(value);
		System.out.println("end");
		System.out.println("start");
	
		for(int i = 0;i < value.length;i++)
		{
			System.out.println(value[i]);
		}
		System.out.println("");
		System.out.println("result:");
		byte [] result = SecurityUtil.AESEncrypt(value,strLoginKey);
		for(int i = 0;i < result.length;i++)
		{
			System.out.println(result[i]);
		}
		String token = BASE64.encode(result);
		System.out.println(token);
	}
}
