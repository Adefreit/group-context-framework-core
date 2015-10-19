package com.adefreitas.gcf.desktop;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import com.adefreitas.gcf.CommManager;
import com.adefreitas.gcf.CommThread;
import com.adefreitas.gcf.messages.CommMessage;
import com.google.gson.Gson;

public class MulticastCommThread extends CommThread 
{
	private int    			  port;
	private String 			  group;
	private Gson 			  gson;
	private MulticastSocket   s;
	private boolean 		  alive;
	
	/**
	 * Constructor
	 * @param port  - the port the multicast communications occurs on
	 * @param group - the group address
	 */
	public MulticastCommThread(CommManager commManager, int port, String group)
	{
		super(commManager);
		
		this.port  	   = port;
		this.group 	   = group;
		this.gson 	   = new Gson();
		
		connect(group, port);
	}
	
	public void connect(String ipAddress, int port)
	{
		try
		{
			super.connect(ipAddress, port);
			s = new MulticastSocket(port);
			s.joinGroup(InetAddress.getByName(ipAddress));
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public void close()
	{
		alive = false;
	}
	
	public void run()
	{	
		alive = true;
		
		if (s == null)
		{
			connect(group, port);
		}
		
		while (alive)
		{
			byte buffer[]		  = new byte[60000];
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
			
			try
			{
				s.receive(packet);
				String json = new String(packet.getData(), 0, packet.getLength());
				
				// Sends the Received Message to a Handler to be Processed
				CommMessage msg = CommMessage.jsonToMessage(json);
				
     			// Allows this Thread to Track WHO it has Seen Messages From
     			this.addToArp(msg.getDeviceID());
				
     			((DesktopCommManager)this.getCommManager()).onMessage(msg);	
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
	}

	public void send(CommMessage message)
	{
		// In Android, Sending a Multicast Message is Essentially the Same Thing as
		// Sending a Message Once
		sendOneTime(this.group, this.port, message);
	}

	public void sendOneTime(String ipAddress, int port, CommMessage message)
	{
		// Converts the Message Object into a JSON String
		final String msg 	  	= gson.toJson(message);
		final byte 	 buffer[] 	= msg.getBytes();
		final String IP_ADDRESS = ipAddress;
		final int    PORT 		= port;
		
		// Creates a Thread to Send the Message
		Thread transmissionThread = new Thread(new Runnable()
		{
			public void run()
			{
				try
				{
					DatagramPacket  packet     = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(IP_ADDRESS), PORT);					
					
					MulticastSocket sendSocket = new MulticastSocket();
					sendSocket.send(packet);
					sendSocket.close();
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}
		});
		
		transmissionThread.start();
	}
}
