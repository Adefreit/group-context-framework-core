package com.adefreitas.gcf;

/**
 * This is intended to be used as a debug class.  The IP Addresses and ports here 
 * are used so that you don't nave to manually hard code them in every project.
 * @author adefreit
 *
 */
public class FrameworkSettings 
{	
	// The Port that the Above Server is Listening On
	public static final int    DEV_MULTICAST_PORT = 12345;
	public static final int    DEV_TCP_PORT       = 12345;
	public static final int	   DEV_MQTT_PORT      = 1883;
	public static final int	   DEV_SFTP_PORT      = 22;
	
	/**
	 * This method allows you to replace a device ID with a more human friendly name
	 * @param name
	 * @return
	 */
	public static String getDeviceName(String name)
	{
		String result = name;
		
		if (name.equals("0b37e2c40c010b4f"))
		{
			result = "Nexus 5-A";
		}
		else if (name.equals("8ccd0d04"))
		{
			result = "Device 2";
		}
		else if (name.equals("928c8b15"))
		{
			result = "Device 1";
		}
		else if (name.equals("99a05699"))
		{
			result = "Device 3";
		}
		else if (name.equals("d74e6846"))
		{
			result = "Device 4";
		}
		else if (name.equals("5db834b8"))
		{
			result = "GoPhone-UbicompLab";
		}
		else if (name.equals("HT1A5X103720"))
		{
			result = "HTC_PINK";
		}
		else if (name.equals("C1690615253745E"))
		{
			result = "Tablet_Commons";
		}
		else if (name.equals("C16906141B5445E"))
		{
			result = "Tablet_DevLab";
		}
		else if (name.equals("C169070F762215E"))
		{
			result = "Tablet_UbicompLab";
		}
		else if (name.equals("00092c582a282f"))
		{
			result = "GalaxyS2-1";
		}
		else if (name.equals("R32D103P75E"))
		{
			result = "Nexus10-1";
		}
		else if (name.equals("1a1a41f"))
		{
			result = "ZTE-1";
		}
		else if (name.equals("1a10bdc"))
		{
			result = "ZTE-2";
		}
		else if (name.equals("ca19bb8"))
		{
			result = "ZTE-3";
		}
		
		return result;
	}
}
