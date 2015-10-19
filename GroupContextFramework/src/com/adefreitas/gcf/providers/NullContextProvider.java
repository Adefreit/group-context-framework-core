package com.adefreitas.gcf.providers;

import com.adefreitas.gcf.ContextProvider;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.messages.ContextRequest;

/**
 * This is a Null Context Provider.  It Does Nothing Except Assume the Context Type you Tell It To
 * Only use when you need to make it "look" like a device is hosting a context
 * @author adefreit
 */
public class NullContextProvider extends ContextProvider
{
	// Context Configuration	
	private static String CONTEXT_TYPE = "NULL";
	private static String LOG_NAME     = "NULL";
	
	public NullContextProvider(String contextType, GroupContextManager groupContextManager) 
	{
		super(contextType, groupContextManager);
		CONTEXT_TYPE = contextType;
		LOG_NAME     = "GCF-ContextProvider [" + CONTEXT_TYPE + "]";
	}

	@Override
	public void start() 
	{
		this.getGroupContextManager().log(LOG_NAME, this.getContextType() + " Provider Started");
	}

	@Override
	public void stop() 
	{
		this.getGroupContextManager().log(LOG_NAME, this.getContextType() + " Provider Stopped");
	}

	public boolean sendCapability(ContextRequest request)
	{
		return false;
	}
	
	@Override
	public double getFitness(String[] parameters) 
	{
		return 0.0;
	}

	@Override
	public void sendContext() 
	{
		// Do Nothing
	}
}
