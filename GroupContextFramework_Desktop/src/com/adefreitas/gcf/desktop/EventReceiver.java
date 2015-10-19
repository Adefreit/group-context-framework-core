package com.adefreitas.gcf.desktop;

import com.adefreitas.gcf.messages.ContextCapability;
import com.adefreitas.gcf.messages.ContextData;

/**
 * Interface so that Applications Can Receive Context
 * @author adefreit
 */
public interface EventReceiver
{
	public void onContextData(ContextData data);
	
	public void onCapabilitySubscribe(ContextCapability capability);
	
	public void onCapabilityUnsubscribe(ContextCapability capability);
}