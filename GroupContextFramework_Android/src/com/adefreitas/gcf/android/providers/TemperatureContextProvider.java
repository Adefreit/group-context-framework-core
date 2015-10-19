package com.adefreitas.gcf.android.providers;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.adefreitas.gcf.ContextProvider;
import com.adefreitas.gcf.GroupContextManager;

/**
 * This is a template for a context provider.
 * COPY AND PASTE; NEVER USE
 * @author adefreit
 */
public class TemperatureContextProvider extends ContextProvider implements SensorEventListener
{
	// Context Configuration
	public  static final String CONTEXT_TYPE  = "TEMP";
	private static final String LOG_NAME      = "GCF-ContextProvider [" + CONTEXT_TYPE + "]";
	
	private final SensorManager mSensorManager;
	private final Sensor 		mTempSensor;
	
	private double temperature;
	private double accuracy;
	
	public TemperatureContextProvider(GroupContextManager groupContextManager, Context context) 
	{
		super(CONTEXT_TYPE, groupContextManager);
		
		mSensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
	    mTempSensor    = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
	}

	@Override
	public void start() 
	{
		this.getGroupContextManager().log(LOG_NAME, this.getContextType() + " Provider Started");
		mSensorManager.registerListener(this, mTempSensor, SensorManager.SENSOR_DELAY_NORMAL);
		
	    temperature = -1.0;
	    accuracy 	= -1.0;
	}

	@Override
	public void stop() 
	{
		this.getGroupContextManager().log(LOG_NAME, this.getContextType() + " Provider Stopped");
		mSensorManager.unregisterListener(this);
	}

	@Override
	public double getFitness(String[] parameters) 
	{
		return accuracy;
	}

	@Override
	public void sendContext() 
	{
		this.sendContext(this.getSubscriptionDeviceIDs(), new String[] { "TEMP=" + temperature });
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		temperature = event.values[0];
	}
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		this.accuracy = accuracy; 
	}
}
