package com.adefreitas.gcf;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import com.adefreitas.gcf.messages.ComputeInstruction;
import com.adefreitas.gcf.messages.ContextCapability;
import com.adefreitas.gcf.messages.ContextRequest;

public abstract class ContextProvider 
{	
	// Constants
	public static final String SPECIFY_CHANNEL = "CHANNEL";
	
	// Internal Context Variables
	private GroupContextManager 					 groupContextManager;
	private String 									 contextType;
	private String									 description;
	private boolean 								 sharable;
	private int 									 refreshRate;
	private Date 									 lastTransmission;
	private boolean									 subscriptionRequiredForCompute;
	private HashMap<String, ContextSubscriptionInfo> subscriptions;
	private ArrayList<String>						 accessList;
	private ArrayList<String>						 subscriptionParameters;
	
	/**
	 * Constructor.  Creates a Context Provider but does not add register it with the group context manager
	 * @param contextType 		  - The unique code representing this context ("LOC", "ACC").  Can be any length, but try to keep short.
	 * @param groupContextManager - A link to the group context manager
	 */
	public ContextProvider(String contextType, GroupContextManager groupContextManager)
	{
		this.contextType 		    		 = contextType;
		this.description					 = "Undefined Context Provider";
		this.groupContextManager    		 = groupContextManager;
		this.sharable 		        		 = true;
		this.subscriptionRequiredForCompute = true;
		this.subscriptions 		    		 = new HashMap<String, ContextSubscriptionInfo>();
		this.refreshRate 		    		 = 60000;
		this.lastTransmission       		 = new Date(0);
		this.accessList						 = new ArrayList<String>();
		this.subscriptionParameters 		 = new ArrayList<String>();
	}
	
	/**
	 * Constructor.  Creates a Context Provider (with a human readable description) but does not add register it with the group context manager
	 * @param contextType - The unique code representing this context ("LOC", "ACC").  Can be any length, but try to keep short.
	 * @param description - A human friendly description of what this context provider actually provides
	 * @param groupContextManager - A link to the group context manager
	 */
	public ContextProvider(String contextType, String description, GroupContextManager groupContextManager)
	{
		this(contextType, groupContextManager);
		this.description = description;
	}
	
	// PUBLIC METHODS ----------------------------------------------------------------------------------
	/**
	 * Sends Context to a Specific Subset of Devices
	 * @param destination
	 * @param context
	 */
	public void sendContext(String[] destination, String[] context)
	{
		String channel = this.getSubscriptionParameter(SPECIFY_CHANNEL);
		
		if (channel != null)
		{
			// Allows the Framework to Send the Message to the Desired Channel
			// WARNING:  This can cause repeats if devices share multiple channels
			//System.out.println("Sending to Channel " + channel + ": " + Arrays.toString(destination));
			this.getGroupContextManager().sendContext(channel, this.getContextType(), destination, context);
		}
		else
		{
			// Allows the Framework to Automatically Deliver the Message to the Device
			// WARNING:  This can result in repeated messages and/or network broadcasts!
			this.getGroupContextManager().sendContext(this.getContextType(), destination, context);
		}
	}
	
	/**
	 * Returns the ContextType for this specific provider
	 * @return A short string (e.g., "LOC" for location, "ACC" for accelerometer) representing the context that this provider can create/share.  A full description of context
	 * providers is provided on the GCF website.
	 */
	public String getContextType()
	{
		return contextType;
	}
	
	/**
	 * Returns the description for this provider
	 * @return A human friendly description about what context(s) this provider creates/shares
	 */
	public String getDescription()
	{
		return description;
	}
	
	/**
	 * Specifies whether or not the context provider is being used by a GCF device (this device, or another)
	 * @return TRUE if the context provider has at least one active subscription; FALSE otherwise
	 */
	public boolean isInUse() 
	{
		return (subscriptions.keySet().size() > 0);
	}

	/**
	 * Specifies whether or not the context provider can be shared with outside devices.  
	 * @return TRUE if the context provider has been marked as sharable; FALSE otherwise
	 */
	public boolean isSharable()
	{
		return sharable;
	}
	
	/**
	 * Controls whether or not this context provider should be shared.  By default, all context providers
	 * are set to be sharable when they are first registered by the GCM, so you need to call this AFTER it
	 * has been registered if you want to change it.
	 * @param sharable Set to TRUE if you want this provider to be sharable; FALSE if not
	 */
	public void setSharable(boolean sharable)
	{
		this.sharable = sharable;
	}
	
	/**
	 * Adds a specific GCF device to the access list
	 * @param deviceID The GCF ID of the device you want to add to the access list
	 */
	public void addToAccessList(String deviceID)
	{
		if (!accessList.contains(deviceID))
		{
			accessList.add(deviceID);
		}
	}
	
	/**
	 * Removes a specific GCF device to the access list
	 * @param deviceID The GCF ID of the device you want to add to the access list
	 */
	public void removeFromAccessList(String deviceID)
	{
		if (accessList.contains(deviceID))
		{
			accessList.remove(deviceID);
		}
	}
	
