package com.adefreitas.gcf;

public class ContextReportingThread extends Thread
{
	private ContextProvider provider;
	private boolean 		keepRunning;
	
	public ContextReportingThread(ContextProvider provider)
	{
		this.provider = provider;
		keepRunning   = true;
	}
	
	public void halt()
	{
		keepRunning = false;
	}
	
	public void run()
	{
		System.out.println("WARNING:  Starting Unique Thread for Provider " + provider.getContextType());
		
		while (keepRunning)
		{
			try
			{
				if (provider.isInUse())
				{
					provider.sendContext();
				}
				
				Thread.sleep(provider.getRefreshRate());
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
	}
}