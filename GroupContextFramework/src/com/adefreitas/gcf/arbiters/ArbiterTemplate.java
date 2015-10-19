package com.adefreitas.gcf.arbiters;

import java.util.ArrayList;

import com.adefreitas.gcf.Arbiter;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.messages.ContextCapability;
import com.adefreitas.gcf.messages.ContextRequest;

/**
 * An arbiter is a judge that determines which devices to group with.  The core GCF framework comes with 3 built in arbiters:
 *      - Single Source:  Groups with the "best" device that provides this context
 *      - Multi  Source:  Groups with ALL devices that provide this context
 *      - Local  Source:  Groups with context providers on the same device that made the request
 * 
 * To make your own context provider, copy this code into your project
 * 
 * @author adefreit
 *
 */
public class ArbiterTemplate extends Arbiter
{

	/**
	 * Constructor
	 * @param requestType A unique value (can be any value bigger than 10) that tells GCF
	 * what Context Requests to associate with this arbiter.
	 */
	public ArbiterTemplate(int requestType) 
	{
		super(requestType);
		
		if (requestType >= 0 && requestType <= 10)
		{
			System.err.println("WARNING: This arbiter's requestType may conflict or override a core GCF arbiter.");
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
				
		return result;
	}

}