	/**
	 * Determines if the specified device has access to the context produced by this context provider.  IMPORTANT: this
	 * method will return TRUE if the access list is empty.
	 * @param deviceID The GCF ID of the device you want to check
	 * @return TRUE if the deviceID is in the access list (or the list is empty); FALSE otherwise
	 */
	public boolean hasAccess(String deviceID)
	{
		return (accessList.size() == 0 || accessList.contains(deviceID));
	}
	
	/**
	 * Specifies whether or not a device needs to be subscribed to this context provider in order to send it Compute Instructions.  
	 * This value is TRUE by default.
	 * @return TRUE if a subscription is required; FALSE otherwise
	 */
	public boolean isSubscriptionRequiredForCompute()
	{
		return subscriptionRequiredForCompute;
	}
	
	/**
	 * Allows you to specify whether or not a device needs to be subscribed to this context provider in order to send it Compute Instructions.
	 * @param newValue TRUE if you want subscriptions to be required; FALSE if you do not want subscriptions to be required
	 */
	public void setSubscriptionRequiredForCompute(boolean newValue)
	{
		subscriptionRequiredForCompute = newValue;
	}
		
	/**
	 * Specifies whether or not the provided device ID is subscribed to this context provider
	 * @param deviceID The deviceID to check
	 * @return TRUE if the device is currently subscribed; FALSE otherwise
	 */
	public boolean isSubscribed(String deviceID)
	{
		return subscriptions.containsKey(deviceID);
	}
	
	/**
	 * Specifies how many subscriptions this device is context provider currently has
	 * @return The number of subscriptions [0..*]
	 */
	public int getNumSubscriptions()
	{
		return subscriptions.size();
	}
		
	/**
	 * Specifies the rate at which this context provider should produce context
	 * @return The refresh rate (in ms)
	 */
	public int getRefreshRate()
	{
		return refreshRate;
	}
	
	/**
	 * Specifies all of the Unique Subscription Parameters provided by other devices.
	 * This allows a context provider to know all of the different behavior flags that have been set by different devices,
	 * however, it does not tell you which device requested what.
	 * @return An array containing all unique subscription parameters
	 */
	public String[] getSubscriptionParameters()
	{
		return subscriptionParameters.toArray(new String[0]);
	}
			
	/**
	 * Provides the contents of a single subscription parameter, provided that the parameter is stored in the format:
	 * PARAMETER_NAME=VALUE
	 * @param parameterName The parameter name being requested (PARAMETER_NAME)
	 * @return The parameter value (VALUE)
	 */
	public String getSubscriptionParameter(String parameterName)
	{
		for (String s : subscriptionParameters)
		{
			String[] elements = s.split("=");
			
			if (elements.length >= 2)
			{
				if (elements[0].trim().equals(parameterName.trim()))
				{
					return s.substring(s.indexOf("=") + 1);
				}	
			}
		}
		
		// Returns NULL if Nothing Found
		return null;
	}
	
	/**
	 * Specifies the Context Subscription Information for a specific device
	 * @param deviceID The GCF ID of the device
	 * @return An object containing all relevant subscription information
	 */
	public ContextSubscriptionInfo getSubscription(String deviceID)
	{
		return subscriptions.get(deviceID);
	}
	
	/**
	 * Specifies all active subscriptions for this context provider
	 * @return An array containing all subscriptions
	 */
	public ContextSubscriptionInfo[] getSubscriptions()
	{
		return subscriptions.values().toArray(new ContextSubscriptionInfo[0]);
	}
	
	/**
	 * Specifies the GCF device IDs of all devices that are currently subscribed
	 * @return An array containing all unique device IDs
	 */
	public String[] getSubscriptionDeviceIDs()
	{
		return subscriptions.keySet().toArray(new String[0]);
	}
			
	/**
	 * Returns the Group Context Manager that this context provider is associated with (not necessarily registered to).
	 * @return A reference to the group context manager
	 */
	public GroupContextManager getGroupContextManager()
	{
		return groupContextManager;
	}
	
	// PROTECTED METHODS ---------------------------------------------------------------------
	/**
	 * Used by the GCM to specify the last time this context provider sent information
	 * @param newDate The date (should be the current timestamp)
	 */
	protected void setLastTransmissionDate(Date newDate)
	{
		lastTransmission = newDate;
	}
	
	/**
	 * Specifies the last time this context provider sent information
	 * @return A date object containing the date of the last transmission
	 */
	protected Date getLastTransmissionDate()
	{
		return lastTransmission;
	}
	
	/**
	 * Registers a Subscription for context
	 * @param deviceID 		- the GCF device ID of the device subscribing to this context provider
	 * @param refreshRate 	- the rate (in ms) that the device expects to receive context
	 * @param heartbeatRate - the agreed upon heartbeat rate (the rate at which the device must resend its request in order to continue receiving context)
	 * @param parameters	- any specific parameters needed for this context request
	 */
	protected void addSubscription(String deviceID, int refreshRate, int heartbeatRate, String[] parameters)
	{
		if (!subscriptions.containsKey(deviceID) && hasAccess(deviceID))
		{
			subscriptions.put(deviceID, new ContextSubscriptionInfo(deviceID, contextType, refreshRate, heartbeatRate * 3, parameters));
			updateConfiguration();
			
			// Raises an Event
			onSubscription(subscriptions.get(deviceID));
		}
	}
	
