package com.adefreitas.gcf.messages;

import java.util.Locale;

public class ContextCapability extends CommMessage
{
	private String  deviceType;
	private String  contextType;
	private int     heartbeatRate;
	private double 	batteryLife;
	private double  sensorFitness;
	private boolean alreadyProviding;

	public ContextCapability(String deviceID, String deviceType, String contextType, int heartbeatRate, double batteryLife, double sensorFitness, boolean alreadyProviding, String[] destination) 
	{
		super(CommMessage.MESSAGE_TYPE_CAPABILITY);
		this.deviceID 		  = deviceID;
		this.deviceType 	  = deviceType;
		this.contextType 	  = contextType;
		this.heartbeatRate    = heartbeatRate;
		this.batteryLife 	  = batteryLife;
		this.sensorFitness 	  = sensorFitness;
		this.alreadyProviding = alreadyProviding;
		this.destination	  = destination;
	}
		
	public String getDeviceType()
	{
		return deviceType;
	}
	
	public String getContextType()
	{
		return contextType;
	}
	
	public int getHeartbeatRate()
	{
		return heartbeatRate;
	}
	
	public double getBatteryLife()
	{
		return batteryLife;
	}
	
	public double getSensorFitness()
	{
		return sensorFitness;
	}
	
	public boolean isAlreadyProviding()
	{
		return alreadyProviding;
	}
	
	public String toString()
	{		
		return String.format(Locale.getDefault(), "CAPABILITY: %s (%s) [Device=%s Bat=%1.2f Fitness=%1.2f Providing=%s]", 
				contextType, deviceID,	deviceType, batteryLife, sensorFitness, alreadyProviding);
	}
}
