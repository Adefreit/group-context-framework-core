package com.adefreitas.gcf.desktop;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.adefreitas.gcf.CommManager;
import com.adefreitas.gcf.ContextProvider;
import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.messages.ContextCapability;
import com.adefreitas.gcf.messages.ContextData;
import com.adefreitas.gcf.messages.ContextRequest;

public class DesktopGroupContextManager extends GroupContextManager
{
	// Request Delay
	private static final int REQUEST_DELAY_TIME = 5000;
	
	// This Flag is Used to Determine Whether or not to Display Debug Events
	private boolean DEBUG;
	
	// This Thread Performs Scheduled GCF Tasks 
	private ScheduledTask scheduledTimerTask;
	
	// This Object is Used to Manage Event Receivers
	private ArrayList<EventReceiver> eventReceivers;
	
	// This is Used to Manage Print Debug Statements
	private ArrayList<String> debugFilter;
	
	/**
	 * Constructor (Does Not Form Any Connections)
	 * @param deviceID
	 * @param batteryMonitor
	 * @param promiscuous
	 */
	public DesktopGroupContextManager(String deviceID, boolean promiscuous)
	{
		super(deviceID, GroupContextManager.DeviceType.Desktop, new DesktopBatteryMonitor(), promiscuous);

		// Creates a Basic Communications Manager
		this.commManager = new DesktopCommManager(this);
		
		// Disables Debug Mode by Default
		DEBUG       = false;
		debugFilter = new ArrayList<String>();
		
		// Initializes List of Event Receivers
		this.eventReceivers = new ArrayList<EventReceiver>();
		
		// Starts the Scheduled Events Thread
		scheduledTimerTask = new ScheduledTask();
		scheduledTimerTask.start();
	}
	
	/**
	 * Constructor (Connects to the Specified IP Address / Port)
	 * @param deviceID
	 * @param commMode
	 * @param ipAddress
	 * @param port
	 * @param batteryMonitor
	 * @param promiscuous
	 */
	public DesktopGroupContextManager(String deviceID, CommManager.CommMode commMode, String ipAddress, int port, boolean promiscuous) 
	{
		// Calls the Base Constructor
		this(deviceID, promiscuous);
		
		this.commManager.connect(commMode, ipAddress, port);
	}
	
	/**
	 * Constructor (Connects to the Specified IP Address / Port, and Subscribes to the Specified Channel
	 * @param deviceID
	 * @param commMode
	 * @param ipAddress
	 * @param port
	 * @param channel
	 * @param batteryMonitor
	 * @param promiscuous
	 */
	public DesktopGroupContextManager(String deviceID, CommManager.CommMode commMode, String ipAddress, int port, String channel, boolean promiscuous)
	{
		this(deviceID, promiscuous);
		
		String connectionKey = this.commManager.connect(commMode, ipAddress, port);
		
		this.commManager.subscribe(connectionKey, channel);
	}
	
	/**
	 * Sends a Request for Context Information
	 */
	public void sendRequest(String contextType, int requestType, String[] deviceIDs, int refreshRate, String[] parameters)
	{
		super.sendRequest(contextType, requestType, deviceIDs, refreshRate, parameters);
		scheduledTimerTask.interrupt();
	}
		
	/**
	 * Cancels a Request for Context Information
	 */
	public void cancelRequest(String type)
	{
		super.cancelRequest(type);
	}
	
	// Debug -----------------------------------------------------------------------------------------------------
	/**
	 * Determines if GCM Should Print Debug Statements
	 * @param value
	 */
	public void setDebugMode(boolean value)
	{
		DEBUG = value;
	}
	
	public void addDebugFilter(String filterCategory)
	{
		if (!this.debugFilter.contains(filterCategory))
		{
			this.debugFilter.add(filterCategory);	
		}
	}
	
	public void removeDebugFilter(String filterCategory)
	{
		this.debugFilter.remove(filterCategory);
	}
	
	public void clearDebugFilter()
	{
		this.debugFilter.clear();
	}
	
	@Override
	public void log(String category, String s) 
	{	
		if (DEBUG || (debugFilter != null && debugFilter.contains(category)))
		{
			SimpleDateFormat sdf 	      = new SimpleDateFormat("MM-dd-yy @ HH:mm:ss");	
			String 			 outputString = sdf.format(new Date()) + ": [" + category + "]  " + s;
			
			System.out.println(outputString);	
		}
	}
	
	// Event Receiver --------------------------------------------------------------------------------------------
	public void registerEventReceiver(EventReceiver eventReceiver)
	{
		if (!eventReceivers.contains(eventReceiver))
		{
			eventReceivers.add(eventReceiver);
		}
	}
	
	public void removeEventReceiver(EventReceiver eventReceiver)
	{
		eventReceivers.remove(eventReceiver);
	}
	
	public void removeAllEventReceivers()
	{
		this.eventReceivers.clear();
	}
	
	// Events ----------------------------------------------------------------------------------------------------	
	@Override
	protected void onContextDataReceived(ContextData data, ContextRequest request) 
	{
		for (EventReceiver e : eventReceivers)
		{
			e.onContextData(data);
		}
	}
	
	@Override
	protected void onCapabilitySubscribe(ContextCapability capability) {
		for (EventReceiver e : eventReceivers)
		{
			e.onCapabilitySubscribe(capability);
		}
	}

	@Override
	protected void onCapabilityUnsubscribe(ContextCapability capability) {
		for (EventReceiver e : eventReceivers)
		{
			e.onCapabilityUnsubscribe(capability);
		}
	}

	protected void onSendingData(ContextData data)
	{
		
	}
	
	protected void onSendingRequest(ContextRequest request) 
	{

	};

	@Override
	protected void onRequestReceived(ContextRequest request) 
	{
		// TODO Auto-generated method stub	
	}
	
	@Override
	protected void onRequestTimeout(ContextRequest request, ContextCapability capability) {
		log(GroupContextManager.LOG_REQUEST, "Request timeout [" + request.getContextType() + "]: " + capability.getDeviceID());
	}

	@Override
	protected void onSubscriptionTimeout(ContextSubscriptionInfo subscription) {
		log(GroupContextManager.LOG_SUBSCRIPTION,"Subscription timeout [" + subscription.getContextType() + "]: " + subscription.getDeviceID());
	}

	@Override
	protected void onProviderSubscribe(ContextProvider provider) {
		// TODO Auto-generated method stub
	}

	@Override
	protected void onProviderUnsubscribe(ContextProvider provider) {
		// TODO Auto-generated method stub
	}	

	@Override
	protected void onCapabilityReceived(ContextCapability capability) {
		// TODO Auto-generated method stub
		
	}
		
	/**
	 * This Thread Continually Performs GCF Related Events
	 * @author adefreit
	 *
	 */
	private class ScheduledTask extends Thread
	{
		private boolean run;
		private long    scheduledTaskDelay;
		
		public ScheduledTask()
		{
			run    = false;
		}
		
		public void run() 
		{	
			run = true;
			
			while (run)
			{
				try
				{
					// Runs all Scheduled Tasks ONCE
					scheduledTaskDelay = runScheduledTasks();
					long newDelayTime  = Math.max(Math.min(scheduledTaskDelay, REQUEST_DELAY_TIME), 0);
					
					// Lets the Thread Sleep
					sleep(newDelayTime);
				}
				catch (Exception ex)
				{
					System.out.println("Coming out of sleep . . . YAWN");
				}
			}	
		}
	}


}
