package com.adefreitas.gcf.android.providers;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.adefreitas.gcf.ContextProvider;
import com.adefreitas.gcf.GroupContextManager;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

/**
 * Location Context Provider [LOC]
 * Delivers Current Location, as Determined by Google Fused Location Provider
 * 
 * All Hail http://javapapers.com/android/android-location-fused-provider/ for the sample code!
 * 
 * Author: Adrian de Freitas
 */
public class LocationContextProvider extends ContextProvider implements LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
{
	// Context Configuration	
	private static final String CONTEXT_TYPE = "LOC";
	private static final String LOG_NAME     = "GCF-ContextProvider [" + CONTEXT_TYPE + "]";
	private static final String DESCRIPTION  = "Uses Google Fused Location provider to specify the user's location.  Uses a mix of" +
			" cell towers, Wi-Fi hotspots, and GPS to obtain this information as efficiently as possible";
	    
	// Variables
	private Context context;
	private boolean runForever;
	
	// Default Values
	private static final int PRIORITY = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
	
	// Most Recent Value
	private Location currentLocation;
	
	// Activity Recognition
	private Boolean			playServicesEnabled;
	private GoogleApiClient googleApiClient;
	
	public LocationContextProvider(Context context, GroupContextManager groupContextManager) 
	{
		super(CONTEXT_TYPE, DESCRIPTION, groupContextManager);
		this.context = context;
		
		// Default Values
		runForever 			= false;
		playServicesEnabled = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS;
		
		// Sets Up the Google API Client
		if (playServicesEnabled)
		{
			googleApiClient = new GoogleApiClient.Builder(context)
			.addApi(LocationServices.API)
			.addConnectionCallbacks(this)
			.addOnConnectionFailedListener(this)
			.build();
			googleApiClient.connect();
		}
	}

	public void start(boolean runForever)
	{
		playServicesEnabled = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS;
		
		if (playServicesEnabled)
		{
			this.runForever = runForever;
			
			if (!googleApiClient.isConnecting() && googleApiClient.isConnected())
			{
				googleApiClient.connect();	
			}
						
			this.getGroupContextManager().log(LOG_NAME, this.getContextType() + " Provider Started [runForever=" + runForever + "]");	
		}
		else
		{
			Toast.makeText(context, "Cannot Start:  Google Play Services Not Found.", Toast.LENGTH_SHORT).show();
		}
	}
	
	@Override
	public void start() 
	{
		start(runForever);
	}

	@Override
	public void stop() 
	{
		playServicesEnabled = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS;
		
		if (playServicesEnabled)
		{
			this.getGroupContextManager().log(LOG_NAME, this.getContextType() + " Provider Stopped");
			
			if (!runForever)
			{
				LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
				googleApiClient.disconnect();
				currentLocation = null;
			}
		}
		else
		{
			Toast.makeText(context, "Cannot Stop:  Google Play Services Not Found.", Toast.LENGTH_SHORT).show();
		}
	}

	public void reboot()
	{
		playServicesEnabled = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS;
		runForever = false;
		Log.d(LOG_NAME, "Rebooting");
		super.reboot();
	}
	
	@Override
	public double getFitness(String[] parameters) 
	{
		if (currentLocation != null)
		{
			return currentLocation.getAccuracy();
		}
		
		return 0.0;
	}

	@Override
	public void sendContext() 
	{
		if (currentLocation != null)
		{
			this.sendContext(this.getSubscriptionDeviceIDs(), new String[] { 
				"LATITUDE="  + currentLocation.getLatitude(), 
				"LONGITUDE=" + currentLocation.getLongitude(),
				"ALTITUDE="  + currentLocation.getAltitude(),
				"BEARING="   + currentLocation.getBearing(),
				"SPEED="     + currentLocation.getSpeed()
				});
		}
	}

	// Getters
	public boolean hasLocation()
	{
		return currentLocation != null;
	}
	
	public double getLatitude()
	{
		if (currentLocation != null)
		{
			return currentLocation.getLatitude();
		}
		
		return Double.NaN;
	}
	
	public double getLongitude()
	{
		if (currentLocation != null)
		{
			return currentLocation.getLongitude();
		}
		
		return Double.NaN;
	}
	
	public double getAltitude()
	{
		if (currentLocation != null)
		{
			return currentLocation.getAltitude();
		}
		
		return Double.NaN;
	}
	
	public double getBearing()
	{
		if (currentLocation != null)
		{
			return currentLocation.getBearing();
		}
		
		return Double.NaN;
	}
	
	public double getSpeed()
	{
		if (currentLocation != null)
		{
			return currentLocation.getSpeed();
		}
		
		return Double.NaN;
	}
	
	// Google API Client Methods
	@Override
	public void onConnectionFailed(ConnectionResult result) 
	{
		Log.d(LOG_NAME, "Location Recognition Connection Failed");
	}

	@Override
	public void onConnected(Bundle bundle) 
	{
		Log.d(LOG_NAME, "Location Recognition Connected");
		
		// Generates a Location Request
		LocationRequest r = new LocationRequest();
        r.setInterval(this.getRefreshRate());
        r.setFastestInterval(this.getRefreshRate()/2);
        r.setPriority(PRIORITY);
		PendingResult<Status> pendingResult = LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, r, this);
	}

	@Override
	public void onConnectionSuspended(int cause) 
	{
		Log.d(LOG_NAME, "Location Recognition Connection Suspended");	
		googleApiClient.connect();
	}
	
	// Event is Fired Whenever the Location Changes
	@Override
	public void onLocationChanged(Location location) 
	{
		Log.d(LOG_NAME, "Location Updated: " + location.getLatitude() + ", " + location.getLongitude());	
		this.currentLocation = location;
	}
}
