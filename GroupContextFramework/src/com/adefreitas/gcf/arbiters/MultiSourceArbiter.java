package com.adefreitas.gcf.arbiters;

import java.util.ArrayList;

import com.adefreitas.gcf.Arbiter;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.messages.ContextCapability;
import com.adefreitas.gcf.messages.ContextRequest;

public class MultiSourceArbiter extends Arbiter
{

	public MultiSourceArbiter(int requestType) 
	{
		super(requestType);
	}

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
			ContextCapability oldEntry = null;
			
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
				//gcm.log("GCM-Request", "  Deleteing Old " + receivedCapability.getDeviceID());
			}
			
			result.add(receivedCapability);
		}
		
		// Keeps Older Entries that are Already Subscribed
		for (ContextCapability subscribedCapability : subscribedCapabilities)
		{
			//gcm.log("GCM-Request", "Subscribed Capability: " + subscribedCapability.getDeviceID());
			result.add(subscribedCapability);
		}
				
		return result;
	}

}
