package com.adefreitas.gcf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import com.adefreitas.gcf.messages.CommMessage;

public abstract class CommManager 
{
	// Communications Types
	public enum CommMode { TCP, UDP_MULTICAST, MQTT };
	
	// Link to the Group Context Manager
	private GroupContextManager gcm;
	
	// CommThreads Used by the CommManager (Key = commThreadKey)
	protected HashMap<String, CommThread> commThreads;
	
	// Experimental: Used to Determine which Devices are Accessible (Key = deviceID)
	// NOTE:  This feature is not currently used
	protected HashMap<String, ArpItem> arp;
	
	/**
	 * Constructor
	 * @param gcm
	 */
	public CommManager(GroupContextManager gcm)
	{
		this.commThreads = new HashMap<String, CommThread>();
		this.arp 		 = new HashMap<String, ArpItem>();
		this.gcm 		 = gcm;
	}
	
	/**
	 * Retrieves the GCM
	 * @return
	 */
	protected GroupContextManager getGroupContextManager()
	{
		return gcm;
	}
	
	/**
	 * Connects to a Specific Socket.  Will not create duplicate sockets to the same address/port
	 * @param commMode
	 * @param ipAddress
	 * @param port
	 * @return
	 */
	public abstract String connect(CommMode commMode, String ipAddress, int port);

	/**
	 * Subscribes to a Specific Channel (if it exists)
	 * @param connectionKey
	 * @param channel
	 * @return
	 */
	public boolean subscribe(String connectionKey, String channel)
	{
		CommThread t = getCommThread(connectionKey);
		
		if (t != null)
		{
			if (t.supportsChannels())
			{
				t.subscribeToChannel(channel);
				gcm.log(GroupContextManager.LOG_COMMUNICATIONS, "Comm thread " + connectionKey + " subscribed to " + channel);
				return true;
			}
			else
			{
				gcm.log(GroupContextManager.LOG_COMMUNICATIONS, "Comm thread " + connectionKey + " does not support channels.");
			}
		}
		else
		{
			gcm.log(GroupContextManager.LOG_COMMUNICATIONS, "Comm thread " + connectionKey + " not found.");
		}
		
		return false;
	}

	/**
	 * Unsubscribes from a Specific Channel
	 * @param connectionKey
	 * @param channel
	 * @return
	 */
	public boolean unsubscribe(String connectionKey, String channel)
	{
		CommThread t = getCommThread(connectionKey);
		
		if (t != null)
		{
			if (t.supportsChannels() && t.isSubscribedToChannel(channel))
			{
				t.unsubscribeToChannel(channel);
				gcm.log(GroupContextManager.LOG_COMMUNICATIONS, "Comm thread " + connectionKey + " unsubscribed from " + channel);
				return true;
			}
			else
			{
				gcm.log(GroupContextManager.LOG_COMMUNICATIONS, "Comm thread " + connectionKey + " does not support channels or is already unsubscribed.");
			}
		}
		else
		{
			gcm.log(GroupContextManager.LOG_COMMUNICATIONS, "Comm thread " + connectionKey + " not found.");
		}
		
		return false;
	}
	
	/**
	 * Disconnects from ALL Threads
	 */
	public void disconnect()
	{
		// Disconnects Each Comm Thread Separately
		for (CommThread t : this.commThreads.values())
		{
			t.close();
		}
		
		// Wipes Out All Comm Threads from Memory
		this.commThreads.clear();
		
		System.out.println("All Comm Threads Closed");
	}
	
	/**
	 * Disconnects from a SPECIFIC Thread
	 * @param connectionKey
	 */
	public void disconnect(String connectionKey)
	{
		if (commThreads.containsKey(connectionKey))
		{
			CommThread t = commThreads.get(connectionKey);
			t.close();
			
			commThreads.remove(connectionKey);
		}
	}
	
	/**
	 * Send a Comm Message 
	 * NOTE:  The CommManager will try to find the most efficient channel/thread to use to get the message to the device
	 * @param message
	 */
	public void send(CommMessage message)
	{
		// Prevents Network Access if the Message is Only Directed to this Device!
		if (isSendToSelf(message))
		{
			gcm.onMessage(message);
		}
		else if (isBroadcast(message))
		{
			//System.out.println("CommManager:  Broadcasting.");
			
			// Sends Message to Each Comm Thread Separately
			for (CommThread t : commThreads.values())
			{
				t.send(message);
			}	
		}
		else
		{			
			ArrayList<String> destinations = (message.getDestination() != null) ? new ArrayList<String>(Arrays.asList(message.getDestination())) : new ArrayList<String>();
			
			//System.out.println("CommManager:  Optimizing Path for " + message.getMessageType() + ": " + Arrays.toString(message.getDestination()));
			ArrayList<String> threads = new ArrayList<String>();
			
			// Tries to Find a Destination for Each One
			while (destinations.size() > 0)
			{
				int    count     = 0;
				int    bestCount = 0;
				String bestMatch = null;
				
				// Looks for the Comm Thread that Satisfies the 
				for (String connectionKey : commThreads.keySet())
				{
					// Only Examines Threads that Have Been Examined Before
					if (!threads.contains(connectionKey))
					{
						CommThread t = commThreads.get(connectionKey);
						
						for (String destinationID : destinations)
						{
							count += (t.receivesMessagesFrom(destinationID)) ? 1 : 0;
						}
						
						if (count > bestCount)
						{
							bestCount = count;
							bestMatch = connectionKey;
						}
					}
				}

				if (bestMatch != null)
				{
					threads.add(bestMatch);
					
					// Removes Destinations that We Have Already Covered
					for (String destinationID : new ArrayList<String>(destinations))
					{
						if (commThreads.get(bestMatch).receivesMessagesFrom(destinationID))
						{
							destinations.remove(destinationID);
						}
					}
				}
				else
				{
					//System.out.println("  No Match Found.  Broadcasting to All!");
					
					// If We Cannot Find a Thread for a Destination, Send them to Everyone!
					threads = new ArrayList<String>(commThreads.keySet());
					destinations.clear();
				}
			}
			
			//System.out.println("  Sending to " + threads.size() + " thread(s) [" + (new Date().getTime() - start.getTime()) + " ms]");
			
			// Sends Message to Each Comm Thread Separately
			for (String connectionKey: threads)
			{
				commThreads.get(connectionKey).send(message);
			}	
		}
	}
	
