package com.adefreitas.gcf.android.providers;

import java.util.ArrayList;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import com.adefreitas.gcf.ContextProvider;
import com.adefreitas.gcf.ContextReportingThread;
import com.adefreitas.gcf.GroupContextManager;

/**
 * Compass Context Provider [COMPASS]
 * Delivers Compass Heading using the Magnetometer and Accelerometer
 * 
 * Parameters
 *      NONE 
 * 
 * Author: Adrian de Freitas
 */
public class CompassContextProvider extends ContextProvider implements SensorEventListener
{
	// GCF Context Configuration
	private static final String CONTEXT_TYPE = "COMPASS";
	private static final String LOG_NAME     = "GCF-ContextProvider [" + CONTEXT_TYPE + "]";
	private static final String DESCRIPTION  = "Shares compass data using the phone's magnetometer and accelerometer.";
	
    // Device sensor manager
    private SensorManager sensorManager;
    private Sensor        accelerometer;
    private Sensor        magnetometer;
    
    // Values
    final int maxEntries = 5;
    int accuracy;
    ArrayList<Double> azimuth;
    ArrayList<Double> pitch;
    ArrayList<Double> roll;
    float[] mGravity;
    float[] mGeomagnetic;
	
	public CompassContextProvider(Context context, GroupContextManager groupContextManager) 
	{
		super(CONTEXT_TYPE, DESCRIPTION, groupContextManager);
		
		// initialize your android device sensor capabilities
        sensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer  = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
      
        azimuth  = new ArrayList<Double>();
        pitch    = new ArrayList<Double>();
        roll     = new ArrayList<Double>();
        accuracy = -1;
	}

	@Override
	public void start() 
	{
		sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
	    sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
	  
		Log.d(LOG_NAME, "Compass Sensor Started");
	}

	@Override
	public void stop() 
	{
		sensorManager.unregisterListener(this);
		Log.d(LOG_NAME, "GPS Sensor Stopped");
	}

	@Override
	public double getFitness(String[] parameters) 
	{
		return 1.0;
	}

	@Override
	public void sendContext() 
	{
		
		this.getGroupContextManager().sendContext(this.getContextType(), 
				this.getSubscriptionDeviceIDs(), 
				new String[] { "AZIMUTH=" + String.format("%1.1f", getAverage(azimuth)), 
			   				   "PITCH=" + String.format("%1.1f", getAverage(pitch)), 
			   				   "ROLL=" + String.format("%1.1f", getAverage(roll)), 
			   				   "ACCURACY=" + String.format("%d", accuracy) });
	}

	@Override
	public void onSensorChanged(SensorEvent event) 
	{
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
		{
			mGravity = event.values;
		}
		   
		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
		{
			mGeomagnetic = event.values;
		}
		     
		if (mGravity != null && mGeomagnetic != null) 
		{
			float R[] = new float[9];
		    float I[] = new float[9];
		    
		    boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
		    
		    if (success) 
		    {
		    	float orientation[] = new float[3];
		        SensorManager.getOrientation(R, orientation);
		        
		        azimuth.add(normalizeAngle(orientation[0]));
		        pitch.add(normalizeAngle(orientation[1]));
		        roll.add(normalizeAngle(orientation[2]));
		        
		        if (azimuth.size() > maxEntries)
		        {
		        	azimuth.remove(0);
		        	pitch.remove(0);
		        	roll.remove(0);
		        }
		    }
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) 
	{
		/**
		 * SENSOR_STATUS_ACCURACY_HIGH = 3
		   SENSOR_STATUS_ACCURACY_MEDIUM = 2
		   SENSOR_STATUS_ACCURACY_LOW = 1
		   SENSOR_STATUS_UNRELIABLE = 0
		 */
		
		this.accuracy = accuracy;
	}

	private double normalizeAngle(double angleInRadians)
	{
	    double newAngle = Math.toDegrees(angleInRadians);
	    
	    if (newAngle < 0) 
	    {
	    	newAngle += 360;
	    }
	    else if (newAngle > 360) 
	    {
	    	newAngle -= 360;
	    }
	    
	    return newAngle;
	}

	private double getAverage(ArrayList<Double> values)
	{
		double sum = 0.0;
		double count = 0;
		
		for (double value : values)
		{
			sum += value;
			count++;
		}
		
		if (count > 0.0)
		{
			return sum / count;
		}
		else
		{
			return 0.0;
		}
	}
}
