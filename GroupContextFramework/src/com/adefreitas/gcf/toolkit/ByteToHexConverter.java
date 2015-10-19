package com.adefreitas.gcf.toolkit;

import java.util.Formatter;

public class ByteToHexConverter 
{
	/**
	 * Converts a Byte to HEX
	 * @param hash
	 * @return
	 */
	public static String convert(final byte[] hash)
	{
	    Formatter formatter = new Formatter();
	    
	    for (byte b : hash)
	    {
	        formatter.format("%02x", b);
	    }
	    
	    String result = formatter.toString();
	    formatter.close();
	    
	    return result;
	}
}
