package com.adefreitas.gcf.android.arbiters;

import java.util.ArrayList;
import java.util.Locale;

import android.util.Log;

import com.adefreitas.gcf.Arbiter;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.android.bluewave.BluewaveManager;
import com.adefreitas.gcf.messages.ContextCapability;
import com.adefreitas.gcf.messages.ContextRequest;

/**
 * This arbiter uses GCF's Bluewave functionality to determine which devices are in range.
 * The system then only considers these devices when forming a group
 * @author adefreit
 */
public class ProximityArbiter extends Arbiter
{
	// The unique request type associated with this arbiter
	public static final int PROXIMITY_REQUEST_TYPE = 4;
	
	// The amount of time between proximity scans (in ms)
	public static final int SCAN_PERIOD = 30000;

	// Properties
	private BluewaveManager bluewaveManager;
	
	/**
	 * Constructor
	 * @param requestType A unique value (can be any value bigger than 10) that tells GCF
	 * what Context Requests to associate with this arbiter.
	 */
	public ProximityArbiter(BluewaveManager bluewaveManager) 
	{
		super(PROXIMITY_REQUEST_TYPE);
		
		this.bluewaveManager = bluewaveManager;
		
		if (!this.bluewaveManager.isScanning())
		{
			Log.d("Proximity Arbiter", "Starting Scan");
			this.bluewaveManager.startScan(SCAN_PERIOD);
		}
		else
		{
			Log.d("Proximity Arbiter", "Already Scanning . . .");
		}
	}

	/**
	 * Determine what context capabilities (i.e., devices) to subscribe to
	 */
	@Override
	public ArrayList<ContextCapability> selectCapability(
			ContextRequest request, 
			GroupContextManager gcm, 
			ArrayList<ContextCapability> subscribedCapabilities, 
			ArrayList<ContextCapability> receivedCapabilities) 
	{
		ArrayList<ContextCapability> result = new ArrayList<ContextCapability>();
			
		// Step 1:  Populate an Array List with Nearby Devices IDs
		ArrayList<String> nearbyDeviceIDs = new ArrayList<String>();
		for (String deviceID : bluewaveManager.getNearbyDevices(SCAN_PERIOD))
		{
			nearbyDeviceIDs.add(deviceID);
		}
		
		// Step 2:  Keep Subscribing to Devices that are In Proximity AND nearby
		for (ContextCapability capability : subscribedCapabilities)
		{
			if (nearbyDeviceIDs.contains(capability.getDeviceID()) || capability.getDeviceID().equals(gcm.getDeviceID()))
			{
				result.add(capability);
			}
		}
		
		// Step 3:  Keep Subscribing to New Devices that are In Proximity
		for (ContextCapability capability : receivedCapabilities)
		{
			if (nearbyDeviceIDs.contains(capability.getDeviceID()) || capability.getDeviceID().equals(gcm.getDeviceID()))
			{
				result.add(capability);
			}
		}
		
		return result;
	}

}