	/**
	 * Removes a Subscription for context
	 * @param deviceID - the GCF device ID of the device
	 */
	protected void removeSubscription(String deviceID)
	{
		if (subscriptions.containsKey(deviceID))
		{
			ContextSubscriptionInfo subscription = subscriptions.get(deviceID);
			
			subscriptions.remove(deviceID);
			updateConfiguration();
			
			// Raises an Event
			onSubscriptionCancelation(subscription);
		}
	}
	
	// OVERRIDABLE METHODS -------------------------------------------------------------------
	/**
	 * Determines whether or not the context provider should send a capability message to this specific GCF device
	 * @param request The requesting device's Context Request message
	 * @return TRUE if the context provider should send a capability message back; FALSE otherwise
	 */
	public boolean sendCapability(ContextRequest request)
	{
		return canCollectContext() && hasAccess(request.getDeviceID());
	}
	
	/**
	 * Specifies the Rate at which devices should resend their request (before getting automatically unsubscribed)
	 * @param request - The Context Request of the device
	 * @return - the Heartbeat Rate (in ms)
	 */
	public int getHeartbeatRate(ContextRequest request)
	{
		return 60000;
	}
	
	/**
	 * Allows a Context Provider to Add Custom Configuration Settings
	 * @param capability
	 */
	public void setCapabilityParameters(ContextCapability capability)
	{

	}
	
	/**
	 * Adjusts the Settings of this Context Providers based on the Currently Subscribed devices
	 */
	public void updateConfiguration()
	{
		refreshRate = Integer.MAX_VALUE;
		subscriptionParameters.clear();
		
		// Examines Each Remaining Subscriptions
		// Determines Which Parameters to Add
		for (String id : subscriptions.keySet())
		{
			ContextSubscriptionInfo csi = subscriptions.get(id);
			
			// Populates Parameters
			for (String parameter : csi.getParameters())
			{
				if (!subscriptionParameters.contains(parameter))
				{
					subscriptionParameters.add(parameter);
				}
			}
			
			// Calculates the Minimum Refresh Rate
			refreshRate = Math.min(refreshRate, csi.getRefreshRate());
		}
	}

	/**
	 * Event that fires when a device subscribes
	 * @param newSubscription The subscription details for the requesting device
	 */
	public void onSubscription(ContextSubscriptionInfo newSubscription)
	{
		this.sendContext();
	}
	
	/**
	 * Event that fires when a device unsubscribes (intentionally, or automatically by the system)
	 * @param subscription The subscription details for the unsubscribing device
	 */
	public void onSubscriptionCancelation(ContextSubscriptionInfo subscription)
	{
		// Do Nothing by Default
	}
	
	/**
	 * Event that fires when the context provider receives a Compute Instruction from another device
	 * @param instruction The Compute Instruction from another device
	 */
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		// Does Nothing for Most Context Providers
	}
		
	/**
	 * Event that fires when the context provider receives a heartbeat (a context request)
	 * @param request The Context Request message
	 */
	public void onHeartbeat(ContextRequest request)
	{
		if (subscriptions.containsKey(request.getDeviceID()))
		{
			// Updates the Contact History of the Device
			ContextSubscriptionInfo csi = subscriptions.get(request.getDeviceID());
			csi.setLastContact(new Date());
			csi.setParameters(request.getPayload());
		}
	}
	
	/**
	 * This method allows us to restore a device to its default settings
	 */
	public void reboot()
	{
		subscriptions.clear();
		subscriptionParameters.clear();
		stop();
		updateConfiguration();
	}
	
	/**
	 * This method determines if the device can provide this context
	 * @return
	 */
	public boolean canCollectContext()
	{
		return true;
	}
	
	// ABSTRACT METHODS ----------------------------------------------------------------------
	/**
	 * This method is used to Start a context provider (initialize sensors).  It is called when the FIRST device subscribes to it
	 * (i.e., numSubscriptions goes from 0->1)
	 */
	public abstract void start();
	
	/**
	 * This method is used to Stop a context provider (turn off sensors).  It is called when the LAST device unsubscribes from it
	 * (i.e., numSubscriptions goes from 1->0)
	 */
	public abstract void stop();
	
	/**
	 * This method is used to evaluate the "goodness" of the context being produced by this context provider
	 * @param parameters - the Context Request parameters of the requesting device
	 * @return A measure of goodness (arbitrary for each context provider, but larger numbers equals higher fitness)
	 */
	public abstract double getFitness(String[] parameters);

	/**
	 * This method is used to send context to all subscribed devices.  It is automatically called by the GCM at the specified refresh rate
	 */
	public abstract void sendContext();
}
