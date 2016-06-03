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
 * ʵ��AES�ӽ��ܵ�ʵ����
 * 
 * @author wangy
 * 
 */
public class EncryptUtil {
	private transient static final Log logger = LogFactory.getLog(EncryptUtil.class);
	// �����㷨
	private static final String encryptAlgorithm = "AES";
	// ��������
	private static final String encryptType = "AES/ECB/PKCS5Padding"; // �㷨/ģʽ/���뷽ʽ,����CBC/CFC/NoPadding,

	// �ַ������뷽ʽ
	private static final String codeStyle = "utf-8"; // "ascii"
	
	// �������ɼ�����Կ
	private static Random random = new Random();

	// base64�����
	private static String base64KeyTable = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

	// ������Կ���ȡ���е��ַ�����γ�
	private static String keyTable = base64KeyTable;

	/*
	 * @param String
	 * @return ��Stringת��Ϊbyte����
	 */
	
	public static byte[] str2Bytes(String value) {
		byte[] result = null;

		try {
			result = value.getBytes(codeStyle); // value.getBytes()
		} catch (Exception e) {
			logger.error(LogUtil.getSysMessage("��Stringת��Ϊbyte����", "�쳣" + e));
		}

		return result;
	}
	
	/**
	 * ֱ�Ӷ��ֽ�������AES����
	 * @param content
	 * @param key
	 * @return
	 */
	public static byte[] encrypt(byte[] content, SecretKeySpec key) throws Exception {
		Cipher cipher = Cipher.getInstance(encryptType);
		cipher.init(Cipher.ENCRYPT_MODE, key);// ��ʼ��
		byte[] result = cipher.doFinal(content);
		return result; // ����
	}

	/**
	 * ֱ�Ӷ��ֽ�������AES����
	 * @param content
	 * @param key
	 * @return
	 */
	public static String encrypt2Str(byte[] content, SecretKeySpec key) throws Exception {
		return bytes2Base64Str(encrypt(content, key)); // �����ܺ��byte�������Base64����
	}

	/**
	 * ֱ�Ӷ��ֽ�������AES����
	 * @param content
	 * @param password
	 * @return
	 */
	public static byte[] encrypt(byte[] content, byte[] password) throws Exception {
		SecretKeySpec key = new SecretKeySpec(password, encryptAlgorithm);
		return encrypt(content, key);
	}

	/**
	 * ֱ�Ӷ��ֽ�������AES����
	 * @param content
	 * @param password
	 * @return
	 */
	public static String encrypt2Str(byte[] content, byte[] password) throws Exception {
		return bytes2Base64Str(encrypt(content, password)); // �����ܺ��byte�������Base64����
	}

	/**
	 * ֱ�Ӷ��ֽ�������AES����
	 * @param content
	 * @param password
	 * @return
	 */
	public static byte[] encrypt(byte[] content, String password) throws Exception {
		return encrypt(content, parseHexStr2Bytes(password)); // ��ʮ�����ƴ�������ת��Ϊbyte����
	}

	/**
	 * ֱ�Ӷ��ֽ�������AES����
	 * @param content
	 * @param password
	 * @return
	 */
	public static String encrypt2Str(byte[] content, String password) throws Exception {
		return bytes2Base64Str(encrypt(content, password)); // �����ܺ��byte�������Base64����
	}

	/**
	 * ����
	 * 
	 * @param content
	 *            ��Ҫ���ܵ�����
	 * @param password
	 *            ��������
	 * @return
	 */
	public static byte[] encrypt(String content, String password) throws Exception {
		return encrypt(str2Bytes(content), password); // ��contentת��Ϊbyte��������
	}

	/**
	 * ����
	 * 
	 * @param content
	 *            ����������
	 * @param key
	 *            ������Կ
	 * @return
	 */
	public static byte[] decrypt(byte[] content, SecretKeySpec key) throws Exception {
		Cipher cipher = Cipher.getInstance(encryptType);
		cipher.init(Cipher.DECRYPT_MODE, key);// ��ʼ��
		byte[] result = cipher.doFinal(content);
		return result; // ����
	}

	/**
	 * ����
	 * 
	 * @param content
	 *            ����������
	 * @param password
	 *            ������Կ
	 * @return
	 */
	public static byte[] decrypt(byte[] content, byte[] password) throws Exception {
		SecretKeySpec key = new SecretKeySpec(password, encryptAlgorithm);
		return decrypt(content, key);
	}

	/**
	 * ����
	 * 
	 * @param content
	 *            ����������
	 * @param password
	 *            ������Կ
	 * @return
	 */
	public static byte[] decrypt(byte[] content, String password) throws Exception {		
		// DigestUtils.md5(password), str2Bytes(password)
		return decrypt(content, parseHexStr2Bytes(password)); // ��ʮ�����ƴ�������ת��Ϊbyte����
	}

	/**
	 * ����
	 * 
	 * @param content
	 *            ����������
	 * @param password
	 *            ������Կ
	 * @return
	 */
	public static byte[] decrypt(String content, String password) throws Exception {
		return decrypt(str2Bytes(content), password); // ��contentת��Ϊbyte��������
	}

