package com.adefreitas.gcf.android.providers.legacy;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import com.adefreitas.gcf.ContextProvider;
import com.adefreitas.gcf.ContextReportingThread;
import com.adefreitas.gcf.GroupContextManager;

public class GPSContextProvider extends ContextProvider
{
	private LocationManager  locationManager;
	private LocationListener locationListener;
	
	// Last Known Coordinates
	private double lat;
	private double lon;
	private double accuracy;
	
	// This is the Thread that will Report
	private ContextReportingThread t;
	
	public GPSContextProvider(Context context, GroupContextManager groupContextManager) 
	{
		super("LOC", groupContextManager);
		
		// Initializes Values
		this.lat      = 0.0;
		this.lon      = 0.0;
		this.accuracy = -1.0;
		
		this.locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
		
		// Creates a Location Listener
		  locationListener = new LocationListener()
			{

				@Override
				public void onLocationChanged(Location location) {
					if (location != null)
					{
						lat      = location.getLatitude();
						lon      = location.getLongitude();
						accuracy = location.getAccuracy();
					}
				}

				@Override
				public void onProviderDisabled(String provider) 
				{
					
				}

				@Override
				public void onProviderEnabled(String provider) 
				{
					
				}

				@Override
				public void onStatusChanged(String provider, int status, Bundle extras) {
					// TODO Auto-generated method stub
					
				}

			};

	}

	@Override
	public void start() 
	{
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
		
		// Turns on the Reporting Thread
		t = new ContextReportingThread(this);
		t.start();
		
		Log.d("GCM-ContextProvider", "GPS Context Sensor Started");
	}

	@Override
	public void stop() 
	{
		locationManager.removeUpdates(locationListener);
		
		// Halts the Reporting Thread
		if (t != null)
		{
			t.halt();
			t = null;	
		}
		
		Log.d("GCM-ContextProvider", "GPS Sensor Stopped");
	}

	@Override
	public double getFitness(String[] parameters) 
	{
		return 1.0;
	}

	@Override
	public void sendContext() 
	{
		this.getGroupContextManager().sendContext(this.getContextType(), new String[0], new String[] { Double.toString(lat), Double.toString(lon), Double.toString(accuracy) });
	}
}
