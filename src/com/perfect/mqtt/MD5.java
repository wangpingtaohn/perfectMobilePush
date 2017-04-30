package com.perfect.mqtt;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;

public class MD5 {
	
	/**
	 * 对 strSrc 进行GBK编码的MD5散列
	 * @param strSrc
	 * @return 16进制串，如果byte转化为16进制串，只是1位串，需要在前面追加0,保持2位16进制串 
	 * @throws  UnsupportedEncodingException NoSuchAlgorithmException
	 */
	public final static String getMd5(String strSrc,int length) throws Exception {
		char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
				'a', 'b', 'c', 'd', 'e', 'f' };
		
			byte[] strTemp = strSrc.getBytes("GBK");
			MessageDigest mdTemp = MessageDigest.getInstance("MD5");
			mdTemp.update(strTemp);
			byte[] md = mdTemp.digest();
			int j = md.length;
			char str[] = new char[j * 2];
			int k = 0;
			for (int i = 0; i < j; i++) {
				byte byte0 = md[i];
				str[k++] = hexDigits[byte0 >>> 4 & 0xf];
				str[k++] = hexDigits[byte0 & 0xf];
			}
			String result = new String(str);
			if (16 == length) {
				result = result.substring(8, 24);
			}
			return result;
	
	}
	
	/**
	 * 对 strSrc按制定编码方式进行编码MD5散列
	 * 
	 * @param strSrc
	 * @param encode
	 * @return 16进制串，如果byte转化为16进制串，只是1位串，需要在前面追加0,保持2位16进制串 
	 * @throws  UnsupportedEncodingException NoSuchAlgorithmException
	 */
	public final static String getMd5(String strSrc, String encode, int length) throws Exception {
		char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
				'a', 'b', 'c', 'd', 'e', 'f' };
		
			byte[] strTemp = strSrc.getBytes(encode);
			MessageDigest mdTemp = MessageDigest.getInstance("MD5");
			mdTemp.update(strTemp);
			byte[] md = mdTemp.digest();
			int j = md.length;
			char str[] = new char[j * 2];
			int k = 0;
			for (int i = 0; i < j; i++) {
				byte byte0 = md[i];
				str[k++] = hexDigits[byte0 >>> 4 & 0xf];
				str[k++] = hexDigits[byte0 & 0xf];
			}
			String result = new String(str);
			if (16 == length) {
				result = result.substring(8, 24);
			}
			return result;
	}
	
	public static String getSecurityInfo(String str) {
		if (null == str) {
			return null;
		}
		char[] ch = str.toCharArray();
		for (int i = 0; i < ch.length; i++) {
			char c = ch[i];
			if (c >= 'a' && c <= 'z') {
				ch[i] = (char) ('a' + 'z' - ch[i]);
			} else if (c >= 'A' && c <= 'Z') {
				ch[i] = (char) ('A' + 'Z' - ch[i]);
			} else if (c >= '0' && c <= '9') {
				String strCh = String.valueOf(c);
				int numCh = Integer.parseInt(strCh);
				numCh = 9 - numCh;
				strCh = String.valueOf(numCh);
				ch[i] = strCh.charAt(0);
			} else {
				ch[i] = c;
			}
		}
		
		return new String(ch);
	}

}
