package com.adefreitas.gcf.android;

import android.content.ContextWrapper;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.adefreitas.gcf.BatteryMonitor;
import com.adefreitas.gcf.ContextProvider;
import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.android.bluewave.BluewaveManager;
import com.adefreitas.gcf.messages.ContextCapability;
import com.adefreitas.gcf.messages.ContextData;
import com.adefreitas.gcf.messages.ContextRequest;

public class AndroidGroupContextManager extends GroupContextManager
{
	private boolean DEBUG = true;
	
	// Intent Labels
	public static final String ACTION_GCF_DATA_RECEIVED 	= "ACTION_GCF_DATA_RECEIVED";
	public static final String ACTION_PROVIDER_SUBSCRIPTION = "GCF_CONTEXT_PROVIDER_SUBSCRIBED";

	// Deprecated
	public static final String ACTION_GCF_OUTPUT = "ACTION_GCF_OUTPUT";
	public static final String GCF_OUTPUT		 = "GCF_OUTPUT";
	
	// Constants
	private static final int REQUEST_DELAY = 2000;
	
	// Keeps a Link to the Context Wrapper (so we can send intent broadcasts)
	private ContextWrapper cw;
	
	// Bluewave:  Context Sharing via Bluetooth Names
	private BluewaveManager bluewaveManager;
	
	// Handlers (Android Specific)
	private ScheduledTaskHandler scheduledTaskHandler; // Used by the GCM to Performed Scheduled Tasks
	
	/**
	 * Constructor
	 * @param cw
	 * @param deviceID
	 * @param batteryMonitor
	 * @param promiscuous
	 */
	public AndroidGroupContextManager(ContextWrapper cw, String deviceID, BatteryMonitor batteryMonitor, boolean promiscuous)
	{
		// Calls the Parent Class Constructor
		super(deviceID, GroupContextManager.DeviceType.Mobile, batteryMonitor, promiscuous);
						
		// Saves a Link to the Activity (to pass intents)
		this.cw = cw;
		
		// Creates the Comm Manager
		this.commManager = new AndroidCommManager(this, cw);
		
		// Creates the Bluewave Manager
		bluewaveManager = new BluewaveManager(cw, this, "http://gcf.cmu-tbank.com/bluewave/");
		
		// Creates the Scheduled Event Timer	
		scheduledTaskHandler = new ScheduledTaskHandler(this);
	}
	
	public AndroidGroupContextManager(ContextWrapper cw, String deviceID, boolean promiscuous)
	{	
		// Calls the Constructor Above
		this(cw, deviceID, new AndroidBatteryMonitor(cw, deviceID, 5), promiscuous);
	}
		
	public void sendRequest(String contextType, int requestType, String[] deviceIDs, int refreshRate, String[] parameters)
	{
		super.sendRequest(contextType, requestType, deviceIDs, refreshRate, parameters);
		
		// Initiates the Timer Delay
		if (scheduledTaskHandler != null)
		{
			scheduledTaskHandler.start(REQUEST_DELAY);
		}
	}
	
	public void sendRequest(String type, int requestType, int refreshRate, double w_battery, double w_sensorFitness, double w_foreign, double w_providing, double w_reliability, String[] parameters)
	{
		super.sendRequest(type, requestType,  refreshRate,  w_battery, w_sensorFitness, w_foreign, w_providing, w_reliability, parameters, null);
		
		if (scheduledTaskHandler != null)
		{
			scheduledTaskHandler.start(REQUEST_DELAY);
		}
	}
	
	public void cancelRequest(String type)
	{
		super.cancelRequest(type);
		
		if (scheduledTaskHandler != null)
		{
			scheduledTaskHandler.start(0);			
		}
	}
		
	public void setDebug(boolean newDebug)
	{
		DEBUG = newDebug;
	}
	
	public void setDeviceID(String newDeviceID)
	{
		super.setDeviceID(newDeviceID);
	}
	
	// OUTPUT METHODS -------------------------------------------------------------------------------------
	@Override
	protected void onContextDataReceived(ContextData data, ContextRequest request) 
	{
		// Creates the Intent
		Intent dataDeliveryIntent = new Intent(ACTION_GCF_DATA_RECEIVED);
		
		// Populates the Intent
		dataDeliveryIntent.putExtra(ContextData.CONTEXT_TYPE, data.getContextType());
		dataDeliveryIntent.putExtra(ContextData.DEVICE_ID, data.getDeviceID());
		dataDeliveryIntent.putExtra(ContextData.PAYLOAD, data.getPayload());
		
		// Sends the Intent
		cw.sendBroadcast(dataDeliveryIntent);
	}

	@Override
	public void log(String category, String message) 
	{
		if (DEBUG)
		{
			if (category.equalsIgnoreCase(LOG_TIMEOUT) || category.equalsIgnoreCase(LOG_ERROR))
			{
				Log.e(category, message);
			}
			else if (category.equalsIgnoreCase(LOG_PERFORMANCE))
			{
				Log.i(category, message);
			}
			else if (category.equalsIgnoreCase(LOG_COMPARISON))
			{
				Log.i(category, message);
			}
			else
			{
				Log.d(category, message);
			}	
		}
	}

	// BLUEWAVE METHODS
	public BluewaveManager getBluewaveManager()
	{
		return bluewaveManager;
	}
	
	public void startBluewaveScan(int scanInterval)
	{
		bluewaveManager.startScan(scanInterval);
	}
	
	public void stopBluewaveScan()
	{
		bluewaveManager.stopScan();
	}
		
