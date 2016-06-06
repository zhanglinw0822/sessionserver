package com.happyinsource.tradetest.util;

import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class SecurityUtil {
	   public static byte[] getmd5(byte[] data){
	       try {
	    	   MessageDigest md = MessageDigest.getInstance("MD5");
	           md.reset();
	           md.update(data);
	           return  md.digest();
	       } catch (Exception e) {
	    	   e.printStackTrace();
	       }
	       	return null;
	   }
	   
       /**
        * AES加密
        * 
        * @param content 需要加密的内容
        * @param password  加密密码
        * @return
        */
       public static byte[] AESEncrypt(byte[] content, byte[] key) {
               try {           
            	   Cipher cipher = Cipher.getInstance("AES");
            	   cipher.init(Cipher.ENCRYPT_MODE,  new SecretKeySpec(key, "AES"));
                   return cipher.doFinal(content);
               } catch (Exception e) {
                       e.printStackTrace();
               }
               return null;
       }
       
       public static byte[] AESDecrypt(byte[] content, byte[] key) {
           try {           
        	   Cipher cipher = Cipher.getInstance("AES");
        	   cipher.init(Cipher.DECRYPT_MODE,  new SecretKeySpec(key, "AES"));
               return cipher.doFinal(content);
           } catch (Exception e) {
                   e.printStackTrace();
           }
           return null;
   }
}
