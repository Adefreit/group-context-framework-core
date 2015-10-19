package com.adefreitas.gcf.arbiters;

import java.util.ArrayList;
import java.util.HashMap;

import com.adefreitas.gcf.Arbiter;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.messages.ContextCapability;
import com.adefreitas.gcf.messages.ContextRequest;

/**
 * This arbiter will only select specific devices for a specific context(s)
 * @author adefreit
 *
 */
public class SpecificSourceArbiter extends Arbiter
{
	// Tracks Which Device
	private HashMap<String, ArrayList<String>> deviceList;
	
	/**
	 * Constructor
	 * @param requestType
	 */
	public SpecificSourceArbiter(int requestType) 
	{
		super(requestType);
		
		// Initializes the Data Structure
		this.deviceList = new HashMap<String, ArrayList<String>>();
	}

	/**
	 * Adds a Specific Device and Context Type
	 * @param deviceID
	 * @param contextType
	 */
	public void addDevice(String deviceID, String contextType)
	{
		// Creates an Entry for the Device if it Doesn't Already Exist
		if (!deviceList.containsKey(deviceID))
		{
			deviceList.put(deviceID, new ArrayList<String>());
		}
		
		// Adds the Entry for the Context Type
		if (!deviceList.get(deviceID).contains(contextType))
		{
			deviceList.get(deviceID).add(contextType);
		}
	}
	
	/**
	 * Removes A Specific Device (and all Contexts)
	 * @param deviceID
	 */
	public void removeDevice(String deviceID)
	{
		deviceList.remove(deviceID);
	}
	
	/**
	 * Removes a Specific Context from a Specific Device
	 * @param deviceID
	 * @param contextType
	 */
	public void removeDevice(String deviceID, String contextType)
	{
		if (deviceList.containsKey(deviceID))
		{
			deviceList.get(deviceID).remove(contextType);
		}
	}
	
	/**
	 * Selects a Capability from the List
	 */
	@Override
	public ArrayList<ContextCapability> selectCapability(
			ContextRequest request, 
			GroupContextManager gcm,
			ArrayList<ContextCapability> subscribedCapabilities,
			ArrayList<ContextCapability> receivedCapabilities) 
	{
		ArrayList<ContextCapability> result = new ArrayList<ContextCapability>();
		
		// Adds Newer Entries that Have Not Been Subscribed
		for (ContextCapability receivedCapability : receivedCapabilities)
		{	
			System.out.println("*** SPECIFIC_SOURCE LOOKING AT: " + receivedCapability.getDeviceID() + " for context " + request.getContextType() + " ***");
			
			ContextCapability oldEntry = null;
			
			// Removes Older Subscription Capabilities with Newer Versions
			for (ContextCapability subscribedCapability : subscribedCapabilities)
			{
				if (subscribedCapability.getDeviceID().equals(receivedCapability.getDeviceID()))
				{
					oldEntry = subscribedCapability;
					break;
				}
			}
	
			if (oldEntry != null)
			{
				subscribedCapabilities.remove(oldEntry);
			}
			
			if (deviceList.containsKey(receivedCapability.getDeviceID()))
			{
				if (deviceList.get(receivedCapability.getDeviceID()).contains(receivedCapability.getContextType()))
				{
					result.add(receivedCapability);
				}
			}
		}
		
		// Keeps Older Entries that are Already Subscribed
		for (ContextCapability subscribedCapability : subscribedCapabilities)
		{
			result.add(subscribedCapability);
		}
				
		return result;
	}

}
