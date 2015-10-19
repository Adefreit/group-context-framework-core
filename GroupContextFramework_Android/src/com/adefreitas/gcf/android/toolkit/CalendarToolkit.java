package com.adefreitas.gcf.android.toolkit;

import java.util.ArrayList;
import java.util.Date;

import org.json.JSONObject;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;

public class CalendarToolkit
{
	// Constants
	public static final String EVENT_NAME = "EventName";
	public static final String START_TIME = "StartTime";
	public static final String END_TIME   = "EndTime";
	
	// Calendar API
	private ContentResolver		  resolver;
	private Cursor 			      mCursor;
	private static final String[] COLS = new String[] { 
		CalendarContract.Instances.TITLE,
		CalendarContract.Instances.BEGIN,
		CalendarContract.Instances.END,
		CalendarContract.Instances.DURATION };
	
	/**
	 * Constructor
	 * @param resolver
	 */
	public CalendarToolkit(ContentResolver resolver)
	{
		this.resolver = resolver;
	}
	
	/**
	 * Pulls Calendar Information from the Phone
	 */
	private ArrayList<CalendarInfo> getCalendarContents(int numDays)
	{		
		// Dates
		Date start = new Date();
		Date end   = new Date(start.getTime() + numDays * 86400000);
		
		return getCalendarInfo(end);
	}
	
	public ArrayList<CalendarInfo> getCalendarInfo(Date stopDate)
	{
		ArrayList<CalendarInfo> result = new ArrayList<CalendarInfo>();
		
		// Dates
		Date start = new Date();
		Date end   = stopDate;
		
		// Generates the Calendar URI
		Uri.Builder eventsUriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon();
		ContentUris.appendId(eventsUriBuilder, start.getTime());
		ContentUris.appendId(eventsUriBuilder, end.getTime());
		Uri eventsUri = eventsUriBuilder.build();	
		
		mCursor = resolver.query(eventsUri, COLS, null, null, "begin ASC");
		
		if (mCursor != null)
		{
			while (mCursor.moveToNext())
			{
				String eventName = mCursor.getString(0);
				Date   startTime = new Date(mCursor.getLong(1));
				Date   endTime   = new Date(mCursor.getLong(2));
				
				if (startTime.getTime() >= new Date().getTime())
				{
					result.add(new CalendarInfo(eventName, startTime, endTime));	
				}
			}	
		}
		
		return result;
	}

	public ArrayList<JSONObject> getCalendarInfoAsJSON(Date stopDate)
	{
		ArrayList<JSONObject> result = new ArrayList<JSONObject>();
		
		for (CalendarInfo event : getCalendarInfo(stopDate))
		{
			JSONObject obj = event.toJSON();
			
			if (obj != null)
			{
				result.add(obj);		
			}
		}
		
		return result;
	}
	
	public ArrayList<CalendarInfo> getCalendarInfo(int numEntries)
	{
		ArrayList<CalendarInfo> result = new ArrayList<CalendarInfo>();
		
		// Populates the List
		ArrayList<CalendarInfo> calendar = getCalendarContents(7);
		
		for (int i=0; i<Math.min(calendar.size(), numEntries); i++)
		{
			result.add(calendar.get(i));
		}
		
		return result;
	}
	
	public ArrayList<JSONObject> getCalendarInfoAsJSON(int numEntries)
	{
		ArrayList<JSONObject> result = new ArrayList<JSONObject>();
		
		for (CalendarInfo event : getCalendarInfo(numEntries))
		{
			JSONObject obj = event.toJSON();
			
			if (obj != null)
			{
				result.add(obj);		
			}
		}
		
		return result;
	}

	/**
	 * Helper Class to Represent a Calendar Event
	 * @author adefreit
	 */
	public class CalendarInfo
	{
		private String eventName;
		private Date   startDate;
		private Date   endDate;
		
		public CalendarInfo(String eventName, Date startDate, Date endDate)
		{
			this.eventName = eventName;
			this.startDate = startDate;
			this.endDate   = endDate;
		}
		
		public String getEventName()
		{
			return eventName;
		}
		
		public Date getStartDate()
		{
			return startDate;
		}
		
		public Date getEndDate()
		{
			return endDate;
		}
	
		public JSONObject toJSON()
		{
			JSONObject obj = new JSONObject();
			
			try
			{	
				// Populates JSON with Important Fields
				obj.put(EVENT_NAME, getEventName());
				obj.put(START_TIME, getStartDate().getTime());
				obj.put(END_TIME,	getEndDate().getTime());
				
				return obj;
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
			
			// Returns NULL if something went wrong
			return null;
		}
	}
}
