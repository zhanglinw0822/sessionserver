package tpme.PMES.timebargain.server.util;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import tpme.PMES.member.ActiveUser.LogonManager;

/**
 * 实现AES加解密的实用类
 * 
 * @author wangy
 * 
 */
public class EncryptUtil {
	private transient static final Log logger = LogFactory.getLog(EncryptUtil.class);
	// 加密算法
	private static final String encryptAlgorithm = "AES";
	// 加密类型
	private static final String encryptType = "AES/ECB/PKCS5Padding"; // 算法/模式/补码方式,还有CBC/CFC/NoPadding,

	// 字符串编码方式
	private static final String codeStyle = "utf-8"; // "ascii"
	
	// 用来生成加密密钥
	private static Random random = new Random();

	// base64编码表
	private static String base64KeyTable = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

	// 加密密钥随机取下列的字符组合形成
	private static String keyTable = base64KeyTable;

	/*
	 * @param String
	 * @return 将String转换为byte数组
	 */
	
	public static byte[] str2Bytes(String value) {
		byte[] result = null;

		try {
			result = value.getBytes(codeStyle); // value.getBytes()
		} catch (Exception e) {
			logger.error(LogUtil.getSysMessage("将String转换为byte数组", "异常" + e));
		}

		return result;
	}
	
	/**
	 * 直接对字节流进行AES加密
	 * @param content
	 * @param key
	 * @return
	 */
	public static byte[] encrypt(byte[] content, SecretKeySpec key) throws Exception {
		Cipher cipher = Cipher.getInstance(encryptType);
		cipher.init(Cipher.ENCRYPT_MODE, key);// 初始化
		byte[] result = cipher.doFinal(content);
		return result; // 加密
	}

	/**
	 * 直接对字节流进行AES加密
	 * @param content
	 * @param key
	 * @return
	 */
	public static String encrypt2Str(byte[] content, SecretKeySpec key) throws Exception {
		return bytes2Base64Str(encrypt(content, key)); // 将加密后的byte数组进行Base64编码
	}

	/**
	 * 直接对字节流进行AES加密
	 * @param content
	 * @param password
	 * @return
	 */
	public static byte[] encrypt(byte[] content, byte[] password) throws Exception {
		SecretKeySpec key = new SecretKeySpec(password, encryptAlgorithm);
		return encrypt(content, key);
	}

	/**
	 * 直接对字节流进行AES加密
	 * @param content
	 * @param password
	 * @return
	 */
	public static String encrypt2Str(byte[] content, byte[] password) throws Exception {
		return bytes2Base64Str(encrypt(content, password)); // 将加密后的byte数组进行Base64编码
	}

	/**
	 * 直接对字节流进行AES加密
	 * @param content
	 * @param password
	 * @return
	 */
	public static byte[] encrypt(byte[] content, String password) throws Exception {
		return encrypt(content, parseHexStr2Bytes(password)); // 将十六进制串的密码转换为byte数组
	}

	/**
	 * 直接对字节流进行AES加密
	 * @param content
	 * @param password
	 * @return
	 */
	public static String encrypt2Str(byte[] content, String password) throws Exception {
		return bytes2Base64Str(encrypt(content, password)); // 将加密后的byte数组进行Base64编码
	}

	/**
	 * 加密
	 * 
	 * @param content
	 *            需要加密的内容
	 * @param password
	 *            加密密码
	 * @return
	 */
	public static byte[] encrypt(String content, String password) throws Exception {
		return encrypt(str2Bytes(content), password); // 将content转换为byte数组后加密
	}

	/**
	 * 解密
	 * 
	 * @param content
	 *            待解密内容
	 * @param key
	 *            解密密钥
	 * @return
	 */
	public static byte[] decrypt(byte[] content, SecretKeySpec key) throws Exception {
		Cipher cipher = Cipher.getInstance(encryptType);
		cipher.init(Cipher.DECRYPT_MODE, key);// 初始化
		byte[] result = cipher.doFinal(content);
		return result; // 解密
	}

	/**
	 * 解密
	 * 
	 * @param content
	 *            待解密内容
	 * @param password
	 *            解密密钥
	 * @return
	 */
	public static byte[] decrypt(byte[] content, byte[] password) throws Exception {
		SecretKeySpec key = new SecretKeySpec(password, encryptAlgorithm);
		return decrypt(content, key);
	}

