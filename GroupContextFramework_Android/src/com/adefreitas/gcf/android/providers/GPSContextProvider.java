package com.adefreitas.gcf.android.providers;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import com.adefreitas.gcf.ContextProvider;
import com.adefreitas.gcf.ContextReportingThread;
import com.adefreitas.gcf.GroupContextManager;

/**
 * GPS Context Provider [LOC_GPS]
 * Delivers GPS coordinates
 * 
 * Parameters
 *      NONE 
 * 
 * Author: Adrian de Freitas
 */
public class GPSContextProvider extends ContextProvider
{
	// GCF Context Configuration
	private static final String CONTEXT_TYPE = "LOC_GPS";
	private static final String LOG_NAME     = "GCF-ContextProvider [" + CONTEXT_TYPE + "]";
	private static final String DESCRIPTION  = "Shares location Data using the GPS sensor.  This method uses more power than Google Fused location (LOC), but is not susceptible to cell tower problems.";
	private static final String SENSOR       = LocationManager.GPS_PROVIDER;
	
	// Location Variables
	private LocationManager  locationManager;
	private LocationListener locationListener;
	
	// Last Known Coordinates
	private double lat;
	private double lon;
	private double accuracy;
	
	public GPSContextProvider(Context context, GroupContextManager groupContextManager) 
	{
		super(CONTEXT_TYPE, DESCRIPTION, groupContextManager);
		
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
			public void onStatusChanged(String provider, int status, Bundle extras) 
			{
				
			}
		};

	}

	@Override
	public void start() 
	{
		locationManager.requestLocationUpdates(SENSOR, 0, 0, locationListener);		
		Log.d(LOG_NAME, "GPS Context Sensor Started");
	}

	@Override
	public void stop() 
	{
		locationManager.removeUpdates(locationListener);
		Log.d(LOG_NAME, "GPS Sensor Stopped");
	}

	@Override
	public double getFitness(String[] parameters) 
	{
		return accuracy;
	}

	@Override
	public void sendContext() 
	{
		this.getGroupContextManager().sendContext(this.getContextType(), 
				this.getSubscriptionDeviceIDs(), 
				new String[] { "LATITUDE=" + Double.toString(lat), "LONGITUDE=" + Double.toString(lon), "ACCURACY=" + Double.toString(accuracy) });
	}
}
