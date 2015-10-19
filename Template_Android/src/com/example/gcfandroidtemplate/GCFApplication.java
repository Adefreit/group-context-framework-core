package com.example.gcfandroidtemplate;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.FrameworkSettings;
import com.adefreitas.gcf.android.AndroidCommManager;
import com.adefreitas.gcf.android.AndroidGroupContextManager;
import com.adefreitas.gcf.android.bluewave.BluewaveManager;
import com.adefreitas.gcf.android.bluewave.JSONContextParser;
import com.adefreitas.gcf.android.toolkit.HttpToolkit;
import com.adefreitas.gcf.messages.ContextData;
import com.google.gson.Gson;

/**
 * This is a Sample Application Class Used to Abstract GCF's Core Functions
 * 
 * Instructions for Use:
 * 		1.  Change the Package Name to Your Application's Name!
 * 		2.  Make Sure the App Manifest Points to your New Package Name
 * 		3.  Modify app_name in res/values/strings.xml
 * 		4.  Update the COMM_MODE, IP_ADDRESS, and PORT for your particular application
 * 		5.  Clean your project!
 * 
 * @author Adrian de Freitas
 */
public class GCFApplication extends Application
{	
	// Application Constants
	public static final String  LOG_NAME = "ANDROID_TEMPLATE"; 
	public static final boolean DEBUG 	 = true;

	// GCF Connection Settings
	public static final CommMode COMM_MODE  = CommMode.MQTT;
	public static final String   IP_ADDRESS = "IP_ADDRESS_GOES_HERE";
	public static final int      PORT 	    = 1883;
	
	// GCF Variables
	public String 	  		          defaultConnectionKey;
	public AndroidGroupContextManager groupContextManager;
	
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
		this.filter.addAction(AndroidCommManager.ACTION_COMMTHREAD_CONNECTED);
		this.filter.addAction(AndroidCommManager.ACTION_CHANNEL_SUBSCRIBED);
		this.filter.addAction(AndroidGroupContextManager.ACTION_GCF_DATA_RECEIVED);
		this.filter.addAction(BluewaveManager.ACTION_OTHER_USER_CONTEXT_RECEIVED);
		this.registerReceiver(intentReceiver, filter);
		
		// Creates the Group Context Manager
		this.groupContextManager = new AndroidGroupContextManager(this, FrameworkSettings.getDeviceName(android.os.Build.SERIAL), false);
		defaultConnectionKey     = this.groupContextManager.connect(COMM_MODE, IP_ADDRESS, PORT);
		Toast.makeText(this, "GCF Started (Inside Application)", Toast.LENGTH_SHORT).show();
		
		// TODO: Initialize Your App's Data Structures
		
		// TODO: Initialize Your App's Context Providers
	}
	
	/**
	 * Returns the Group Contest Manager
	 * @return
	 */
	public AndroidGroupContextManager getGroupContextManager()
	{
		return groupContextManager;
	}
	
	/**
	 * Returns the Bluewave Manager
	 * @return
	 */
	public BluewaveManager getBluewaveManager()
	{
		return groupContextManager.getBluewaveManager();
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
			if (intent.getAction().equals(AndroidCommManager.ACTION_COMMTHREAD_CONNECTED))
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
			
			Log.d(LOG_NAME, "Received " + contextType + " from " + deviceID);
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
