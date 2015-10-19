package com.adefreitas.gcf;
/**
 * This Data Structure Tracks Context Subscriptions from all Subscribed Devices
 */


import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class ContextReliabilityInfo 
{
	private String  			     		 deviceID;
	private HashMap<String, ArrayList<Date>> timeoutHistory;
	private HashMap<String, ArrayList<Date>> updateHistory;
	private HashMap<String, Date>			 startDates;
	private final int 						 HISTORY_LENGTH = 600000;
	
 	public ContextReliabilityInfo(String deviceID)
	{
		this.deviceID 		= deviceID;
		this.timeoutHistory = new HashMap<String, ArrayList<Date>>();
		this.updateHistory  = new HashMap<String, ArrayList<Date>>();
		this.startDates 	= new HashMap<String, Date>();
	}
	
	public String getDeviceID()
	{
		return deviceID;
	}

	public int getNumTimeouts()
	{
		int total = 0;
		
		for (String type : timeoutHistory.keySet())
		{
			total += getNumTimeouts(type);
		}
		
		return total;
	}
	
	public int getNumTimeouts(String contextType)
	{
		int count = 0;
		
		if (timeoutHistory.containsKey(contextType))
		{
			Date currentDate = new Date();
			
			for (Date timeoutDate : new ArrayList<Date>(timeoutHistory.get(contextType)))
			{
				if (currentDate.getTime() - timeoutDate.getTime() > HISTORY_LENGTH)
				{
					timeoutHistory.remove(timeoutDate);
				}
				else
				{
					count++;
				}
			}
			
			return count;
		}
		else
		{
			return 0;
		}
	}
	
	public int getNumUpdates()
	{
		int total = 0;
		
		for (String type : updateHistory.keySet())
		{
			total += getNumUpdates(type);
		}
		
		return total;
	}
	
	public int getNumUpdates(String contextType)
	{
		int count = 0;
		
		if (updateHistory.containsKey(contextType))
		{
			Date currentDate = new Date();
			
			for (Date timeoutDate : new ArrayList<Date>(updateHistory.get(contextType)))
			{
				if (currentDate.getTime() - timeoutDate.getTime() > HISTORY_LENGTH)
				{
					timeoutHistory.remove(timeoutDate);
				}
				else
				{
					count++;
				}
			}
			
			return count;
		}
		else
		{
			return 0;
		}
	}
	
	public Date getLastContact(String contextType)
	{
		Date result = new Date(0);
		
		// Looks to See if the Device has Ever Sent Data
		if (updateHistory.containsKey(contextType))
		{
			ArrayList<Date> updateDates = updateHistory.get(contextType);
			result = updateDates.get(updateDates.size() - 1);
		}
		
		// Looks to See if the Device has Subscribed Yet
		if (startDates.containsKey(contextType))
		{
			Date startDate = startDates.get(contextType);
			result = (startDate.getTime() > result.getTime()) ? startDate : result;
		}
		
		return result;
	}
	
	public double getReliabilityRatio(String contextType)
	{
		int timeouts = getNumTimeouts(contextType);
		//int updates  = getNumUpdates(contextType);
		
		double ratio = 1.0 - Math.pow((double)timeouts / 5.0, 2.0);
		
		if (ratio < 0.0)
		{
			return 0.0;
		}
		else if (ratio > 1.0)
		{
			return 1.0;
		}
		else
		{
			return ratio;
		}
	}

	public void addDataUpdate(String contextType)
	{	
		if (!updateHistory.containsKey(contextType))
		{
			updateHistory.put(contextType, new ArrayList<Date>());	
		}
		
		ArrayList<Date> history = updateHistory.get(contextType);
		
		// Adds the Entry to the List
		history.add(new Date());
	
		// Adds 
		while (history.size() > 5)
		{
			history.remove(0);
		}
	}
	
	public void addContextTimeout(String contextType)
	{
		if (!timeoutHistory.containsKey(contextType))
		{
			timeoutHistory.put(contextType, new ArrayList<Date>());	
		}
		
		timeoutHistory.get(contextType).add(new Date());
	}
	
	public void setSubscriptionStartDate(String contextType)
	{
		if (startDates.containsKey(contextType))
		{
			startDates.remove(contextType);
		}
		
		startDates.put(contextType, new Date());
	}
}
