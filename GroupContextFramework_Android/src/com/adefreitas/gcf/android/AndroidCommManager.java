package com.adefreitas.gcf.android;

import java.lang.ref.WeakReference;
import java.util.Arrays;

import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.adefreitas.gcf.CommManager;
import com.adefreitas.gcf.CommThread;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.messages.CommMessage;

public class AndroidCommManager extends CommManager
{
	public static final boolean DEBUG_MODE = true;
	
	// Static Intents
	public static final String ACTION_COMMTHREAD_CONNECTED = "androidcommmanager.comm_connected";
	public static final String ACTION_CHANNEL_SUBSCRIBED   = "androidcommmanager.comm_subscribed";
	public static final String ACTION_CONNECTION_ERROR     = "androidcommmanager.comm_error";
	
	// Static Extras
	public static final String EXTRA_IP_ADDRESS = "IP_ADDRESS";
	public static final String EXTRA_PORT		= "PORT";
	public static final String EXTRA_CHANNEL	= "CHANNEL";
	public static final String EXTRA_ERROR      = "ERROR";
	
	private CommHandler    commHandler;  // Used by the Communications Thread to Deliver Messages Received
	private ContextWrapper cw;
	
	public AndroidCommManager(AndroidGroupContextManager gcm, ContextWrapper cw)
	{
		super(gcm);

		commHandler = new CommHandler(gcm, cw);
		this.cw     = cw;
		
		if (DEBUG_MODE)
		{
			Thread debugThread = new Thread()
			{
				public void run()
				{
					while (true)
					{					
						try
						{
							String entry = "Active Comm Threads: " + Arrays.toString(getCommThreadKeys());
							Log.d(GroupContextManager.LOG_COMMUNICATIONS, entry);
							sleep(60000);
						}
						catch (Exception ex)
						{
							ex.printStackTrace();
						}
					}
				}
			};
			debugThread.start();	
		}
	}

	@Override
	public String connect(CommMode commMode, String ipAddress, int port)
	{
		// Closes the Comm Thread if it Already Exists	
		try
		{
			String socketKey = this.getCommThreadKey(commMode, ipAddress, port);
			
			// Generates the Correct Kind of Comm Thread
			if (this.getCommThread(socketKey) == null)
			{
				if (commMode == CommMode.UDP_MULTICAST)
				{
					MulticastCommThread t = new MulticastCommThread(this, this.getGroupContextManager().getDeviceID(), port, ipAddress, commHandler);
					t.start();
					
					this.commThreads.put(socketKey, t);
				}
				else if (commMode == CommMode.TCP)
				{
					TCPCommThread t = new TCPCommThread(this, this.getGroupContextManager().getDeviceID(), port, ipAddress, commHandler);
					t.start();
					
					this.commThreads.put(socketKey, t);
				}
				else if (commMode == CommMode.MQTT)
				{
					MqttCommThread t = new MqttCommThread(this, this.getGroupContextManager().getDeviceID(), ipAddress, port, commHandler, cw);
					t.start();
					
					this.commThreads.put(socketKey, t);
				}
				else
				{
					Log.d(GroupContextManager.LOG_COMMUNICATIONS, "Communications Mode Not Recognized " + commMode);
				}	
			}
			else
			{
				Log.d(GroupContextManager.LOG_COMMUNICATIONS, "Socket " + socketKey + " Already Created");
				this.notifyConnected(this.getCommThread(socketKey));
			}
			
			return socketKey;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		return null;
	}
	
	public void notifyConnected(CommThread ct)
	{
		Intent intent = new Intent(ACTION_COMMTHREAD_CONNECTED);
		intent.putExtra(EXTRA_IP_ADDRESS, ct.getIPAddress());
		intent.putExtra(EXTRA_PORT, ct.getPort());
		cw.getBaseContext().sendBroadcast(intent);
	}
	
	public void notifySubscribed(CommThread ct, String channel)
	{
		Intent intent = new Intent(ACTION_CHANNEL_SUBSCRIBED);
		intent.putExtra(EXTRA_IP_ADDRESS, ct.getIPAddress());
		intent.putExtra(EXTRA_PORT, ct.getPort());
		intent.putExtra(EXTRA_CHANNEL, channel);
		cw.getBaseContext().sendBroadcast(intent);
	}
	
	/**
	 * This Class Handles Messages Returned from the Comm Threads 
	 * @author adefreit
	 */
	static class CommHandler extends Handler
	{
		private final WeakReference<AndroidGroupContextManager> gcmReference; 

		public CommHandler(AndroidGroupContextManager gcm, ContextWrapper cw) 
		{
			gcmReference = new WeakReference<AndroidGroupContextManager>(gcm);
			//cwReference  = new WeakReference<ContextWrapper>(cw);
	    }
		
		public void handleMessage(Message msg)
		{
			AndroidGroupContextManager gcm = gcmReference.get();
			
			if (msg.obj instanceof CommMessage)
			{
				CommMessage message = (CommMessage)msg.obj;				
				gcm.onMessage(message);	
			}
		}
	}
}
