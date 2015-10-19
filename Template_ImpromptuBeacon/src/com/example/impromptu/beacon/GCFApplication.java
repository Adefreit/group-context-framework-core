package com.example.impromptu.beacon;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

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
import com.adefreitas.gcf.ContextProvider;
import com.adefreitas.gcf.FrameworkSettings;
import com.adefreitas.gcf.android.AndroidCommManager;
import com.adefreitas.gcf.android.AndroidGroupContextManager;
import com.adefreitas.gcf.android.bluewave.BluewaveManager;
import com.adefreitas.gcf.android.bluewave.JSONContextParser;
import com.adefreitas.gcf.android.impromptu.AndroidApplicationProvider;
import com.adefreitas.gcf.android.toolkit.HttpToolkit;
import com.adefreitas.gcf.impromptu.ApplicationProvider;
import com.adefreitas.gcf.impromptu.ApplicationSettings;
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
public class GCFApplication extends Application
{	
	// Application Constants
	public static final String  LOG_NAME 				= "IMPROMPTU_BEACON_TEMPLATE"; 
	public static final boolean DEBUG 	 				= true;
	public static final int     BLUEWAVE_UPDATE_SECONDS = 60;
	public static final int     SCAN_PERIOD_IN_SECONDS  = 30;
	public static final boolean BLUETOOTH_DISCOVERABLE  = true;

	// GCF Connection Settings
	public static final String 	 DEV_NAME   = FrameworkSettings.getDeviceName(android.os.Build.SERIAL);
	public static final CommMode COMM_MODE  = CommMode.MQTT;
	public static final String   IP_ADDRESS = "IP_ADDRESS_GOES_HERE";
	public static final int      PORT 	    = 1883;
	
	// GCF Bluewave App ID:  This Value is Obtained from the GCF Site
	public static final String BLUEWAVE_APP_ID = "BEACON_TEMPLATE";
	
	// GCF Bluewave Contexts:  This is the list of the JSON tags 
	public static final String[] BLUEWAVE_CONTEXTS = new String[] { "device", "identity" };
	
	// GCF Variables
	public String 	  		          defaultConnectionKey;
	public AndroidGroupContextManager groupContextManager;
	
	// Application Preferences
	private SharedPreferences sharedPreferences;
		
	// Cloud Storage Settings
	private HttpToolkit httpToolkit;
	
	// Object Serialization Tool
	private Gson gson;
	
	// Tracks Devices Detected Thus Far (Key = Device ID)
	private HashMap<String, String> log;
	
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
				
		// Creates the Group Context Manager, which is Responsible for Context Producing and Sharing
		groupContextManager = new AndroidGroupContextManager(this, DEV_NAME, false);
		Toast.makeText(this, "Beacon Started", Toast.LENGTH_SHORT).show();
		
		// Connects to Default DNS Channel and Channels
		defaultConnectionKey = groupContextManager.connect(COMM_MODE, IP_ADDRESS, PORT);

		// Initializes the Log
		this.log = new HashMap<String, String>();
		
		// Initializes Context Providers
		configureContextProviders();
		configureAppProviders();
		
		// Initializes Bluewave
		groupContextManager.getBluewaveManager().setDiscoverable(BLUETOOTH_DISCOVERABLE);
		groupContextManager.startBluewaveScan(SCAN_PERIOD_IN_SECONDS * 1000);
		groupContextManager.getBluewaveManager().receiveContext(BLUEWAVE_APP_ID, BLUEWAVE_CONTEXTS);
		
		// Create Intent Filter and Receiver
		// NOTE:  You can modify this section if you have other intents that you want to listen for
		this.intentReceiver = new ApplicationIntentReceiver();
		this.filter = new IntentFilter();
		this.filter.addAction(AndroidCommManager.ACTION_COMMTHREAD_CONNECTED);
		this.filter.addAction(AndroidCommManager.ACTION_CHANNEL_SUBSCRIBED);
		this.filter.addAction(AndroidGroupContextManager.ACTION_GCF_DATA_RECEIVED);
		this.filter.addAction(BluewaveManager.ACTION_OTHER_USER_CONTEXT_RECEIVED);
		this.registerReceiver(intentReceiver, filter);
				
		// TODO: Initialize Your App's Data Structures
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
	
    /**
     * Retrieves the Application's Shared Preferences	
     * @return
     */
	public SharedPreferences getSharedPreferences()
	{
		return sharedPreferences;
	}
	
	public String getLogEntry(String deviceID)
	{
		if (log.containsKey(deviceID))
		{
			return log.get(deviceID);
		}
		else  
		{
			return "Non-GCF Bluetooth Device";
		}
	}
	
	/**
	 * INITIALIZATION METHOD:  Configures Context Providers (i.e., Sensors) to the Beacon
	 */
	private void configureContextProviders()
	{
		// TODO: Add GCFContext Providers (If Needed)
	}
	
