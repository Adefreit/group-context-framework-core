package com.adefreitas.gcf.android.providers;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.adefreitas.gcf.ContextProvider;
import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.android.AndroidGroupContextManager;
import com.adefreitas.gcf.android.bluewave.BluewaveManager;
import com.adefreitas.gcf.android.bluewave.JSONContextParser;
import com.adefreitas.gcf.messages.ContextData;
import com.adefreitas.gcf.messages.ContextRequest;
import com.google.gson.Gson;

/**
 * Bluewave Context Provider [BLUEWAVE]
 * Delivers (Over the Network) All Bluewave Context Received by the Bluewave Manager as it Arrives
 * It also reports the IDs of the Bluetooth devices encountered at the specified interval
 * 
 * Parameters
 *      TARGET - will only return Bluewave data from the specified targets (e.g., TARGET=device1,device2)  
 * 
 * Author: Adrian de Freitas
 */
public class BluewaveContextProvider extends ContextProvider
{
	// Context Configuration
	private static final String CONTEXT_TYPE = "BLUEWAVE";
	private static final String LOG_NAME     = "GCF-ContextProvider [" + CONTEXT_TYPE + "]";
	
	// Link to the Application
	private Context context;
	private AndroidGroupContextManager gcm;
	
	// Scan Interval (in ms)
	private int scanInterval;
	
	// Flag Indicating Whether or Not this Context Provider STARTED Bluewave
	private boolean startedScan;
	
	// Intent Receivers
	private IntentFilter   filter;
	private IntentReceiver intentReceiver;
	
	/**
	 * Constructor
	 * @param application
	 * @param groupContextManager
	 */
	public BluewaveContextProvider(Context context, AndroidGroupContextManager groupContextManager, int scanInterval) 
	{
		super(CONTEXT_TYPE, groupContextManager);
		
		// Stores Important Variables
		this.context = context;
		this.gcm	 = groupContextManager;
		
		// Initializes Values
		this.scanInterval   = scanInterval;
		this.startedScan    = false;
		
		// Initializes Intent Receiver
		this.intentReceiver = new IntentReceiver();
		this.filter 		= new IntentFilter();
		this.filter.addAction(BluewaveManager.ACTION_OTHER_USER_CONTEXT_RECEIVED);
	}

	@Override
	public void start() 
	{
		this.getGroupContextManager().log(LOG_NAME, this.getContextType() + " Provider Started");
		
		if (!gcm.getBluewaveManager().isScanning())
		{
			startedScan = true;
		}
		
		gcm.getBluewaveManager().startScan(scanInterval);
		
		context.registerReceiver(intentReceiver, filter);
	}

	@Override
	public void stop() 
	{
		if (startedScan)
		{
			startedScan = false;
			gcm.stopBluewaveScan();
		}
		
		if (this.isInUse())
		{
			context.unregisterReceiver(intentReceiver);	
		}
		
		this.getGroupContextManager().log(LOG_NAME, this.getContextType() + " Provider Stopped");
	}

	@Override
	public double getFitness(String[] parameters) 
	{
		return 1.0;
	}

	@Override
	public void sendContext() 
	{		
		sendContext(this.getSubscriptionDeviceIDs(), new String[] { "DEVICES=" + new Gson().toJson(gcm.getBluewaveManager().getNearbyDevices(getRefreshRate()))});
	}
	
	public void sendBluewaveContext(String context, boolean isNew)
	{
		try
		{
			JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_TEXT, context);
			
			// Delivers Context on a Per User Basis!
			for (ContextSubscriptionInfo subscription : this.getSubscriptions())
			{
				String targetString = subscription.getParameter("TARGET");
				
				if (targetString != null && targetString.length() > 0)
				{
					ArrayList<String> targets = new ArrayList<String>();
					
					for (String s : targetString.split(","))
					{
						targets.add(s);
					}
					
					if (targets.contains(parser.getDeviceID()))
					{
						sendContext(new String[] { subscription.getDeviceID() }, new String[] { "DEVICE=" + parser.getDeviceID(), "CONTEXT=" + context, "IS_NEW=" + isNew });
					}
				}
				else
				{
					sendContext(new String[] { subscription.getDeviceID() }, new String[] { "DEVICE=" + parser.getDeviceID(), "CONTEXT=" + context, "IS_NEW=" + isNew });
				}
			}
		}
		catch (Exception ex)
		{
			Log.e(LOG_NAME, "Error Parsing Bluewave Context: " + ex.getMessage());
		}
	}
	
	// Intent Receiver --------------------------------------------------------------------------
	private class IntentReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent) 
		{	
			if (intent.getAction().equals(BluewaveManager.ACTION_OTHER_USER_CONTEXT_RECEIVED))
			{
				onBluewaveContextReceived(context, intent);
			}
			else
			{
				Log.e("", "Unknown Action: " + intent.getAction());
			}
		}
		
		private void onBluewaveContextReceived(Context context, Intent intent)
		{
			// This is the Raw JSON from the Device
			String  json  = intent.getStringExtra(BluewaveManager.EXTRA_OTHER_USER_CONTEXT);
			boolean isNew = intent.getBooleanExtra(BluewaveManager.EXTRA_IS_NEW_CONTEXT, true);
						
			sendBluewaveContext(json, isNew);
		}
	}
}
