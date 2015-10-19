package com.adefreitas.gcf.android.providers.legacy;
import com.adefreitas.gcf.*;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class CompassContextProvider extends ContextProvider implements SensorEventListener
{
	private SensorManager sensorManager;
	private Sensor 		  accelerometer;
	private Sensor 		  magnetometer;
	
	private float[] mGravity;
	private float[] mGeomagnetic;
	
	private double accuracy;

	// Most Recent Calculations
	double azimuthInDegrees;
	double pitchInDegrees;
	double rollInDegrees;
	
	// Auto Reporting Feature
	private ContextReportingThread t;
	
	/**
	 * Constructor
	 * @param groupContextManager
	 */
	public CompassContextProvider(GroupContextManager groupContextManager, SensorManager sensorManager)
	{
		super("CPS", groupContextManager);
		
		this.sensorManager = sensorManager;
		this.accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		this.magnetometer  = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

		azimuthInDegrees = Double.NaN;
		pitchInDegrees   = Double.NaN;
		rollInDegrees    = Double.NaN;
		
		start();
	}
		
	public void onAccuracyChanged(Sensor sensor, int accuracy) 
    {
		// Android SDK reports Sensor Accuracy in Terms of Low (1), Medium (2), or High (3)
		// http://developer.android.com/reference/android/hardware/SensorManager.html
    	this.accuracy = (double)(accuracy / 3.0);
    }

	public void onSensorChanged(SensorEvent event) 
    {
    	if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
    	{
    		mGravity = event.values;
    	}
    	else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
    	{
    		mGeomagnetic = event.values;
    	}
    	      
    	if (mGravity != null && mGeomagnetic != null) 
    	{
    		float R[] = new float[9];
    	    float I[] = new float[9];
    	    
    	    if (SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic)) 
    	    {
    	        float orientation[] = new float[3];  // orientation contains: azimuth, pitch and roll
    	        SensorManager.getOrientation(R, orientation);
    	        
    	        if (isInUse())
    	        {    	        	
    	        	azimuthInDegrees = (double)orientation[0] * (180.0/Math.PI);
    	        	pitchInDegrees   = (double)orientation[1] * (180.0/Math.PI);
    	        	rollInDegrees    = (double)orientation[2] * (180.0/Math.PI);
    	        }
    	    }
    	}
    	
    	if (!this.isInUse())
		{
			stop();
		}
    }
		
	public void start()
	{	
		Log.d("GCM-ContextProvider", "Compass Sensor Started");
		sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
		sensorManager.registerListener(this, magnetometer,  SensorManager.SENSOR_DELAY_NORMAL);
		
		// Turns on the Reporting Thread
//		t = new ContextReportingThread(this);
//		t.start();
	}

	public void stop()
	{
		Log.d("GCM-ContextProvider", "Compass Sensor Stopped");
		sensorManager.unregisterListener(this);
		
		// Halts the Reporting Thread
		if (t != null)
		{
			t.halt();
			t = null;	
		}
	}
	
	public double getFitness(String[] parameters)
	{
		return accuracy;
	}
	
	public void sendContext()
	{		
		if (!Double.isNaN(azimuthInDegrees) && !Double.isNaN(pitchInDegrees) && !Double.isNaN(rollInDegrees))
		{
			this.getGroupContextManager().sendContext(getContextType(), new String[0], new String[] { Double.toString(azimuthInDegrees), Double.toString(pitchInDegrees), Double.toString(rollInDegrees) });
		}
	}
}