	/**
	 * INITIALIZATION METHOD: Adds Applications (i.e., "services") to the Beacon
	 */
	private void configureAppProviders()
	{
		// Creates a List of Apps
		ArrayList<ApplicationProvider> apps = new ArrayList<ApplicationProvider>();
		
		// Creates Application Providers
		apps.add(new SampleImpromptuApp(groupContextManager, COMM_MODE, IP_ADDRESS, PORT));
		
		// Listens on the Channel for these Applications
		for (ApplicationProvider app : apps)
		{
			this.getGroupContextManager().registerContextProvider(app);
			this.getGroupContextManager().subscribe(this.defaultConnectionKey, app.getContextType());
		}
		
		// Debug Message
		Log.d(LOG_NAME, apps.size() + " app(s) loaded.");
	}
			
	/**
	 * This method allows all installed beacon apps to see the Bluewave context
	 * @param parser
	 */
	private void analyzeBluewaveContext(JSONContextParser parser)
	{
		// Attempts to Extract Connection Information
		JSONObject context = parser.getJSONObject("identity");
		
		if (context != null)
		{
			String deviceID = parser.getDeviceID();
			
			try
			{
				CommMode  commMode   = CommMode.valueOf(context.getString("COMM_MODE")); 
				String    ipAddress  = context.getString("IP_ADDRESS");
				int       port       = context.getInt("PORT");
				
				String result       = "";
				String allValidApps = "";
				
				if (ipAddress != null)
				{	
					ArrayList<String> appPayload = new ArrayList<String>();
					
					for (ContextProvider p : groupContextManager.getRegisteredProviders())
					{	
						if (p instanceof AndroidApplicationProvider)
						{
							AndroidApplicationProvider appProvider = (AndroidApplicationProvider)p;
							
							// Determines whether the App Provider Wants to Share this Application
							boolean redundant = isRedundant(appProvider, context);
							boolean sendData  = appProvider.sendAppData(parser.toString());
							
							//Toast.makeText(this, appProvider.getContextType() + ": [" + deviceID + "] redundant: " + redundant + "; sendData: " + sendData, Toast.LENGTH_LONG).show();
							
							if (!redundant && sendData)
							{
								result += appProvider.getContextType() + " ";
								
								appPayload.add(gson.toJson(appProvider.getInformation(parser.toString())));
							}
							
							if (sendData)
							{
								allValidApps += appProvider.getContextType() + " ";
							}
						}
					}
					
					// Transmits All of the Payloads at Once
					if (appPayload.size() > 0)
					{
						String connectionKey = "";
						
						// Either Connects, or Uses the Existing Connection
						if (!groupContextManager.isConnected(commMode, ipAddress, port))
						{
							connectionKey = groupContextManager.connect(commMode, ipAddress, port);
						}
						else
						{
							connectionKey = groupContextManager.getConnectionKey(commMode, ipAddress, port);
						}
						
						// Sends ONE Message with Everything :)
						groupContextManager.sendComputeInstruction(
								connectionKey, 
								ApplicationSettings.DNS_CHANNEL, 
								ApplicationSettings.DNS_CONTEXT_TYPE, 
								new String[] { "LOS_DNS" }, 
								"SEND_ADVERTISEMENT", 
								new String[] { "DESTINATION=" + deviceID, "APPS=" + gson.toJson(appPayload.toArray(new String[0])) });
					}
					
					log.put(deviceID, "Detected: " + new Date().toString() + "\nValid Apps: " + allValidApps.trim());
				}
			}
			catch (Exception ex)
			{
				log.put(deviceID, "Detected: " + new Date().toString() + "\nERROR: " + ex.getMessage());
			}	
		}
	}
	
	/**
	 * This method is used to determine if an app has already been sent to a device
	 * @param appProvider
	 * @param context
	 * @return
	 */
	private boolean isRedundant(AndroidApplicationProvider appProvider, JSONObject context)
	{
		if (context.has("APPS"))
		{
			try
			{
				JSONArray apps = context.getJSONArray("APPS");
				
				for (int i=0; i<apps.length(); i++)
				{			
					JSONObject appObject = apps.getJSONObject(i);
					
					if (appObject.getString("name").equals(appProvider.getContextType()))
					{
						if (appObject.has("expiring"))
						{
							return !appObject.getBoolean("expiring");
						}
						else
						{
							return false;
						}
					}
				}
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
			
			return false;
		}
		
		return true;
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
			
			System.out.println("Received " + contextType + " from " + deviceID);
		}
		
		private void onOtherUserContextReceived(Context context, Intent intent)
		{			
			// This is the Raw JSON from the Device
			String json = intent.getStringExtra(BluewaveManager.EXTRA_OTHER_USER_CONTEXT);
			
			// Creates a Parser
			JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_TEXT, json);
			
			// Analyzes the Context to See if the Beacon Should Advertise its Services
			analyzeBluewaveContext(parser);
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
