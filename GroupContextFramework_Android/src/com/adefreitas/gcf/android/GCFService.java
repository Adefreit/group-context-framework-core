package com.adefreitas.gcf.android;

import java.util.Date;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.adefreitas.gcf.ContextProvider;
import com.adefreitas.gcf.FrameworkSettings;
import com.adefreitas.gcf.android.bluewave.BluewaveManager;

public class GCFService extends Service
{
	public static final String LOG_NAME = "GCFService [" + (System.currentTimeMillis() % 1000) + "]";
	
	// Preferences
	private SharedPreferences sharedPreferences;
	
	// Intent Variables
	public static final String GCF_WAKELOCK		  = "GCF_WAKELOCK";
	public static final String ACTION_GCF_STARTED = "ACTION_GCF_STARTED";
	
	// Service Variables
	private static Date					       dateStarted;
	private static String 					   deviceID;
	private static AndroidGroupContextManager  gcm;
	private static IBinder					   binder;
	
	// Power Management
	private PowerManager powerManager;
	private WakeLock 	 wakeLock;
	
	/**
	 * Called when the Service is Created
	 */
	@Override
	public void onCreate() 
	{
		Log.d(LOG_NAME, "OnCreate");
	    super.onCreate();
		
	    dateStarted = new Date();
	    binder      = new GCFServiceBinder();
	    
		// Service Preferences
	    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
	}
	
	/**
	 * Called when the Service is Destroyed
	 */
	@Override
	public void onDestroy()
	{
		Log.d(LOG_NAME, "OnDestroy");
		super.onDestroy();
		
		// Releases the Wakelock
		if (wakeLock.isHeld())
		{
			wakeLock.release();
		}
	}
	
	/**
	 * Runs when the GCF Service is Started
	 * NOTE:  This may be called Multiple Times!
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) 
	{    
		if (gcm == null)
		{
			Log.d(LOG_NAME, "OnStartCommand: Creating New GCM");
		
			if (intent != null && intent.hasExtra("name"))
			{
				Log.d(LOG_NAME, "  " + intent.getStringExtra("name"));
			}
			
			// Gets the Device Name (or generates one)
			this.deviceID = FrameworkSettings.getDeviceName(android.os.Build.SERIAL);
			
			// Creates a Battery Monitor
			AndroidBatteryMonitor batteryMonitor = new AndroidBatteryMonitor(this, deviceID, 5);
			
		    // Creates the Group Context Manager
		    gcm = new AndroidGroupContextManager(this, deviceID, batteryMonitor, false);	
		    
		    // Creates the PowerManager
		    powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
		    wakeLock     = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, GCF_WAKELOCK);
		    wakeLock.acquire();
		}
		else
		{
			Log.d(LOG_NAME, "OnStartCommand: Using Existing GCM");
			
			if (intent != null && intent.hasExtra("name"))
			{
				Log.d(LOG_NAME, "  " + intent.getStringExtra("name"));
			}
		}
		
		// Creates an Intent to Let Everyone Know the Service has Started
		Intent startIntent = new Intent(ACTION_GCF_STARTED);
		this.sendBroadcast(startIntent);
		
	    // TODO Auto-generated method stub
	    return START_STICKY;
	}
	
	/**
	 * Called when Android Tries to Turn off this Service
	 */
	@Override
	public void onTaskRemoved(Intent intent) 
	{
		Log.d(LOG_NAME, "OnTaskRemoved");
				
		if (intent.hasExtra("name"))
		{
			Log.d(LOG_NAME, "  " + intent.getStringExtra("name"));
		}
		
		if (gcm != null)
		{
			for (ContextProvider p : gcm.getRegisteredProviders())
			{
				p.reboot();
			}	
		}
		
		// Releases the Wakelock
		if (wakeLock != null && wakeLock.isHeld())
		{
			wakeLock.release();
		}
		
		// Restarts the GCF Service
		Log.d(LOG_NAME, "Restarting Service");
		
		// Generates the Intent to Restart the Service
		Intent restartService = new Intent(getApplicationContext(), this.getClass());
	    restartService.setPackage(getPackageName());
	    PendingIntent restartServicePI = PendingIntent.getService(getApplicationContext(), 1, restartService, PendingIntent.FLAG_ONE_SHOT);

        //Restart the service once it has been killed by android
        AlarmManager alarmService = (AlarmManager)getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 100, restartServicePI);	
	}
	
	/**
	 * Called when an App Binds to this Service
	 */
	@Override
	public IBinder onBind(Intent intent) 
	{
		Log.d(LOG_NAME, "OnBind");
		
		if (intent.hasExtra("name"))
		{
			Log.d(LOG_NAME, "  " + intent.getStringExtra("name"));
		}
				
		return binder;
	}

	// Custom Methods -----------------------------------------------------------------------------------
	/**
	 * Returns TRUE if GCF Objects are Instantiated; FALSE otherwise
	 * @return
	 */
	public Boolean isReady()
	{
		return gcm != null;
	}

	/**
	 * Returns a Unique String Representing this Service
	 * @return
	 */
	public String getServiceID()
	{
		return "GCF_SERVICE_" + (dateStarted.getTime() % 1000);
	}

	/**
	 * Returns the Date this Service Started
	 * @return
	 */
	public Date getDateStarted()
	{
		return this.dateStarted;
	}
	
	/**
	 * Returns the Group Context Manager (Main GCF Object)
	 * @return
	 */
	public AndroidGroupContextManager getGroupContextManager()
	{
		return gcm;
	}

	/**
	 * Returns the Bluewave Manager (Used for Bluetooth Context Scanning)
	 * @return
	 */
	public BluewaveManager getBluewaveManager()
	{
		if (isReady())
		{
			return gcm.getBluewaveManager();
		}
		else
		{
			return null;
		}
	}

	 /**
     * Allows Applications to Bind to this Service
     */
    public class GCFServiceBinder extends Binder 
    {
        public GCFService getService() 
        {
            // Return this instance of LocalService so clients can call public methods
            return GCFService.this;
        }
    }
    
}
