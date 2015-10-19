package com.adefreitas.gcf;

import java.util.ArrayList;

import com.adefreitas.gcf.messages.ContextCapability;
import com.adefreitas.gcf.messages.ContextRequest;

public abstract class Arbiter 
{
	private int requestType;
	
	public Arbiter(int requestType)
	{
		this.requestType = requestType;
	}
	
	public int getRequestType()
	{
		return requestType;
	}
	
	// ABSTRACT METHOD ------------------------------------------------------------------
	public abstract ArrayList<ContextCapability> selectCapability(
			ContextRequest request, 
			GroupContextManager gcm, 
			ArrayList<ContextCapability> subscribedCapabilities, 
			ArrayList<ContextCapability> receivedCapabilities);
}
