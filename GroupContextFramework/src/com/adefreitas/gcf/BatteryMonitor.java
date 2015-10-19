package com.adefreitas.gcf;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public abstract class BatteryMonitor 
{	
	public static final int MILLISECONDS_IN_DAY = 86400000;
	
	private ArrayList<BatteryInfo> batteryPowerHistory;
	private int 				   maxBatteryHistory;
	
	public BatteryMonitor(int maxBatteryHistory)
	{
		batteryPowerHistory    = new ArrayList<BatteryInfo>();
		this.maxBatteryHistory = maxBatteryHistory;
	}
	
	public double getBatteryPercent() {
		if (batteryPowerHistory.size() > 0)
		{
			return batteryPowerHistory.get(batteryPowerHistory.size() - 1).getPercentage();
		}
		else
		{
			return 100.0;
		}
	}
	
	public void addBatteryInfo(double percentage, double estimatedTimeRemaining, boolean charging)
	{
		addBatteryInfo(new Date(), percentage, estimatedTimeRemaining, charging);
	}
	
	public void addBatteryInfo(Date date, double percentage, double estimatedTimeRemaining, boolean charging)
	{
		batteryPowerHistory.add(new BatteryInfo(date, percentage, estimatedTimeRemaining, charging));
		
    	while (batteryPowerHistory.size() > maxBatteryHistory)
    	{
    		batteryPowerHistory.remove(0);
    	}
	}

	public BatteryInfo getMostRecentBatteryInfo()
	{
		if (batteryPowerHistory.size() > 0)
		{
			return batteryPowerHistory.get(batteryPowerHistory.size()-1);
		}
		else
		{
			return null;
		}
	}

	public double getBatteryTimeRemaining()
	{
//		if (batteryPowerHistory.size() <= 1)
//		{
//			return MILLISECONDS_IN_DAY;
//		}
//		else
//		{
//			BatteryInfo earliestEntry = batteryPowerHistory.get(0);
//			BatteryInfo latestEntry   = batteryPowerHistory.get(batteryPowerHistory.size() - 1);
//			BatteryInfo tmp           = batteryPowerHistory.get(batteryPowerHistory.size() - 2);
//			
//			if (latestEntry.isCharging())
//			{
//				return MILLISECONDS_IN_DAY;
//			}
//			else
//			{				
//				double percentChange = latestEntry.getPercentage() - tmp.getPercentage();
//				long   timeSpan      = latestEntry.getTimestamp().getTime() - tmp.getTimestamp().getTime();
//				
//				if (percentChange >= 0)
//				{
//					return MILLISECONDS_IN_DAY;
//				}
//				else
//				{
//					return timeSpan * getBatteryPercent(); 
//				}
//			}
//		}
		
		if (batteryPowerHistory.size() == 0 || batteryPowerHistory.get(batteryPowerHistory.size()-1).isCharging())
		{
			return 100.0;
		}
		else
		{
			return getBatteryPercent();
		}
	}

	public abstract boolean isCharging();
	
	public class BatteryInfo
	{
		private Date    timestamp;
		private double  percentage;
		private double  estimatedTimeRemaining;
		private boolean charging;
		
		public BatteryInfo(Date timestamp, double percentage, double estimatedTimeRemaining, boolean charging)
		{
			this.timestamp  			= timestamp;
			this.percentage 			= percentage;
			this.estimatedTimeRemaining = estimatedTimeRemaining;
			this.charging 				= charging;
		}
		
		public Date getTimestamp()
		{
			return timestamp;
		}
			
		public double getPercentage()
		{
			return percentage;
		}
		
		public double getEstimatedTimeRemaining()
		{
			return estimatedTimeRemaining;
		}
		
		public boolean isCharging()
		{
			return charging;
		}
		
		public String toString()
		{
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.US);
			return String.format(Locale.US, "%s\t%1.4f\t%1.4f\t%s", sdf.format(timestamp), percentage, estimatedTimeRemaining, (charging) ? "Charging" : "Not Charging");
		}
	}
}
