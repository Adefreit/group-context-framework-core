package com.adefreitas.gcf.android;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.adefreitas.gcf.CommManager;
import com.adefreitas.gcf.CommThread;
import com.adefreitas.gcf.messages.CommMessage;
import com.adefreitas.gcf.messages.ComputeInstruction;
import com.google.gson.Gson;

public class MqttCommThread extends CommThread implements MqttCallback
{
	private static final String  LOG_NAME      = "GCF-MQTT [" + Calendar.getInstance().getTimeInMillis() + "]";
	private static final boolean DEBUG         = true; 
	private static final int     RETRY_TIMEOUT = 5000;
	
	// No MQTT Messages are Ever Expected to be Guaranteed to Arrive
	private static final int QOS = 0;
	
    private Gson    gson;
    private Handler commHandler;
    
    private String			  brokerIP;
    private int				  port;
    private MqttClient     	  client;
    private ArrayList<String> channels;
    private String         	  deviceID;
    private ContextWrapper    cw;
    
    ArrayList<MqttMessage>  sendBuffer = new ArrayList<MqttMessage>();
    HashMap<String, String> channelARP = new HashMap<String, String>();
    
    private boolean run;
    private int 	disconnectCount = 0;
    
    /**
     * Constructor
     * @param deviceID
     * @param serverIP
     * @param port
     * @param commHandler
     * @param cw
     */
    public MqttCommThread(CommManager commManager, String deviceID, String brokerIP, int port, Handler commHandler, ContextWrapper cw)  
	{
    	super(commManager);
    	
		this.deviceID    = deviceID;
	    this.gson 	   	 = new Gson();
	    this.commHandler = commHandler;
	    this.cw 		 = cw;
	    this.channels	 = new ArrayList<String>();
	    this.run         = true; 

	    this.brokerIP = brokerIP;
	    this.port     = port;
    }
        
    /**
     * This is the Maintenance Thread.  It continually checks to make sure that we are still connected
     */
    public void run()
    {    	
    	Log.d(LOG_NAME, "MQTT Comm Thread Running Maintenance Check");
    	
    	try
    	{
    		while (run)
    		{
    			if (client == null || !client.isConnected())
    			{
    				connect(brokerIP, port);
    			}
    			else
    			{
    				Log.d(LOG_NAME, "MQTT Comm Online: " + this.getIPAddress() + ":" + this.getPort() + ":" + Arrays.toString(channels.toArray(new String[0])) + " " + this.count + " attempt(s); " + this.disconnectCount + " disconnect(s).");	
    			}
    			
    			sleep(60000);
    		}
    	}
    	catch (Exception ex)
    	{
    		ex.printStackTrace();
    		close();
    	}
    }
    
	@Override
	public void connect(final String serverIP, final int port)
	{			
		super.connect(serverIP, port);
						
		// Creates a Networking Thread to Establish the Connection
		Thread connectThread = new ConnectionThread(serverIP, port);
		connectThread.start();
	}
	
