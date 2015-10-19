package com.adefreitas.gcf.android;

import java.util.Date;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.adefreitas.gcf.BatteryMonitor;

public class AndroidBatteryMonitor extends BatteryMonitor
{	
	
	private Handler 	   parentActivityHandler;
	private ContextWrapper cw;
	private Date 		   lastChanged;
	
	// Android Mechanism for Listening to Battery Information
	private BatteryBroadcastReceiver batteryInfoReceiver;
	
	
	int level;
    int plugged;
    int scale;
	
	/**
	 * Constructor
	 * @param a - the Activity that created this object
	 * @param batteryCapacity - the capacity of the battery in mAh (milliamp hours)
	 */
	public AndroidBatteryMonitor(ContextWrapper cw, String deviceID, int maxBatteryHistorySize)
	{
		super(maxBatteryHistorySize);
		
		batteryInfoReceiver   = new BatteryBroadcastReceiver(this);
		parentActivityHandler = null;
		this.cw 			  = cw;
		
		// Allows the GCM to Have Access to Battery Data
		cw.registerReceiver(batteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
	}
		
	public void close()
	{
		cw.unregisterReceiver(batteryInfoReceiver);
	}
	
	class BatteryBroadcastReceiver extends BroadcastReceiver
	{
		AndroidBatteryMonitor batteryMonitor;
		
		public BatteryBroadcastReceiver(AndroidBatteryMonitor batteryMonitor)
		{
			this.batteryMonitor = batteryMonitor;
		}
		
		public void onReceive(Context context, Intent intent) 
        {
			// Calculates Battery Stats
			level	= intent.getIntExtra(BatteryManager.EXTRA_LEVEL,0);
            plugged	= intent.getIntExtra(BatteryManager.EXTRA_PLUGGED,0);
            scale	= intent.getIntExtra(BatteryManager.EXTRA_SCALE,0);

            // Extraneous Battery Options that COULD Be Downloaded
			//int     health	    = intent.getIntExtra(BatteryManager.EXTRA_HEALTH,0);
            //int     icon_small  = intent.getIntExtra(BatteryManager.EXTRA_ICON_SMALL,0);
            //boolean present     = intent.getExtras().getBoolean(BatteryManager.EXTRA_PRESENT); 
            //int     status	    = intent.getIntExtra(BatteryManager.EXTRA_STATUS,0);
            //String  technology  = intent.getExtras().getString(BatteryManager.EXTRA_TECHNOLOGY);
            //int     temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0);
            //int     voltage	    = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE,0);
            
            // Determines the New Battery Percentage
            double newBatteryPercent = 100.0 * ((double)level / (double)scale);
            
            // Updates the Battery Rating iff it Actually Changed
            if (newBatteryPercent != getBatteryPercent())
            {           	           	
            	// Calculates Battery Life Remaining in Minutes
            	double batteryTimeRemaining = getBatteryTimeRemaining() / 1000.0 / 60.0;
            	
            	// Creates an Entry Describing this Battery Information
            	batteryMonitor.addBatteryInfo(newBatteryPercent, batteryTimeRemaining, plugged > 0);

            	String message = getBatteryPercent() + "% (" + batteryTimeRemaining + " min remaining)  Last Change:  ";
            	message += (lastChanged == null) ? "0 ms" : new Date().getTime() - lastChanged.getTime() + " ms";
//            	System.out.println(message);
//            	lastChanged = new Date();
            	
            	Log.d("GCM-Battery", message);
            	//FileHelper.writeToFile(activity, "/GroupContextManager/", filename, batteryMonitor.getMostRecentBatteryInfo().toString());
            	
            	// Delivers the Battery Information to the Main Application
            	if (parentActivityHandler != null)
            	{
                	Message m = Message.obtain();
            		m.obj     = message;
            		parentActivityHandler.sendMessage(m);
            	}
            }
        }	
	}

	
	@Override
	public boolean isCharging() 
	{
		return plugged != 0;
	}	
}