	/**
	 * ����
	 * 
	 * @param content
	 *            ����������
	 * @param password
	 *            ������Կ
	 * @return
	 */
	public static byte[] decryptStr(String content, String password) throws Exception {
		return decrypt(base64Str2Bytes(content), password); // ��������base64������ַ���ת��Ϊbyte��������
	}

	/**
	 * ��������ת����Base64�ַ���
	 * 
	 * @param buf
	 * @return
	 */
	public static String bytes2Base64Str(byte buf[]) {
		return new Base64().encodeToString(buf);
	}

	/**
	 * ��Base64�ַ���ת��Ϊ������
	 * 
	 * @param base64Str
	 * @return
	 */
	public static byte[] base64Str2Bytes(String base64Str) {
		return new Base64().decode(base64Str);
	}
	
	/**
	 * ��str����md5����
	 * @param str
	 * @return
	 */
	public static String str2Md5Str(String str) {
		return DigestUtils.md5Hex(str);
	}

	/**
	 * ��str����md5����
	 * @param str
	 * @return
	 */
	public static String str2Md5Str(byte[] str) {
		return DigestUtils.md5Hex(str);
	}
	
	/**
	 * ��str����md5����
	 * @param str
	 * @return
	 */
	public static byte[] str2Md5(String str) {
		return DigestUtils.md5(str);
	}

	/**
	 * ��str����md5����
	 * @param str
	 * @return
	 */
	public static byte[] str2Md5(byte[] str) {
		return DigestUtils.md5(str);
	}

	/**
	 * ��16����ת��Ϊ������
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
	 * ��shortֵת��Ϊbyte����
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
	 * ��byte����ת��Ϊshortֵ
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
	 * ��intֵת��Ϊbyte����
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
	 * ��byte����ת��Ϊintֵ
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
	 * ��longֵת��Ϊbyte����
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
	 * ��byte����ת��Ϊlongֵ
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
	 * �ϲ���������
	 * 
	 * @param first
	 * @param second
	 * @return
	 */
	public static byte[] mergeArray(byte[] first, byte[] second) {
		byte[] result = new byte[first.length + second.length];
		System.arraycopy(first, 0, result, 0, first.length); // ������һ������ 
		System.arraycopy(second, 0, result, first.length, second.length); // �����ڶ�������

		return result;
	}

	/**
	 * ������ɼ�����Կ
	 * 
	 * @return
	 */
	public static byte[] generateKey() {
		byte[] k1 = long2ByteArray(random.nextLong());
		byte[] k2 = long2ByteArray(random.nextLong());

		return mergeArray(k1, k2);
	}

	/**
	 * ������ɼ�����Կ,�����㷨���ɵ���Կ����ǿ��̫��,�ݲ�����(��Ҫ���ַ�����չ)
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
	 * @return byte����
	 */
	public static byte[] getUTF(ByteBuffer buffer) {
		short len = buffer.getShort();
		byte[] result = new byte[len];
		buffer.get(result);

		return result;
	}

	/**
	 * @param ByteBuffer
	 * @return byte����
	 * @throws IOException 
	 */
	public static DataOutputStream writeUTF(DataOutputStream os, byte[] value) throws IOException {
		os.writeShort(Short.reverseBytes((short)value.length)); // ��д�������ֽڵĳ���,�뱾���ֽ�����ͬ
		os.write(value); // ��д������

		return os;
	}

	/**
	 * @param ByteBuffer
	 * @return byte����
	 * @throws IOException 
	 */
	public static DataOutputStream writeUTF(DataOutputStream os, String value) throws IOException {
		return writeUTF(os, str2Bytes(value)); // ���ַ���ת��Ϊbyte�����д�뵽�����
	}

	/**
	 * @param sessionKey
	 * @param sessionId
	 * @return ����Base64�����Ticket
	 */
	public static String generateTicket(byte[] sessionKey, String sessionId, byte[] encryptKey) {
		String ticket = null;

		try {
			ByteArrayOutputStream array = new ByteArrayOutputStream();
			DataOutputStream os = new DataOutputStream(array);

			EncryptUtil.writeUTF(os, sessionKey); // д��sessionKey
			EncryptUtil.writeUTF(os, sessionId); // д��sessionId

			os.flush();
			ticket = encrypt2Str(array.toByteArray(), encryptKey); // ���ܺ����ΪBase64
		} catch (Exception e) {
			logger.error(LogUtil.getSysMessage("����Base64�����Ticket", "�쳣" + e));
		}

		return ticket;
	}


	/**
	 * @param sessionKey
	 * @param sessionId
	 * @return ����Base64�����ClientToken
	 * @throws UnsupportedEncodingException 
	 */
	public static String generateTicket(byte[] sessionKey, String sessionId, String pwd) {
		return generateTicket(sessionKey, sessionId, parseHexStr2Bytes(pwd)); // ��ʮ����������ת��Ϊbyte������������ticket
	}
}