	/**
	 * 解密
	 * 
	 * @param content
	 *            待解密内容
	 * @param password
	 *            解密密钥
	 * @return
	 */
	public static byte[] decrypt(byte[] content, String password) throws Exception {		
		// DigestUtils.md5(password), str2Bytes(password)
		return decrypt(content, parseHexStr2Bytes(password)); // 将十六进制串的密码转换为byte数组
	}

	/**
	 * 解密
	 * 
	 * @param content
	 *            待解密内容
	 * @param password
	 *            解密密钥
	 * @return
	 */
	public static byte[] decrypt(String content, String password) throws Exception {
		return decrypt(str2Bytes(content), password); // 将content转换为byte数组后加密
	}

	/**
	 * 解密
	 * 
	 * @param content
	 *            待解密内容
	 * @param password
	 *            解密密钥
	 * @return
	 */
	public static byte[] decryptStr(String content, String password) throws Exception {
		return decrypt(base64Str2Bytes(content), password); // 将内容是base64编码的字符串转换为byte数组后解密
	}

	/**
	 * 将二进制转换成Base64字符串
	 * 
	 * @param buf
	 * @return
	 */
	public static String bytes2Base64Str(byte buf[]) {
		return new Base64().encodeToString(buf);
	}

	/**
	 * 将Base64字符串转换为二进制
	 * 
	 * @param base64Str
	 * @return
	 */
	public static byte[] base64Str2Bytes(String base64Str) {
		return new Base64().decode(base64Str);
	}
	
	/**
	 * 将str进行md5加密
	 * @param str
	 * @return
	 */
	public static String str2Md5Str(String str) {
		return DigestUtils.md5Hex(str);
	}

	/**
	 * 将str进行md5加密
	 * @param str
	 * @return
	 */
	public static String str2Md5Str(byte[] str) {
		return DigestUtils.md5Hex(str);
	}
	
	/**
	 * 将str进行md5加密
	 * @param str
	 * @return
	 */
	public static byte[] str2Md5(String str) {
		return DigestUtils.md5(str);
	}

	/**
	 * 将str进行md5加密
	 * @param str
	 * @return
	 */
	public static byte[] str2Md5(byte[] str) {
		return DigestUtils.md5(str);
	}

	/**
	 * 将16进制转换为二进制
	 * 
	 * @param hexStr
	 * @return
	 */
	public static byte[] parseHexStr2Bytes(String hexStr) {
		if (hexStr.length() < 1 || 0 != (hexStr.length() % 2)) {
			return null;
		}

		int length = hexStr.length() / 2;
		byte[] result = new byte[length];
		int high;
		int low;
		for (int i = 0; i < length; i++) {
			high = Integer.parseInt(hexStr.substring(i * 2, i * 2 + 1), 16);
			low = Integer.parseInt(hexStr.substring(i * 2 + 1, i * 2 + 2), 16);
			result[i] = (byte)(high * 16 + low);
		}

		return result;
	}

	/**
	 * 将short值转换为byte数组
	 * 
	 * @param value
	 * @return
	 */
	public static byte[] short2ByteArray(short value) {
		int offset;
		int length = 2;
		byte[] result = new byte[length];

		for (int i = 0; i < length; i++) {
			offset = (result.length - 1 - i) * 8;
			result[i] = (byte) ((value >>> offset) & 0xFF);
		}

		return result;
	}

	/**
	 * 将byte数组转换为short值
	 * 
	 * @param value
	 * @return
	 */
	public static short byteArray2Short(byte[] value) {
		if (2 != value.length) {
			return -1;
		}

		int offset;
		short result = 0;

		for (int i = 0; i < value.length; i++) {
			offset = (value.length - 1 - i) * 8;
			result += (value[i] << offset);
		}

		return result;
	}

	/**
	 * 将int值转换为byte数组
	 * 
	 * @param value
	 * @return
	 */
	public static byte[] int2ByteArray(int value) {
		int offset; 
		int length = 4;
		byte[] result = new byte[length];

		for (int i = 0; i < length; i++) {
			offset = (result.length - 1 - i) * 8;
			result[i] = (byte) ((value >>> offset) & 0xFF);
		}

		return result;
	}

