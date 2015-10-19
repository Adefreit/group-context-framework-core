package com.adefreitas.gcf.desktop;
import com.adefreitas.gcf.BatteryMonitor;

public class DesktopBatteryMonitor extends BatteryMonitor
{

	public DesktopBatteryMonitor()
	{
		super(5);
	}

	public double getBatteryPowerRemaining() {
		return 100.0;
	}
	
	public double getBatteryChange(long duration)
	{
		return 0.0;
	}

	
	@Override
	public boolean isCharging() {
		// TODO Auto-generated method stub
		return true;
	}
}