	public void disconnect()
	{	
		try
		{
			if (client != null && client.isConnected())
			{
				client.disconnect();
				client.close();
				client = null;		
			}	
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	@Override
	public void close()
	{
		// Stops the Maintenance Thread
		run = false;
	}

	// Communication Methods
	@Override
	public void send(CommMessage message)
	{
		ArrayList<String> channelsToSend = new ArrayList<String>();
		boolean 		  broadcast      = message.getDestination().length == 0;
		
		// Looks for Channels for Specific Destinations
		for (String deviceID : message.getDestination())
		{
			if (channelARP.containsKey(deviceID) && !channelsToSend.contains(channelARP.get(deviceID)))
			{
				channelsToSend.add(channelARP.get(deviceID));
			}
			else
			{
				broadcast = true;
				break;
			}
		}

		// TODO:  Evaluate whether or not this is actually needed
		broadcast = broadcast || channelsToSend.size() == 0;
		
		// Determines Whether to Broadcast the Message or to Only Send it to Select Channels
		if (broadcast)
		{
			for (String channel : channels)
			{
				send(message.getMessageType(), channel, gson.toJson(message));	
			}
		}
		else
		{
			if (channelsToSend.size() == 0)
			{
				Log.d(LOG_NAME,  "No channels found for " + message.toString());
			}
			else
			{
				for (String channel : channelsToSend)
				{
					send(message.getMessageType(), channel, gson.toJson(message));
				}	
			}
		}
	}
	
	public void send(CommMessage message, String channel)
	{
		if (message != null)
		{
			send(message.getMessageType(), channel, gson.toJson(message));		
		}
	}

	private void send(String messageType, String channel, String message)
	{
		try
		{
			MqttMessage msg = new MqttMessage();
			msg.setPayload(message.getBytes());
			msg.setQos(QOS);
			msg.setRetained(false);
			sendBuffer.add(msg);
			
			if (sendBuffer.size() > 3)
			{
				Log.d(LOG_NAME,  "Buffer exceeding max size.  Removing the oldest message.");
				sendBuffer.remove(0);
			}
			
			if (client != null && client.isConnected())
			{
				for (MqttMessage m : sendBuffer)
				{
					Log.d(LOG_NAME,  "Sending " + messageType + " [" + message.length() + " bytes] to channel " + channel);
					client.getTopic(channel).publish(m);
				}
		
				sendBuffer.clear();
			}
			else
			{
				Log.d(LOG_NAME,  "Buffering " + message.length() + " bytes.");
				close();
				connect(this.getIPAddress(), this.getPort());
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
   	
	// PAHO Methods
	@Override
	public void connectionLost(Throwable arg0)
	{
		Toast.makeText(cw, "MQTT Connection Lost: " + this.getIPAddress() + ":" + this.getPort(), Toast.LENGTH_SHORT).show();
		close();
		
		disconnectCount++;
		connect(this.getIPAddress(), this.getPort());
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken t)
	{

	}

	@Override
	public void messageArrived(String channel, MqttMessage message) throws Exception
	{
		String 	    s   = new String(message.getPayload(), "UTF-8");
		CommMessage msg = CommMessage.jsonToMessage(s);
		//Log.d(LOG_NAME, "Received: " + s.length());
		
		if (commHandler != null && msg != null)
		{			
			if (!(msg instanceof ComputeInstruction))
			{
				// Allows this Thread to Track WHO it has Seen Messages From
				this.addToArp(msg.getDeviceID());
				
				// Allows this Thread to Track WHICH CHANNEL a Device is On
				//if (!channelARP.containsKey(msg.getDeviceID()) || !channelARP.get(msg.getDeviceID()).equals(channel))
				{
					channelARP.put(msg.getDeviceID(), channel);
					//Log.d(LOG_NAME, "Associating " + msg.getDeviceID() + " with channel [" + channel + "]");	
				}
			}
			
			Message m = commHandler.obtainMessage();
			m.obj = msg;
			commHandler.sendMessage(m);
		}
	}
	
	// Channel Methods
	public boolean supportsChannels()
	{
		return true;
	}
	
	public void subscribeToChannel(final String channel)
	{	
		if (client != null && client.isConnected())
		{
			Thread subscribeThread = new SubscribeThread(channel);
			subscribeThread.start();	
		}
		else
		{
			Log.d(LOG_NAME, "MQTT cannot subscribe to " + channel + " at this time, but will the next time it connects.");
			if (!channels.contains(channel))
			{
				this.channels.add(channel);
			}
		}
	}
	
	public void unsubscribeToChannel(final String channel)
	{
		if (client != null && client.isConnected() && isSubscribedToChannel(channel))
		{
			Thread unsubscribeThread = new Thread()
			{
				boolean success = false;
				int     count	= 1;
				
				public void run()
				{
					try
					{
						if (channel != null && channels.contains(channel))
						{
							while (!success)
							{
								// Makes Sure the Client it Good to Go Before Officially Subscribing
								if (client != null && client.isConnected())
								{
									Log.d(LOG_NAME, "ATTEMPT " + count + ":  MQTT unsubscribing from channel: " + channel);
									client.unsubscribe(channel);	
									channels.remove(channel);
									
									// Cleaning the channelARP
									HashMap<String, String> newARP = new HashMap<String, String>(channelARP);
									
									for (String devID : channelARP.keySet())
									{
										if (newARP.get(devID).equals(channel))
										{
											newARP.remove(devID);
											Log.d(LOG_NAME, "Disassociating " + devID + " with channel " + channel);
										}
									}
									
									// Replaces the Old ChannelARP with a New Version
									channelARP = newARP;
									
									success = true;
								}
								else
								{
									Log.d(LOG_NAME, "ATTEMPT " + count + ":  Could Not Unsubscribe from " + channel);
									sleep(100);
								}
								
								count++;
							}
						}
					}
					catch (Exception ex)
					{
						ex.printStackTrace();
					}				
				}
			};
			unsubscribeThread.start();	
		}
		else
		{
			channels.remove(channel);
		}
	}
	
	public boolean isSubscribedToChannel(String channel)
	{
		return channels.contains(channel);
	}

	// Threads
	public class ConnectionThread extends Thread
	{
		Date   dateCreated;
		String serverIP;
		int    port;
		
		public ConnectionThread(String serverIP, int port)
		{
			this.dateCreated = new Date();
			this.serverIP    = serverIP;
			this.port        = port;
		}
		
		public void run()
		{
			try
			{
	    		Log.d(LOG_NAME, "Attempting to connect to " + serverIP + ":" + port + " . . . ");
	    		
				MqttConnectOptions options = new MqttConnectOptions();
				options.setUserName(deviceID);
				options.setKeepAliveInterval(300);
				options.setCleanSession(true);
				
				client = new MqttClient("tcp://" + serverIP + ":" + port, "mqtt_" + new Date().getTime(), null);
				client.connect(options);
				client.setCallback(MqttCommThread.this);
				
				if (client.isConnected())
				{
					// Creates a Backup List of Channels
					ArrayList<String> previousChannels = new ArrayList<String>(channels);
					channels.clear();
					
					// Reconnects to Previously Connected Channels
					for (String channel : previousChannels)
					{
						Log.d(LOG_NAME, "Reconnecting to: " + channel);
						subscribeToChannel(channel);
					}
					
					// Reports Success
					Log.d(LOG_NAME, "MQTT Connected: " + serverIP);
					
					// Reports that the Connection was Successful
					((AndroidCommManager)(getCommManager())).notifyConnected(MqttCommThread.this);	
				}
				else
				{
					// Reports Success
					Log.e(LOG_NAME, "MQTT Connection Failure: " + serverIP);
				}
			}
			catch (Exception ex)
			{
				Intent intent = new Intent(AndroidCommManager.ACTION_CONNECTION_ERROR);
				intent.putExtra(AndroidCommManager.EXTRA_IP_ADDRESS, serverIP);
				intent.putExtra(AndroidCommManager.EXTRA_PORT, port);
				intent.putExtra(AndroidCommManager.EXTRA_ERROR, "Cannot connect to MQTT Broker: " + serverIP);
				cw.sendBroadcast(intent);
				
				Log.e(LOG_NAME, "MQTT Connection Failure: " + serverIP);
				
//				if (client != null)
//				{
//					close();
//				}
			}		
		}
	}
	
	public class SubscribeThread extends Thread
	{
		String  channel;
		boolean success;
		int 	i;
		
		public SubscribeThread(String channel)
		{
			this.channel = channel;
			this.success = false;
			this.i       = 1;
		}
		
		public void run()
		{
			try
			{
				if (channel != null && !channels.contains(channel))
				{
					while (!success)
					{
						// Makes Sure the Client it Good to Go Before Officially Subscribing
						if (client != null && client.isConnected() && !channels.contains(channel))
						{
							Log.d(LOG_NAME, "ATTEMPT " + i + ":  MQTT subscribing to: " + channel);
							channels.add(channel);
							client.subscribe(channel);
							
							success = true;
							break;
						}
						else
						{
							Intent intent = new Intent(AndroidCommManager.ACTION_CONNECTION_ERROR);
							intent.putExtra(AndroidCommManager.EXTRA_IP_ADDRESS, getIPAddress());
							intent.putExtra(AndroidCommManager.EXTRA_PORT, getPort());
							intent.putExtra(AndroidCommManager.EXTRA_ERROR, "Cannot subscribe to channel " + channel);
							cw.sendBroadcast(intent);
							
							Log.d(LOG_NAME, "ATTEMPT " + i + ":  Could Not Subscribe to " + channel);
							sleep(RETRY_TIMEOUT);
						}	
						
						i++;
					}
					
					// Reports that the Subscription was Successful
					((AndroidCommManager)(getCommManager())).notifySubscribed(MqttCommThread.this, channel);
				}
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}	
		}
	}
}