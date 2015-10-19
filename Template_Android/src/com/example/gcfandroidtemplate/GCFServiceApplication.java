package com.example.gcfandroidtemplate;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.adefreitas.gcf.FrameworkSettings;
import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.android.*;
import com.adefreitas.gcf.android.arbiters.ProximityArbiter;
import com.adefreitas.gcf.android.bluewave.*;
import com.adefreitas.gcf.android.toolkit.*;
import com.adefreitas.gcf.android.providers.BluetoothContextProvider;
import com.adefreitas.gcf.android.providers.BluewaveContextProvider;
import com.adefreitas.gcf.android.providers.LocationContextProvider;
import com.adefreitas.gcf.messages.ContextData;
import com.google.gson.Gson;

/**
 * This is a Sample Application Class Used to Abstract GCF's Core Functions
 * 
 * Instructions for Use:
 * 		1.  Change the Package Name to Your Application's Name!
 * 		2.  Make Sure the App Manifest Points to your New Package Name
 * 		3.  Update the COMM_MODE, IP_ADDRESS, and PORT for your particular application
 * 
 * @author Adrian de Freitas
 */
public class GCFServiceApplication extends Application
{	
	// Application Constants
	public static final String  LOG_NAME = "ANDROID_TEMPLATE"; 
	public static final boolean DEBUG 	 = true;

	// GCF Connection Settings
	public static final CommMode COMM_MODE  = CommMode.MQTT;
	public static final String   IP_ADDRESS = "IP_ADDRESS_GOES_HERE";
	public static final int      PORT 	    = 1883;
	
	// GCF Variables
	public  String 	  		  defaultConnectionKey;
	private GCFService 		  gcfService;
	private ServiceConnection gcfServiceConnection = new ServiceConnection() 
	{
	    public void onServiceConnected(ComponentName name, IBinder service) 
	    {
	        GCFService.GCFServiceBinder mLocalBinder = (GCFService.GCFServiceBinder)service;
	        gcfService 								 = mLocalBinder.getService();
	    }
		
		public void onServiceDisconnected(ComponentName name) 
		{
	        gcfService = null;
	    }
	};
	
	// Application Preferences
	private SharedPreferences sharedPreferences;
		
	// Cloud Storage Settings
	private HttpToolkit httpToolkit;
	
	// Object Serialization Tool
	private Gson gson;
	
	// Intent Filters
	private IntentFilter   			  filter;
	private ApplicationIntentReceiver intentReceiver;
	
	/**
	 * One-Time Application Initialization Method
	 * Runs when the Application First Turns On
	 */
	@Override
	public void onCreate() 
	{
		super.onCreate();
		
		// Initializes Values and Data Structures
		// NOTE:  You don't need all of these, but they are helpful utilities!
		this.httpToolkit	   = new HttpToolkit(this);
		this.gson 		  	   = new Gson();	
		this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		
		// Create Intent Filter and Receiver
		// NOTE:  You can modify this section if you have other intents that you want to listen for
		this.intentReceiver = new ApplicationIntentReceiver();
		this.filter = new IntentFilter();
		this.filter.addAction(GCFService.ACTION_GCF_STARTED);
		this.filter.addAction(AndroidCommManager.ACTION_COMMTHREAD_CONNECTED);
		this.filter.addAction(AndroidCommManager.ACTION_CHANNEL_SUBSCRIBED);
		this.filter.addAction(AndroidGroupContextManager.ACTION_GCF_DATA_RECEIVED);
		this.filter.addAction(BluewaveManager.ACTION_OTHER_USER_CONTEXT_RECEIVED);
		this.registerReceiver(intentReceiver, filter);
		
		// Creates Service (if Not Already Created)
		startGCFService();	
		
		// TODO: Initialize Your App's Data Structures
	}
	
	/**
	 * Creates the GCF Service if it does not already exist
	 */
	private void startGCFService()
	{
		if (gcfService == null)
		{			
			if (DEBUG)
			{
				Toast.makeText(this, "GCF Started (As Background Service)", Toast.LENGTH_SHORT).show();	
			}
			
			// Creates Intent to Start the Service
			Intent i = new Intent(this, GCFService.class);
			i.putExtra("name", "Template App");
			this.bindService(i, gcfServiceConnection, BIND_AUTO_CREATE);
			this.startService(i);
		}
	}
	