	/**
	 * Sends a Message Along a Specific Comm Connection
	 * @param message
	 * @param connectionKey
	 */
	public void send(CommMessage message, String connectionKey)
	{		
		// Prevents Network Access if the Message is Only Directed to this Device!
		if (isSendToSelf(message))
		{
			gcm.onMessage(message);
		}
		else if (commThreads.containsKey(connectionKey))
		{
			commThreads.get(connectionKey).send(message);
		}
		else
		{
			System.out.println("CommManager: No Comm Channel Found with the Connection Key: " + connectionKey);
		}
	}
		
	/**
	 * Sends a Message Along a Specific Comm Connection AND Channel
	 * WARNING:  Will do nothing if the comm  channel does not support channels!
	 * @param message
	 * @param connectionKey
	 * @param channel
	 */
	public void send(CommMessage message, String connectionKey, String channel)
	{		 
		// Prevents Network Access if the Message is Only Directed to this Device!
		if (isSendToSelf(message))
		{
			gcm.onMessage(message);
		}
		else if (commThreads.containsKey(connectionKey))
		{
			commThreads.get(connectionKey).send(message, channel);
		}
		else
		{
			System.out.println("CommManager: No Comm Channel Found with the Connection Key: " + connectionKey);
		}
	}
	
	/**
	 * Sends a Message Along the Specified Channel
	 * NOTE:  This may send repeats to the same device if they are all on the same channels
	 * @param message
	 * @param channel
	 */
	public void sendUsingChannel(CommMessage message, String channel)
	{
		// Prevents Network Access if the Message is Only Directed to this Device!
		if (isSendToSelf(message))
		{
			gcm.onMessage(message);
		}
		else
		{
			ArrayList<CommThread> threadsToUse = new ArrayList<CommThread>();
			
			for (String deviceID : message.getDestination())
			{
				for (CommThread commThread : commThreads.values())
				{
					if (commThread.receivesMessagesFrom(deviceID))
					{
						if (!threadsToUse.contains(commThread))
						{
							threadsToUse.add(commThread);	
						}
						break;
					}
				}
			}
			
			if (threadsToUse.size() > 0)
			{
				for(CommThread commThread : threadsToUse)
				{
					gcm.log(GroupContextManager.LOG_COMMUNICATIONS, "Sending message to " + message.getDeviceID() + " using channel " + channel);
					commThread.send(message, channel);
				}	
			}
			else
			{
				gcm.log(GroupContextManager.LOG_COMMUNICATIONS, "Cannot Find CommThread for Devices: " + Arrays.toString(message.getDestination()));
			}
		}
	}
	
	/**
	 * Checks to see if a comm thread is connected
	 * @param commMode
	 * @param ipAddress
	 * @param port
	 * @return
	 */
	public boolean isConnected(CommMode commMode, String ipAddress, int port)
	{
		String connectionKey = getCommThreadKey(commMode, ipAddress, port);
		
		return commThreads.containsKey(connectionKey);
	}
			
	/**
	 * Get Comm Thread Key
	 * @param commMode
	 * @param ipAddress
	 * @param port
	 * @return
	 */
	public String getCommThreadKey(CommMode commMode, String ipAddress, int port)
	{
		return commMode + ":" + ipAddress + ":" + port;
	}

	/**
	 * Returns All Comm Thread Keys Managed by this Object
	 * @return
	 */
	public String[] getCommThreadKeys()
	{
		return commThreads.keySet().toArray(new String[0]);
	}
	
	/**
	 * Retrieves a Single Comm Thread with the Specified Connection Key
	 * @param connectionKey
	 * @return
	 */
	public CommThread getCommThread(String connectionKey)
	{
		return commThreads.get(connectionKey);
	}

	/**
	 * Returns all Comm Threads
	 * @return
	 */
	public CommThread[] getCommThreads()
	{
		return commThreads.values().toArray(new CommThread[0]);
	}
	
	// Private Methods -----------------------------------------------------------------------------------------
	private boolean isSendToSelf(CommMessage message)
	{
		return message.getDestination().length == 1 && message.getDestination()[0].equals(gcm.getDeviceID());
	}
	
	private boolean isBroadcast(CommMessage message)
	{
		return message.getDestination() == null || message.getDestination().length == 0;
	}

	/**
	 * This Class Keeps Track of Where a Device has Last been Heard From
	 * @author adefreit
	 *
	 */
	public class ArpItem
	{
		private String deviceID;
		private String connectionKey;
		private String channel;
		
		public ArpItem(String deviceID, String connectionKey, String channel)
		{
			this.deviceID 	   = deviceID;
			this.connectionKey = connectionKey;
			this.channel 	   = channel;
		}

		public String getDeviceID() {
			return deviceID;
		}

		public String getConnectionKey() {
			return connectionKey;
		}

		public String getChannel() {
			return channel;
		}
	}
}
