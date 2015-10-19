package com.adefreitas.gcf.desktop;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.adefreitas.gcf.CommManager;
import com.adefreitas.gcf.CommThread;
import com.adefreitas.gcf.messages.CommMessage;
import com.adefreitas.gcf.messages.ComputeInstruction;
import com.google.gson.Gson;

public class MqttCommThread extends CommThread implements MqttCallback
{
	private static final int 	 KEEP_ALIVE_INTERVAL = 60;
	private static final boolean DEBUG 				 = true;
	private static final int 	 QOS   				 = 0;
	
	private String brokerIP;
	private int    port;
    private Gson   gson;
	
	private MqttClient 		  client;
	private ArrayList<String> channels;
	private String 	   		  deviceID;
    
	// Keeps Track of what Channel
	private ArrayList<MqttMessage>  sendBuffer;
    private HashMap<String, String> channelARP;
	
    private boolean run;
	
    /**
     * Constructor
     * @param port		- the port
     * @param brokerIP	- the IP address of the destination machine (the TCP relay)
     * @param processor - where fully formed messages should be delivered once assembled
     */
	public MqttCommThread(CommManager commManager, String deviceID, String brokerIP, int port)  
	{
		super(commManager);
		
		this.deviceID   = deviceID;
	    this.gson  	    = new Gson();
	    this.channels   = new ArrayList<String>();
	    this.run	    = false;
	    this.sendBuffer = new ArrayList<MqttMessage>();
	    this.channelARP = new HashMap<String, String>();
	    
	    // Repeatedly Tries to Connect
		this.brokerIP = brokerIP;
		this.port     = port;
    }
    
	/**
	 * Repeatedly Listens for Packets as they Arrive
	 */
    public void run()
    {
    	run = true;
    	
    	while (run)
		{
	    	try
	    	{
    			if (client == null || !client.isConnected())
    			{
    				connect(brokerIP, port);
    			}
    			else
    			{   				
    				if (DEBUG)
    				{
        				System.out.println("MQTT Comm Online: " + brokerIP + ":" + port + ":" + Arrays.toString(channels.toArray(new String[0])));
    				}
    			}
    			
    			System.out.println("Sleeping for " + KEEP_ALIVE_INTERVAL * 1000 + " ms.");
    			sleep(KEEP_ALIVE_INTERVAL * 1000);
	    	}
	    	catch (Exception ex)
	    	{
	    		ex.printStackTrace();
	    	}
		}
    	
    	System.out.println("MQTT Comm Thread Terminated.");
    }
	
	/**
	 * Repeatedly attempts to connect to the destination
	 * @param serverIP - the IP address of the destination machine (the TCP relay?)
	 * @param port 	   - the port
	 */
	public void connect(String serverIP, int port)
	{				
		super.connect(serverIP, port);
		
		System.out.println("Connecting to " + serverIP + ":" + port + " . . . ");
		
		// Tracking the Port and IP Address
		this.port      = port;
		this.brokerIP  = serverIP;
		
		try
		{
			MqttConnectOptions options = new MqttConnectOptions();
			options.setUserName(deviceID);
			options.setKeepAliveInterval(KEEP_ALIVE_INTERVAL);
			options.setCleanSession(true);
			
			client = new MqttClient("tcp://" + serverIP + ":" + port, "mqtt_" + new Date().getTime(), null);
			client.connect(options);
			client.setCallback(this);
			
			// Creates a Backup List
			ArrayList<String> previousChannels = new ArrayList<String>(channels);
			channels.clear();
			
			// Reconnects to Previously Connected Channels
			for (String channel : previousChannels)
			{
				subscribeToChannel(channel);
			}
			
			System.out.println("CONNECTED!");
		}
		catch (Exception ex)
		{
			System.out.println("MQTT CONNECT FAILED!");
			ex.printStackTrace();
			
			client = null;
		}
	}
		
	/**
	 * Closes the Socket Connection
	 */
    public void close() 
    {    	
    	run = false;
    }
	
    /**
     * Sends a Communications Message to the MQTT Destination
     */
	public void send(CommMessage message)
    {
		ArrayList<String> channelsToSend = new ArrayList<String>();	
		boolean broadcast = message.getDestination().length == 0;
		
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

		// Determines Whether to Broadcast the Message or to Only Send it to Select Channels
		if (broadcast)
		{
			for (String channel : new ArrayList<String>(channels))
			{
				send(channel, gson.toJson(message));	
			}
		}
		else
		{
			for (String channel : channelsToSend)
			{
				send(channel, gson.toJson(message));
			}
		}
	}

	public void send(CommMessage message, String channel)
	{
		if (message != null)
		{
			send(channel, gson.toJson(message));		
		}
	}
    
    private void send(String channel, String message)
    {
    	try
		{
			MqttMessage msg = new MqttMessage();
			msg.setPayload(message.getBytes());
			msg.setQos(QOS);
			msg.setRetained(false);
			sendBuffer.add(msg);
			
			if (sendBuffer.size() > 1)
			{
				System.out.println("Buffer exceeding max size.  Removing the oldest message.");
				sendBuffer.remove(0);
			}
			
			if (client != null && client.isConnected())
			{
				for (MqttMessage m : sendBuffer)
				{
					client.getTopic(channel).publish(m);
				}
				
				sendBuffer.clear();
			}
			else
			{
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
		//arg0.printStackTrace();
		System.out.println("MQTT Connection Lost.  Reconnecting.");
		connect(brokerIP, port);
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken arg0)
	{
		// TODO Auto-generated method stub
	}
	
	@Override
	public void messageArrived(String channel, MqttMessage message) throws Exception
	{
		String 	    s   = new String(message.getPayload(), "UTF-8");
		CommMessage msg = CommMessage.jsonToMessage(s);
		
		if (msg != null)
		{
			if (!(msg instanceof ComputeInstruction))
			{
				// Allows this Thread to Track WHO it has Seen Messages From
				this.addToArp(msg.getDeviceID());	
				
				// Allows this Thread to Track WHICH CHANNEL a Device is On
				//if (!channelARP.containsKey(msg.getDeviceID()))
				{
					//System.out.println("Learned that " + msg.getDeviceID() + " comes from " + channel);
					channelARP.put(msg.getDeviceID(), channel);	
				}					
			}
			
			((DesktopCommManager)this.getCommManager()).onMessage(msg);
		}	
	}

	// Overridding Methods
	public boolean supportsChannels()
	{
		return true;
	}
	
	public void subscribeToChannel(String channel)
	{
		try
		{
			if (client != null && client.isConnected() && !channels.contains(channel))
			{
				channels.add(channel);
				client.subscribe(channel);
				System.out.println("MQTT Subscribed to " + channel);
			}
			else
			{
				if (!channels.contains(channel))
				{
					this.channels.add(channel);
				}
			}
		}
		catch (Exception ex)
		{
			System.out.println("Problem while subscribing to channel " + channel + ": " + ex.getMessage());
		}
	}
	
	public void unsubscribeToChannel(String channel)
	{
		try
		{
			if (client != null && channels.contains(channel))
			{
				channels.remove(channel);
				client.unsubscribe(channel);
				System.out.println("MQTT Unsubscribed to " + channel);
			}
		}
		catch (Exception ex)
		{
			System.out.println("Problem while unsubscribing to channel " + channel + ": " + ex.getMessage());
		}
	}
	
	public boolean isSubscribedToChannel(String channel)
	{
		return channels.contains(channel);
	}
	
}
