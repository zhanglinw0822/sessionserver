package tpme.PMES.timebargain.server.util;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * �ַ���ʵ����
 * @author zhousp
 */
public class StringUtil {
    //~ Static fields/initializers =============================================

    private final static Log log = LogFactory.getLog(StringUtil.class);

    //~ Methods ================================================================

    /**
     * �����㷨���ַ�������
     * @param ���ܵ��ַ���
     * @param �㷨
     * @return ���ܺ���ַ���
     */
    public static String encodePassword(String password, String algorithm) {
        byte[] unencodedPassword = password.getBytes();

        MessageDigest md = null;

        try {
            // first create an instance, given the provider
            md = MessageDigest.getInstance(algorithm);
        } catch (Exception e) {
            log.error("Exception: " + e);

            return password;
        }

        md.reset();

        // call the update method one or more times
        // (useful when you don't know the size of your data, eg. stream)
        md.update(unencodedPassword);

        // now calculate the hash
        byte[] encodedPassword = md.digest();

        StringBuffer buf = new StringBuffer();

        for (int i = 0; i < encodedPassword.length; i++) {
            if ((encodedPassword[i] & 0xff) < 0x10) {
                buf.append("0");
            }

            buf.append(Long.toString(encodedPassword[i] & 0xff, 16));
        }

        return buf.toString();
    }

    /**
     * ʹ�� Base64���б���
     * @param str
     * @return String
     */
    public static String encodeString(String str)  {
        sun.misc.BASE64Encoder encoder = new sun.misc.BASE64Encoder();
        return encoder.encodeBuffer(str.getBytes()).trim();
    }

    /**
     * ʹ�� Base64���н���
     * @param str
     * @return String
     */
    public static String decodeString(String str) {
        sun.misc.BASE64Decoder dec = new sun.misc.BASE64Decoder();
        try {
            return new String(dec.decodeBuffer(str));
        } catch (IOException io) {
        	throw new RuntimeException(io.getMessage(), io.getCause());
        }
    }
	/**
	 * ����lenλ����ַ���
	 * @param len  ������
	 * @return String
	 */
	public static String generateRandomStr(int len)
	{
		String str = "";
		Random random = new Random(); 
		for (int i = 0;i < len; i++)
		{  
			str += String.valueOf(random.nextInt(10));  
		} 
		return str;
	}
	
	/**
	 * �����������ָ�����β���ַ���Ҳ����ȷ�����ַ�������
	 * 
	 * @param original
	 * @param regex
	 * @return
	 */
	public static String[] split(String original, String regex) {
		ArrayList ary = new ArrayList();
		String vlu = null;
		while (true) {
			if (original.indexOf(regex) != -1) {
				vlu = original.substring(0, original.indexOf(regex));
				ary.add(vlu);
				original = original.substring(original.indexOf(regex) + 1);
			} else {
				ary.add(original);
				break;
			}
		}
		return (String[]) ary.toArray(new String[0]);
	}
	
	/**
	 * �ж��ַ����Ƿ�������
	 * @param str
	 * @return
	 */
	public static boolean isNumeric(String str)
	{
       Pattern pattern = Pattern.compile("[0-9]*");
       Matcher isNum = pattern.matcher(str);
       if( !isNum.matches() ) {
          return false;
       }
       return true;
	}

    public static void main(String[] args) {
//    	String id = "0808";
//    	System.out.println("****".replace('*', ' ').trim());
//    	System.out.println(isNumeric("11200"));
//    	System.out.println(id.substring(0));
//    	
//    	System.out.println(id.startsWith(""));
    	//System.out.println("encodeString(00000000):" + StringUtil.encodeString("00000000"));
    	/*
    	Map map = new TreeMap();
    	map.put("2009-03-21", "2009-03-21");
    	map.put("2009-03-01", "2009-03-01");
    	map.put("2009-03-25", "2009-03-25");
    	map.put("2009-03-04", "2009-03-04");
    	String curDate = "2009-05-10";
    	String validDate=null;
    	Iterator itr = map.keySet().iterator();		
		while(itr.hasNext()) 
		{
			String d = (String)itr.next();
			System.out.println(d);
			if(curDate.compareTo(d) >= 0)
			{
				validDate = d;
				continue;
			}
			else
			{
				break;
			}
		}
		System.out.println("validDate:" + validDate);
		*/
    	System.out.println("encodeString(111111):" + StringUtil.encodeString("111111"));
    }
}