	/**
	 * Returns the GCF Service
	 * @return
	 */
	public GCFService getGCFService()
	{
		return gcfService;
	}
	
	/**
	 * Returns the Group Contest Manager
	 * @return
	 */
	public AndroidGroupContextManager getGroupContextManager()
	{
		if (gcfService != null)
		{
			return gcfService.getGroupContextManager();	
		}
		
		return null;
	}
	
	/**
	 * Returns the Bluewave Manager
	 * @return
	 */
	public BluewaveManager getBluewaveManager()
	{
		if (gcfService != null)
		{
			return gcfService.getGroupContextManager().getBluewaveManager();	
		}
		
		return null;
	}
		
    // Preference Methods -----------------------------------------------------------------------	
	public SharedPreferences getSharedPreferences()
	{
		return sharedPreferences;
	}
		
	// Intent Receiver --------------------------------------------------------------------------
	public class ApplicationIntentReceiver extends BroadcastReceiver
	{		
		@Override
		public void onReceive(Context context, Intent intent) 
		{				
			if (intent.getAction().equals(GCFService.ACTION_GCF_STARTED))
			{
				onGCFServiceStarted(context, intent);
			}
			else if (intent.getAction().equals(AndroidCommManager.ACTION_COMMTHREAD_CONNECTED))
			{
				onCommThreadConnected(context, intent);
			}
			else if (intent.getAction().equals(AndroidCommManager.ACTION_CHANNEL_SUBSCRIBED))
			{
				onChannelSubscribed(context, intent);
			}
			else if (intent.getAction().equals(AndroidGroupContextManager.ACTION_GCF_DATA_RECEIVED))
			{
				onContextDataReceived(context, intent);
			}
			else if (intent.getAction().equals(BluewaveManager.ACTION_OTHER_USER_CONTEXT_RECEIVED))
			{
				onOtherUserContextReceived(context, intent);
			}
			else
			{
				Log.e("", "Unexpected Action: " + intent.getAction());
			}
		}
	
		private void onGCFServiceStarted(Context context, Intent intent)
		{				
			if (gcfService != null && gcfService.isReady())
			{
				// Connects to Default DNS Channel and Channels
				defaultConnectionKey = gcfService.getGroupContextManager().connect(COMM_MODE, IP_ADDRESS, PORT);
				
				// Creates Context Providers
				//LocationContextProvider  locationProvider  = new LocationContextProvider(GCFApplication.this, gcfService.getGroupContextManager());
				
				// Registers Context Providers
				//gcfService.getGroupContextManager().registerContextProvider(locationProvider);
				
				// EXPERIMENTAL
				//gcfService.getGroupContextManager().registerArbiter(new ProximityArbiter(gcfService.getGroupContextManager().getBluewaveManager()), ProximityArbiter.PROXIMITY_REQUEST_TYPE);
				//gcfService.getGroupContextManager().sendRequest("LOC", ProximityArbiter.PROXIMITY_REQUEST_TYPE, new String[0], 30000, new String[0]);
			}
		}
		
		private void onCommThreadConnected(Context context, Intent intent)
		{
			String ipAddress = intent.getStringExtra(AndroidCommManager.EXTRA_IP_ADDRESS);
			int    port      = intent.getIntExtra(AndroidCommManager.EXTRA_PORT, -1);
		}
		
		private void onChannelSubscribed(Context context, Intent intent)
		{
			String channel = intent.getStringExtra(AndroidCommManager.EXTRA_CHANNEL);
		}
		
		private void onContextDataReceived(Context context, Intent intent)
		{
			// Extracts the values from the intent
			String   contextType = intent.getStringExtra(ContextData.CONTEXT_TYPE);
			String   deviceID    = intent.getStringExtra(ContextData.DEVICE_ID);
			String[] payload     = intent.getStringArrayExtra(ContextData.PAYLOAD);
			
			System.out.println("Received " + contextType + " from " + deviceID);
		}
		
		private void onOtherUserContextReceived(Context context, Intent intent)
		{
			// This is the Raw JSON from the Device
			String json = intent.getStringExtra(BluewaveManager.EXTRA_OTHER_USER_CONTEXT);
			
			// Creates a Parser
			JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_TEXT, json);
		}
	}

	// This Receiver is Called When the Device First Boots Up
	public static class BootupReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent) 
		{	
			// Do Nothing
		}
	}

}
