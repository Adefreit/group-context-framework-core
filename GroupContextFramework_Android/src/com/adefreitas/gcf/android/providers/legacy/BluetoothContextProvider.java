package com.adefreitas.gcf.android.providers.legacy;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.adefreitas.gcf.ContextProvider;
import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.messages.CommMessage;

/**
 * This is a template for a context provider.
 * COPY AND PASTE; NEVER USE
 * @author adefreit
 */
public class BluetoothContextProvider extends ContextProvider
{
	// Context Configuration
	private static final String FRIENDLY_NAME = "BLUETOOTH";	
	private static final String CONTEXT_TYPE  = "BLU";
	private static final String LOG_NAME      = "GCF-ContextProvider [" + CONTEXT_TYPE + "]";
	private static final long   MAX_TIME 	  = 30000;
	
	// Intent Variables
	public  static final String ACTION_BLUETOOTH_SCAN_UPDATE = "BT_UPDATE";
	public  static final String BLUETOOTH_SCAN_RESULTS       = "BT_RESULTS";
	
	// Provider Variables
	private Context 		 	  context;
	private BluetoothAdapter 	  bluetooth;
	private boolean 		 	  scanning;
	private HashMap<String, Date> deviceLog;
	
	// Behavior Variables
	private Date			  scanStart;
	private ArrayList<String> hitList;
	
	public BluetoothContextProvider(Context context, GroupContextManager groupContextManager) 
	{
		super(CONTEXT_TYPE, groupContextManager);
		
		// Initializes Variables
		this.context   = context;
		this.bluetooth = BluetoothAdapter.getDefaultAdapter();
		this.scanning  = false;
		this.hitList   = new ArrayList<String>();
		this.scanStart = new Date(0);
		
		// Sets Filter for When Bluetooth Devices are Found
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		context.registerReceiver(mReceiver, filter);
		
		// Allows Bluetooth Adapter to be in Discoverable mode
		if (bluetooth.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)
		{
			Intent discoverable = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverable.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0); // Set to 0 for infinite, Max is 1200 (I think)
			discoverable.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			this.context.startActivity(discoverable);			
		}

		deviceLog = new HashMap<String, Date>();
	}

	@Override
	public void start() 
	{		
		if(bluetooth != null)
		{
			deviceLog.clear();

			if (!bluetooth.isDiscovering())
			{
				bluetooth.startDiscovery();
				scanning = true;
				scanStart = new Date();
				this.getGroupContextManager().log(LOG_NAME, this.getContextType() + " Provider Started");
			}
			else
			{
				this.getGroupContextManager().log(LOG_NAME, this.getContextType() + " Provider Already Started");
			}
		}
	}

	@Override
	public void stop() 
	{
		if (bluetooth != null)
		{
			if (bluetooth.isDiscovering())
			{
				bluetooth.cancelDiscovery();
				scanning = false;
				this.getGroupContextManager().log(LOG_NAME, this.getContextType() + " Provider Stopped");			
			}
			else
			{
				this.getGroupContextManager().log(LOG_NAME, this.getContextType() + " Provider Already Stopped");
			}
		}
		
		
	}

	@Override
	public double getFitness(String[] parameters) 
	{
		return 1.0;
	}

	@Override
	public void sendContext() 
	{
		this.getGroupContextManager().sendContext(this.getContextType(), new String[0], new String[] {
			"SCAN_START=" + scanStart.getTime(), 
			"DEVICES=" + CommMessage.toCommaString(deviceLog.keySet().toArray(new String[0]))});
	}

	// Private Methods
	private void removeOldEntries()
	{
		// Check for any device IDs that are expired
        for (String deviceID : new ArrayList<String>(deviceLog.keySet()))
        {
        	Date lastContact = deviceLog.get(deviceID);
        	
        	if (new Date().getTime() - lastContact.getTime() > MAX_TIME)
        	{
        		deviceLog.remove(deviceID);
        	}
        }
	}
	
	private void updateHitList()
	{
		hitList.clear();
		
		for (ContextSubscriptionInfo subscription : this.getSubscriptions())
		{
			for (String entry : CommMessage.getValues(subscription.getParameters(), "HITLIST"))
			{
				if (!hitList.contains(entry))
				{
					hitList.add(entry);
				}
			}
		}
	}
	
	// Create a BroadcastReceiver for ACTION_FOUND
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() 
	{
		private void onBluetoothDeviceFound(Context context, Intent intent)
		{
            // Get the BluetoothDevice object from the Intent
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            // Cleanup Old Entries
            removeOldEntries();
            
            // Updates Hit List
            updateHitList();
            
            // Adds the Device to the Log (with a new date of last contact)
            if (!deviceLog.containsKey(device.getName()))
            {
            	deviceLog.put(device.getName(), new Date());
            }

            // Sends an Updated Message
            if (hitList.contains(device.getName()))
            {
            	getGroupContextManager().sendContext(getContextType(), new String[0], new String[] { 
            		"FOUND=" + device.getName(), 
            		"SCAN_START=" + scanStart.getTime(), 
            		"TIMESTAMP=" + new Date().getTime(),
            		"DEVICES=" + CommMessage.toCommaString(deviceLog.keySet().toArray(new String[0]))
            		});
            }
    			            
            // Add the name and address to an array adapter to show in a ListView
            Log.i(LOG_NAME, "Found " + device.getName() + ":" + device.getAddress());
		}
		
		private void onBluetoothDiscoveryFinished(Context context, Intent intent)
		{
        	// Restarts the Discovery to keep it continuous
        	if (scanning)
        	{
        		if (!bluetooth.isDiscovering())
        		{
        			Log.i(LOG_NAME, "Restarting Scan.");
            		bluetooth.startDiscovery();
            		scanStart = new Date();	
        		}
        	}
		}
		
	    public void onReceive(Context context, Intent intent) 
	    {
	        String action = intent.getAction();
	        
	        // When discovery finds a device
	        if (BluetoothDevice.ACTION_FOUND.equals(action)) 
	        {	        	
	        	onBluetoothDeviceFound(context, intent);
	        }
	        else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
	        {
	        	onBluetoothDiscoveryFinished(context, intent);
	        }
	    }
	};
}
