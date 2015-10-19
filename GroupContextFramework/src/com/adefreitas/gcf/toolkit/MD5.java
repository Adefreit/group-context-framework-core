package com.adefreitas.gcf.toolkit;

import java.security.MessageDigest;

public class MD5 
{
	public static String getHash(String value)
	{
		String result = null;
		
		try
		{
			MessageDigest digest = MessageDigest.getInstance("MD5");
			digest.reset();
			digest.update(value.getBytes("UTF-8"));
			result = ByteToHexConverter.convert(digest.digest());
		}
		catch (Exception ex)
		{
			System.out.println("A problem occurred while calculating the MD5 hash of " + value + ": " + ex.getMessage());
		}
		
		return result;
	}
}