	// CONNECTIVITY METHODS -------------------------------------------------------------------------------
	public boolean isConnectedWiFi()
	{
		ConnectivityManager cm = (ConnectivityManager) cw.getSystemService(cw.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        
        for (NetworkInfo ni : netInfo) 
        {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
            {
            	if (ni.isConnected())
                {
                	return true;
                }
            }
        }
        
        return false;
	}
	
	public boolean isConnectedMobile()
	{
		ConnectivityManager cm = (ConnectivityManager) cw.getSystemService(cw.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        
        for (NetworkInfo ni : netInfo) 
        {
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
            {
            	if (ni.isConnected())
                {
                	return true;
                }
            }
        }
        
        return false;
	}
	
	// PRIVATE METHODS
	private void createNotification(String title, String subtitle)
	{
		boolean success = false;
		
		// Tries to Create a Notification using the OS Tools
//		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) 
//		{
//			//Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//			
//			 Intent 			  intent 			  = new Intent(ACTION_PROVIDER_SUBSCRIPTION);
//			 PendingIntent 		  pendingIntent 	  = PendingIntent.getActivity(cw, 0, intent, 0);
//			 NotificationManager  notificationManager = (NotificationManager)cw.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
//			 Notification.Builder builder 			  = new Notification.Builder(cw)
//			 	.setSmallIcon(com.adefreitas.groupcontextandroidframework.R.drawable.gcf)
//			 	.setContentTitle(title)
//			 	.setContentText(subtitle)
//			 	.setAutoCancel(true)
//			 	//.setSound(soundUri)
//			 	.setContentIntent(pendingIntent);
//			 
//			 if (builder != null && notificationManager != null)
//			 {
//				 if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH)
//				 {
//					 notificationManager.notify(0, builder.getNotification());
//				 }
//				 else
//				 {
//					 notificationManager.notify(0, builder.build());
//				 }
//				 
//				 success = true;
//			 }
//		}
		
		// Creates a Generic Toast if No Notification Can Be Created
		if (!success)
		{
			Toast.makeText(cw, title + ": " + subtitle, Toast.LENGTH_SHORT).show();
		}
	}
	
	private void createSubscriptionNotification()
	{
		if (DEBUG)
		{
			String providerStatus = "";
			
			for (ContextProvider p : this.getRegisteredProviders())
			{
				if (p.getNumSubscriptions() > 0)
				{
					providerStatus += String.format("[%s]: %d  ", p.getContextType(), p.getNumSubscriptions());	
				}
			}
			
			providerStatus = (providerStatus.length() == 0) ? "No Subscriptions" : providerStatus;
			
			createNotification("GCF Subscription Update", providerStatus);
		}
	}

	// GCM Events -----------------------------------------------------------------------------------------
	protected boolean allowCapabilitySubscribe(ContextCapability capability)
	{
		return true;
	}
	
	@Override
	protected void onCapabilityReceived(ContextCapability capability) 
	{
		// TODO Auto-generated method stub	
	}
	
	@Override
	protected void onCapabilitySubscribe(ContextCapability capability) 
	{
		//print("Subscribing to " + capability.getDeviceID() + " [" + capability.getContextType() + "]");
	}

	@Override
	protected void onCapabilityUnsubscribe(ContextCapability capability) 
	{
		//print("Unsubscribing from " + capability.getDeviceID() + " [" + capability.getContextType() + "]");
	}

	protected void onSendingData(ContextData data)
	{
		
	}

	protected void onRequestReceived(ContextRequest request) 
	{
	
	}
	
	@Override
	protected void onRequestTimeout(ContextRequest request, ContextCapability capability) 
	{
		//print("REQ TIMEOUT: " + capability.getDeviceID() + " [" + capability.getContextType() + "]");
	}

	@Override
	protected void onSubscriptionTimeout(ContextSubscriptionInfo subscription) 
	{
		//print("SUB TIMEOUT: " + subscription.getDeviceID());
	}

	@Override
	protected void onProviderSubscribe(ContextProvider provider) 
	{
		String message = "SUB:  Context Provider " + provider.getContextType() + " has " + provider.getNumSubscriptions() + " subscriptions.";
		
		// Creates a Notification to Say How Many Subscriptions are In Progress
		createSubscriptionNotification();
		
		if (scheduledTaskHandler != null)
		{
			scheduledTaskHandler.start(100);
		}
	}

	@Override
	protected void onProviderUnsubscribe(ContextProvider provider) 
	{
		String message = "UNSUB:  Context Provider " + provider.getContextType() + " has " + provider.getNumSubscriptions() + " subscriptions.";
		
		// Creates a Notification to Say How Many Subscriptions are In Progress
		createSubscriptionNotification();
	}
		
	/**
	 * This Class Allows the GCM to Run its Scheduled Tasks
	 * @author adefreit
	 */
	static class ScheduledTaskHandler extends Handler
	{
		private Runnable 				   scheduledTask;
		private AndroidGroupContextManager gcm;
		
		public ScheduledTaskHandler(AndroidGroupContextManager gcm)
		{
			this.gcm 							  = gcm;
			final AndroidGroupContextManager agcm = gcm;
			
			scheduledTask = new Runnable() 
			{
				public void run() 
				{ 	
					// Runs all Scheduled Tasks ONCE
					long delayTime = agcm.runScheduledTasks();
					
					agcm.log("GCM-ScheduledTask", "Next Scheduled Task in " + delayTime + " ms");
					//agcm.print("Next Scheduled Task in " + delayTime + " ms");
					postDelayed(this, delayTime);
				}
			};
		}
		
		public void start(long delayTime)
		{
			// Stops Any Existing Delays
			stop();
			
			// Creates the Next Task Instance
			postDelayed(scheduledTask, delayTime);
		}
		
		public void stop()
		{
			removeCallbacks(scheduledTask);	
		}
	}

}
