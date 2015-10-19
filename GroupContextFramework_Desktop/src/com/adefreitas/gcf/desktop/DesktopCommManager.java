package com.adefreitas.gcf.desktop;

import com.adefreitas.gcf.CommManager;
import com.adefreitas.gcf.messages.CommMessage;

public class DesktopCommManager extends CommManager
{
	private DesktopGroupContextManager gcm;
	
	public DesktopCommManager(DesktopGroupContextManager gcm)
	{
		super(gcm);
		this.gcm = gcm;
	}

	@Override
	public String connect(CommMode commMode, String ipAddress, int port)
	{
		try
		{
			String socketKey = this.getCommThreadKey(commMode, ipAddress, port);
			
			// Generates the Correct Kind of Comm Thread
			if (!this.commThreads.containsKey(socketKey))
			{
				if (commMode == CommManager.CommMode.UDP_MULTICAST)
				{
					MulticastCommThread t = new MulticastCommThread(this, port, ipAddress);
					t.start();
					
					this.commThreads.put(socketKey, t);
				}
				else if (commMode == CommManager.CommMode.TCP)
				{
					TCPCommThread t = new TCPCommThread(this, this.getGroupContextManager().getDeviceID(), port, ipAddress);
					t.start();
					
					this.commThreads.put(socketKey, t);
				}
				else if (commMode == CommManager.CommMode.MQTT)
				{
					MqttCommThread t = new MqttCommThread(this, this.getGroupContextManager().getDeviceID(), ipAddress, port);
					t.start();
					
					this.commThreads.put(socketKey, t);
				}
				else
				{
					System.out.println("Communications Mode Not Recognized " + commMode);
				}	
			}
			else
			{
				System.out.println("Socket " + socketKey + " Already Created");
			}
			
			return socketKey;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		return null;
	}

	public void onMessage(CommMessage message)
	{		
		gcm.onMessage(message);
	}

}
