package com.adefreitas.gcf.android.providers;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONObject;

import android.content.ContentResolver;

import com.adefreitas.gcf.ContextProvider;
import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.android.toolkit.CalendarToolkit;
import com.adefreitas.gcf.messages.CommMessage;
import com.adefreitas.gcf.messages.ComputeInstruction;
import com.adefreitas.gcf.messages.ContextRequest;

/**
 * Accesses a Google Calendar and Provides Appointment Information
 * Needs the Following Permission:  <uses-permission android:name="android.permission.READ_CALENDAR"/>
 * PARAMETERS:  days=X, where x = the number of days to pull calendar events (default is 1)
 * @author adefreit
 */
public class GoogleCalendarProvider extends ContextProvider
{	
	// Context Configuration
	private static final String FRIENDLY_NAME = "Google Calendar";	
	private static final String CONTEXT_TYPE  = "CAL";
	private static final String LOG_NAME      = "GCF-ContextProvider [" + CONTEXT_TYPE + "]";
	
	// Calendar API
	private CalendarToolkit calendarToolkit;
	
	/**
	 * Constructor
	 * @param groupContextManager
	 */
	public GoogleCalendarProvider(GroupContextManager groupContextManager, ContentResolver resolver) 
	{
		super(CONTEXT_TYPE, groupContextManager);
		calendarToolkit = new CalendarToolkit(resolver);
	}
		
	// CONTEXT PROVIDER METHODS ---------------------------------------------------------------------
	@Override
	public void start() 
	{		
		this.getGroupContextManager().log(LOG_NAME, FRIENDLY_NAME + " Provider is Running");
	}

	@Override
	public void stop() 
	{
		this.getGroupContextManager().log(LOG_NAME, FRIENDLY_NAME + " Provider has Stopped");
	}

	@Override
	public double getFitness(String[] parameters) 
	{
		// If the credentials are good, return 1.0;
		return 1.0;
	}

	@Override
	public void sendContext() 
	{				
		Date 				  stopDate   = getStopDate();
		ArrayList<JSONObject> events 	 = calendarToolkit.getCalendarInfoAsJSON(stopDate);
		String[] 			  eventsJSON = new String[events.size()];
		
		this.getGroupContextManager().log(LOG_NAME, eventsJSON.length + " calendar events retrieved.");
		
		try
		{
			for (int i=0; i<events.size(); i++)
			{
				eventsJSON[i] = events.get(i).toString(0);
			}
			
			this.getGroupContextManager().sendContext(getContextType(), new String[0], eventsJSON);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public Date getStopDate()
	{
		int days = 1;
		
		for (ContextSubscriptionInfo i : this.getSubscriptions())
		{
			String num = CommMessage.getValue(i.getParameters(), "days");
			
			if (num != null)
			{
				days = Math.max(days, Integer.valueOf(num));
			}
		}
		
		Calendar cal = Calendar.getInstance();
	    cal.setTime(new Date());
	    cal.add(Calendar.DAY_OF_MONTH, days);
		
		return cal.getTime();
	}
	
	// OVERRIDDING METHODS --------------------------------------------------------------------------
	@Override
	public int getHeartbeatRate(ContextRequest request)
	{
		return 10000;
	}
	
	public void onSubscription(ContextSubscriptionInfo newSubscription)
	{
		sendContext();
	}
	
	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{		
		System.out.println("Received Compute Instruction: " + instruction);
	}
}
