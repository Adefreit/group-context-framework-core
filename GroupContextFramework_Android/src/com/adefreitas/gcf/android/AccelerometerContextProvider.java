package com.adefreitas.gcf.android;

import java.util.ArrayList;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.adefreitas.gcf.ContextProvider;
import com.adefreitas.gcf.ContextReportingThread;
import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.messages.CommMessage;

public class AccelerometerContextProvider extends ContextProvider implements SensorEventListener
{
	private SensorManager sensorManager;
	private Sensor 		  accelerometerSensor;
	private double 		  previousX, previousY, previousZ;
	private double 		  deltaX, deltaY, deltaZ;
	private double 		  accuracy;
	private double 		  noiseConstant;
	
	private ArrayList<String> operations;
	
	private ContextReportingThread t;
	
	public AccelerometerContextProvider(GroupContextManager groupContextManager, SensorManager sensorManager, double noiseConstant) 
	{
		super("ACC", groupContextManager);
		
		this.sensorManager 	     = sensorManager;
		this.accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		this.previousX 			 = Double.NaN;
		this.previousY 			 = Double.NaN;
		this.previousZ 			 = Double.NaN;
		this.deltaX 			 = 0.0;
		this.deltaY				 = 0.0;
		this.deltaZ				 = 0.0;
		this.noiseConstant 		 = noiseConstant;
		
		operations 		 = new ArrayList<String>();
		
		// Turns on Sensor to get First Accuracy Reading
		start();
	}

	public void updateConfiguration()
	{
		super.updateConfiguration();
		
		operations.clear();
		
		for (ContextSubscriptionInfo csi : this.getSubscriptions())
		{
			for (String opcode : CommMessage.getValues(csi.getParameters(), "OPCODE"))
			{
				if (!operations.contains(opcode))
				{
					operations.add(opcode);
				}
			}
		}
		
		Log.d("ContextProvider", "# of operations: " + operations.size());
	}
	
	@Override
	public void start() 
	{
		sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
		t = new ContextReportingThread(this);
		t.start();
		
		Log.d("GCM-ContextProvider", "Accelerometer Sensor Started");
	}

	@Override
	public void stop() 
	{
		sensorManager.unregisterListener(this);
		
		if (t != null)
		{
			t.halt();
			t = null;	
		}
		
		Log.d("GCM-ContextProvider", "Accelerometer Sensor Stopped");
	}
	
	@Override
	public double getFitness(String[] parameters) 
	{
		return accuracy;
	}

	@Override
	public void sendContext() 
	{
		String[] gravityData = new String[] { Double.toString(previousX), Double.toString(previousY), Double.toString(previousZ) }; 
		String[] deltaData   = new String[] { Double.toString(previousX), Double.toString(previousY), Double.toString(previousZ) }; 
		
		if (operations.size() > 0)
		{
			for (String operation : operations)
			{	
				if (operation.equalsIgnoreCase("GRAVITY"))
				{	
					this.getGroupContextManager().sendContext(getContextType(), new String[0], gravityData);	
				}
				else
				{ 
					this.getGroupContextManager().sendContext(getContextType(), new String[0], deltaData);
				}
			}
		}
		else
		{
			this.getGroupContextManager().sendContext(getContextType(), new String[0], gravityData);	
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) 
	{
		if (sensor.getType() == Sensor.TYPE_ACCELEROMETER)
		{
			// Android SDK reports Sensor Accuracy in Terms of Low (1), Medium (2), or High (3)
			// http://developer.android.com/reference/android/hardware/SensorManager.html
			this.accuracy = (double)accuracy / 3.0;	
		}
		
		if (!this.isInUse())
		{
			stop();
		}
	}

	@Override
	public void onSensorChanged(SensorEvent event) 
	{
		if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
		{
			double x = event.values[0] - noiseConstant;
			double y = event.values[1] - noiseConstant;
			double z = event.values[2] - noiseConstant;
			
			if (Double.isNaN(previousX) || Double.isNaN(previousY) || Double.isNaN(previousZ))
			{
				previousX = x;
				previousY = y;
				previousZ = z;
			}
			else
			{
				deltaX    = previousX - x;
				deltaY    = previousY - y;
				deltaZ    = previousZ - z;
				previousX = x;
				previousY = y;
				previousZ = z;
			}
		}
	}
	
	private double getMagnitude()
	{
		return Math.sqrt(Math.pow(deltaX, 2.0) + Math.pow(deltaY, 2.0) + Math.pow(deltaZ,  2.0));
	}
}
