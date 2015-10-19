package com.adefreitas.gcf;

import java.util.ArrayList;

import com.adefreitas.gcf.messages.CommMessage;

public abstract class CommThread extends Thread
{		
	// Constants
	public static final String PUBLIC_CHANNEL = "gcf/public";
	
	// Connection Settings
	private CommManager commManager;
	private String 		ipAddress;
	private int  		port;
	
	// DEBUG:  Tracks How Many Times a Comm Thread was Restarted
    protected int count = 0;
	
    // EXPERIMENTAL:  Determines what Devices Have Been Observed Through this Channel
	private ArrayList<String> arp;
		
	/**
	 * Constructor
	 * @param commManager
	 */
	public CommThread(CommManager commManager)
	{
		this.commManager = commManager;
		this.arp		 = new ArrayList<String>();
	}
	
	/**
	 * Connects to the Specified IP address and Port
	 * @param ipAddress
	 * @param port
	 */
	public void connect(String ipAddress, int port)
	{
		// Saves Default Settings
		this.ipAddress = ipAddress;
		this.port      = port;
		
		// TODO:  Remove when we no longer care about how many times it reconnects
		count++;
		
		// Subscribes to Default Channels
		if (supportsChannels())
		{
			//this.subscribeToChannel(this.getDefaultPublicChannel());
			this.subscribeToChannel(this.getDefaultDeviceChannel());
		}
	}
	
	/**
	 * Closes the Connection
	 */
	public abstract void close();
	
	/**
	 * Sends a Message
	 * @param message
	 */
	public abstract void send(CommMessage message);

	/**
	 * Sends a Message to a Specific Channel.  Important:  Not all comm thread types support channels.  You need to check using the supportsChannels() method.
	 * @param message
	 * @param channel
	 */
	public void send(CommMessage message, String channel)
	{
		// By Default, Just Send the Normal Way
		// Each thread needs to override this method
		this.send(message);
	}
	
	/**
	 * Returns TRUE if this comm thread utilizes a channel mechanism
	 * @return
	 */
	public boolean supportsChannels()
	{
		return false;
	}
	
	/**
	 * Subscribe to a channel (e.g., MQTT topic).  Important:  Not all comm thread types support channels.  You need to check using the supportsChannels() method.
	 * @param channel
	 */
	public void subscribeToChannel(String channel)
	{
		// Do Nothing by Default
	}
	
	/**
	 * Unsubscribes from a channel.  Important:  Not all comm thread types support channels.  You need to check using the supportsChannels() method.
	 * @param channel
	 */
	public void unsubscribeToChannel(String channel)
	{
		// Do Nothing by Default
	}
	
	/**
	 * Returns TRUE if this comm thread is connected to the specified channel
	 * @param channel
	 * @return
	 */
	public boolean isSubscribedToChannel(String channel)
	{
		// By Default, Assume that the Channel is Not Subscribed
		// It is up to the developer to implement this for specific comm threads
		return false;
	}
	
	/**
	 * Accesses the Communications Manager that is Overseeing this Thread
	 * @return
	 */
	public CommManager getCommManager()
	{
		return commManager;
	}
	
	/**
	 * Returns the IP Address that this comm thread is connected to.
	 * @return
	 */
	public String getIPAddress()
	{
		return ipAddress;
	}
	
	/**
	 * Returns the Port that this comm thread is connected to.
	 * @return
	 */
	public int getPort()
	{
		return port;
	}
	
	/**
	 * Returns the Default PUBLIC Channel
	 * @return
	 */
	public String getDefaultPublicChannel()
	{
		return PUBLIC_CHANNEL;
	}
	
	/**
	 * Returns the Default PRIVATE Channel
	 * @return
	 */
	public String getDefaultDeviceChannel()
	{
		return "dev/" + commManager.getGroupContextManager().getDeviceID();
	}
	
	/**
	 * Adds a Device ID to the Comm Thread's ARP Table
	 * @param deviceID
	 */
	public void addToArp(String deviceID)
	{
		if (!arp.contains(deviceID))
		{
			this.getCommManager().getGroupContextManager().log(GroupContextManager.LOG_COMMUNICATIONS, "Associating " + deviceID + " with " + ipAddress + ":" + port);
			arp.add(deviceID);
		}
	}
	
	/**
	 * Tells You if this Thread has Ever Received a Message from the Specified Device ID
	 * @param deviceID
	 * @return
	 */
	public boolean receivesMessagesFrom(String deviceID)
	{
		return arp.contains(deviceID);
	}
	
	/**
	 * Removes all Illegal Characters from a String
	 * @param originalString
	 * @return
	 */
	public String processString(String originalString)
	{
		char[] result 		    = new char[originalString.length()];
		int    currentCharacter = 0;
			
		for (int c=0; c < originalString.length(); c++)
		{
			char character = originalString.charAt(c);
				
			if ((int)character > 0)
			{
				result[currentCharacter] = character;
				currentCharacter++;
			}
		}
		
		return String.valueOf(result).trim();
	}

}