	/**
	 * 将byte数组转换为int值
	 * 
	 * @param value
	 * @return
	 */
	public static int byteArray2Int(byte[] value) {
		if (4 != value.length) {
			return -1;
		}

		int offset;
		int result = 0;

		for (int i = 0; i < value.length; i++) {
			offset = (value.length - 1 - i) * 8;
			result += (value[i] << offset);
		}

		return result;
	}

	/**
	 * 将long值转换为byte数组
	 * 
	 * @param value
	 * @return
	 */
	public static byte[] long2ByteArray(long value) {
		int offset; 
		int length = 8;
		byte[] result = new byte[length];

		for (int i = 0; i < length; i++) {
			offset = (result.length - 1 - i) * 8;
			result[i] = (byte) ((value >>> offset) & 0xFF);
		}

		return result;
	}

	/**
	 * 将byte数组转换为long值
	 * 
	 * @param value
	 * @return
	 */
	public static long byteArray2Long(byte[] value) {
		if (8 != value.length) {
			return -1;
		}

		int offset;
		long result = 0;

		for (int i = 0; i < value.length; i++) {
			offset = (value.length - 1 - i) * 8;
			result += (value[i] << offset);
		}

		return result;
	}

	/**
	 * 合并两个数组
	 * 
	 * @param first
	 * @param second
	 * @return
	 */
	public static byte[] mergeArray(byte[] first, byte[] second) {
		byte[] result = new byte[first.length + second.length];
		System.arraycopy(first, 0, result, 0, first.length); // 拷贝第一个数组 
		System.arraycopy(second, 0, result, first.length, second.length); // 拷贝第二个数组

		return result;
	}

	/**
	 * 随机生成加密密钥
	 * 
	 * @return
	 */
	public static byte[] generateKey() {
		byte[] k1 = long2ByteArray(random.nextLong());
		byte[] k2 = long2ByteArray(random.nextLong());

		return mergeArray(k1, k2);
	}

	/**
	 * 随机生成加密密钥,此种算法生成的密钥加密强度太低,暂不采用(需要将字符表扩展)
	 * 
	 * @return
	 */
	public static String generateStrKey() {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < 16; i++) {
			sb.append(keyTable.charAt(random.nextInt()));
		}

		return sb.toString();
	}

	/**
	 * @param ByteBuffer
	 * @return byte数组
	 */
	public static byte[] getUTF(ByteBuffer buffer) {
		short len = buffer.getShort();
		byte[] result = new byte[len];
		buffer.get(result);

		return result;
	}

	/**
	 * @param ByteBuffer
	 * @return byte数组
	 * @throws IOException 
	 */
	public static DataOutputStream writeUTF(DataOutputStream os, byte[] value) throws IOException {
		os.writeShort(Short.reverseBytes((short)value.length)); // 先写入两个字节的长度,与本地字节序相同
		os.write(value); // 再写入数据

		return os;
	}

	/**
	 * @param ByteBuffer
	 * @return byte数组
	 * @throws IOException 
	 */
	public static DataOutputStream writeUTF(DataOutputStream os, String value) throws IOException {
		return writeUTF(os, str2Bytes(value)); // 将字符串转换为byte数组后写入到输出流
	}

	/**
	 * @param sessionKey
	 * @param sessionId
	 * @return 返回Base64编码的Ticket
	 */
	public static String generateTicket(byte[] sessionKey, String sessionId, byte[] encryptKey) {
		String ticket = null;

		try {
			ByteArrayOutputStream array = new ByteArrayOutputStream();
			DataOutputStream os = new DataOutputStream(array);

			EncryptUtil.writeUTF(os, sessionKey); // 写入sessionKey
			EncryptUtil.writeUTF(os, sessionId); // 写入sessionId

			os.flush();
			ticket = encrypt2Str(array.toByteArray(), encryptKey); // 加密后编码为Base64
		} catch (Exception e) {
			logger.error(LogUtil.getSysMessage("返回Base64编码的Ticket", "异常" + e));
		}

		return ticket;
	}


	/**
	 * @param sessionKey
	 * @param sessionId
	 * @return 生成Base64编码的ClientToken
	 * @throws UnsupportedEncodingException 
	 */
	public static String generateTicket(byte[] sessionKey, String sessionId, String pwd) {
		return generateTicket(sessionKey, sessionId, parseHexStr2Bytes(pwd)); // 将十六进制密码转换为byte数组后加密生成ticket
	}
}
