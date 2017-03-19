package org.archicontribs.database;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DBChecksum {
	/**
	 * Calculate a MD5 from a StringBuilder
	 */
	public static String calculateChecksum(StringBuilder input) {
		return calculateChecksum(input.toString().getBytes());
	}
	
	/**
	 * Calculate a MD5 from a String
	 */
	public static String calculateChecksum(String input) {
		return calculateChecksum(input.getBytes());
	}
	
	/**
	 * Calculate a MD5 from a byte array
	 */
	public static String calculateChecksum(byte[] bytes) {
	    if ( bytes == null )
	    	return null;
	    
		try {
	    	MessageDigest md;
			md = MessageDigest.getInstance("MD5");
	    	md.update(bytes);
	    	BigInteger hash = new BigInteger(1, md.digest());
	    	return hash.toString(16);
		} catch (NoSuchAlgorithmException e) {
			return Integer.toString(bytes.hashCode());
		}
	}
}
