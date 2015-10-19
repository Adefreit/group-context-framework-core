package com.adefreitas.gcf.toolkit;

import java.security.MessageDigest;

public class SHA1 {

	/**
	 * Generates the SHA-1 Hash for the Given String
	 * @param value
	 * @return
	 */
	public static String getHash(String value)
	{
	    String sha1 = "";
	    try
	    {
	        MessageDigest crypt = MessageDigest.getInstance("SHA-1");
	        crypt.reset();
	        crypt.update(value.getBytes("UTF-8"));
	        sha1 = ByteToHexConverter.convert(crypt.digest());
	    }
	    catch(Exception ex)
	    {
	    	ex.printStackTrace();
	    }
	    
	    return sha1;
	}

}
