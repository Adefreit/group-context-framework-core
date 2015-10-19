package com.adefreitas.gcf.desktop.providers;

import java.util.ArrayList;

import com.adefreitas.gcf.ContextProvider;
import com.adefreitas.gcf.ContextReportingThread;
import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;

public class FixedLocationContextProvider extends ContextProvider
{
	private double lat;
	private double lon;
	private int    zipcode;
	private String state;
	private String city;
	private String country;
	
	private ContextReportingThread t;
	
	public FixedLocationContextProvider(GroupContextManager groupContextManager) {
		super("LOC", groupContextManager);
		this.lat     = 0.0;
		this.lon     = 0.0;
		this.zipcode = 0;
		this.state   = "UNKNOWN";
		this.city    = "UNKNOWN";
		this.country = "UNKNOWN";
	}

	@Override
	public void start() 
	{	
		t = new ContextReportingThread(this);
		t.start();
		
		this.getGroupContextManager().log("GCM-ContextProvider", "Fixed Location Sensor Started");
	}

	@Override
	public void stop() {
		// Halts the Reporting Thread
		if (t != null)
		{
			t.halt();
			t = null;	
		}
	}
	
	@Override
	public double getFitness(String[] parameters) {
		return 1.0;
	}

	public void setLocation(double latitude, double longitude, String city, String state, int zipcode, String country)
	{
		this.lat = latitude;
		this.lon = longitude;
	}
	
	@Override
	public void sendContext() 
	{
		ArrayList<String> context = new ArrayList<String>();
		
		// Always Include Latitude and Longitude
		context.add("LATITUDE=" + lat);
		context.add("LONGITUDE=" + lon);
		
		for (ContextSubscriptionInfo csi : this.getSubscriptions())
		{
			if (csi.getParameter("CITY").equalsIgnoreCase("TRUE"))
			{
				String contextToAdd = "CITY=" + city;
				
				if (!context.contains(contextToAdd))
				{
					context.add(contextToAdd);
				}
			}
			
			if (csi.getParameter("ZIPCODE").equalsIgnoreCase("TRUE"))
			{
				String contextToAdd = "ZIPCODE=" + zipcode;
				
				if (!context.contains(contextToAdd))
				{
					context.add(contextToAdd);
				}
			}
			
			if (csi.getParameter("COUNTRY").equalsIgnoreCase("TRUE"))
			{
				String contextToAdd = "COUNTRY=" + country;
				
				if (!context.contains(contextToAdd))
				{
					context.add(contextToAdd);
				}
			}
			
			if (csi.getParameter("STATE").equalsIgnoreCase("TRUE"))
			{
				String contextToAdd = "STATE=" + state;
				
				if (!context.contains(contextToAdd))
				{
					context.add(contextToAdd);
				}
			}
		}
		
		// Sends the Requested Information
		this.sendContext(new String[0], context.toArray(new String[0]));
	}
}
