package com.adefreitas.gcf.android.providers;

import java.util.ArrayList;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.adefreitas.gcf.ContextProvider;
import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.android.AndroidGroupContextManager;
import com.adefreitas.gcf.android.bluewave.BluewaveManager;
import com.google.gson.Gson;

/**
 * Bluetooth Context Provider [BLU]
 * Delivers All Bluetooth Device IDs Detected via Discovery
 * 
 * Parameters
 *      TARGET - will return Bluetooth data from the specified targets as soon as they are discovered (e.g., TARGET=device1,device2)  
 * 
 * Author: Adrian de Freitas
 */
public class BluetoothContextProvider extends ContextProvider
{
	// Context Configuration
	private static final String CONTEXT_TYPE = "BLU";
	private static final String LOG_NAME     = "GCF-ContextProvider [" + CONTEXT_TYPE + "]";
	private static final String DESCRIPTION  = "Shares information about which devices are nearby.  This process consumes additional power.";
	
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
	public BluetoothContextProvider(Context context, AndroidGroupContextManager groupContextManager, int scanInterval) 
	{
		super(CONTEXT_TYPE, DESCRIPTION, groupContextManager);
		
		// Stores Important Variables
		this.context = context;
		this.gcm	 = groupContextManager;
		
		// Initializes Values
		this.scanInterval   = scanInterval;
		this.startedScan    = false;
		
		// Initializes Intent Receiver
		this.intentReceiver = new IntentReceiver();
		this.filter 		= new IntentFilter();
		this.filter.addAction(BluetoothDevice.ACTION_FOUND);
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
	
	public void onBluetoothDiscovery(String deviceID)
	{
		try
		{			
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
					
					if (targets.contains(deviceID))
					{
						sendContext(new String[] { subscription.getDeviceID() }, new String[] { "TARGET_FOUND=" + deviceID });
					}
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
			if (intent.getAction().equals(BluetoothDevice.ACTION_FOUND))
			{
				onBluetoothDeviceFound(context, intent);
			}
			else
			{
				Log.e("", "Unknown Action: " + intent.getAction());
			}
		}
		
		private void onBluetoothDeviceFound(Context context, Intent intent)
		{
			BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			onBluetoothDiscovery(BluewaveManager.getDeviceName(device.getName()));
		}
	}
}
