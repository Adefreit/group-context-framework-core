package com.adefreitas.gcf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.arbiters.LocalSourceArbiter;
import com.adefreitas.gcf.arbiters.MultiSourceArbiter;
import com.adefreitas.gcf.arbiters.SingleSourceArbiter;
import com.adefreitas.gcf.messages.CommMessage;
import com.adefreitas.gcf.messages.ComputeInstruction;
import com.adefreitas.gcf.messages.ContextCapability;
import com.adefreitas.gcf.messages.ContextData;
import com.adefreitas.gcf.messages.ContextRequest;
import com.adefreitas.gcf.messages.ContextSubscription;

public abstract class GroupContextManager
{
	public static final double FRAMEWORK_VERSION = 0.96;
	public static enum DeviceType { Desktop, Laptop, Mobile, Sensor, Other };

	// Log Labels
	public static final String LOG_GENERAL 	       = "GCF";
	public static final String LOG_CAPABILITY      = "GCF-Capability";
	public static final String LOG_COMMUNICATIONS  = "GCF-Comm";	
	public static final String LOG_COMPARISON      = "GCF-Comparison";
	public static final String LOG_COMPUTE 	       = "GCF-Compute";
	public static final String LOG_ERROR  	       = "GCF-Error";
	public static final String LOG_PERFORMANCE     = "GCF-Performance";
	public static final String LOG_REQUEST         = "GCF-Request";
	public static final String LOG_SUBSCRIPTION    = "GCF-Subscription";
	public static final String LOG_TIMEOUT	       = "GCF-Timeout";
	
	// Constants
	public static final int    NO_SUBSCRIPTION_DELAY		 = 10000;
	public static final int    ADVERTISEMENT_DELAY 			 = 15000;
	public static final int    MIN_MESSAGE_VERSION 			 = 1;
	public static final double DEFAULT_WEIGHT_BATTERY 	     = 0.4;
	public static final double DEFAULT_WEIGHT_SENSOR_QUALITY = 0.2;
	public static final double DEFAULT_WEIGHT_IS_FOREIGN	 = 0.2;
	public static final double DEFAULT_WEIGHT_PROVIDING	     = 0.01;
	public static final double DEFAULT_WEIGHT_RELIABILITY    = 0.19;
		
	// Device Identifiers/Properties
	private String     deviceID;
	private DeviceType deviceType;
	
	// Determines if the GCM Runs in Promiscuous Mode (forwards all received data to application, regardless of subscription status)
	private boolean promiscuous;
	
	// Object Which Handles Broadcast Send/Receive Operations
	protected CommManager commManager;
	
	// Battery Statistics Monitor
	private BatteryMonitor batteryMonitor;
				
	// Registry of Available Context Providers Registered to this Device
	private HashMap<String, ContextProvider> providers;
	
	// Registry of Available Arbiters Used to Select Context Capabilities
	private HashMap<Integer, Arbiter> arbiters;
	
	// Keeps Track of All Requests Made by this Device
	private ArrayList<ContextRequest>     internalRequests;
	private HashMap<ContextRequest, Date> lastHeartbeat;
	
	// Unprocessed Capability Advertisements (Clears Every Time Scheduled Tasks Runs)
	private ArrayList<ContextCapability> receivedCapabilities;
	
	// Keeps Track of Capabilities that this Device has Subscribed to
	private HashMap<ContextRequest, ArrayList<ContextCapability>> subscribedCapabilities;
	
	// Tracks Reliability Statistics for all Devices that this Device has Subscribed to in the Past
	private HashMap<String, ContextReliabilityInfo> reliabilityInfo;
	
	/**
	 * Constructor
	 * @param parentActivityHandler - the Handler where all Context Messages will be Forwarded To
	 * @param outputHandler - the Handler which allows the GCM to send debug messages to the app (will be removed)
	 * @param deviceID - the unique ID representing this device
	 */
	public GroupContextManager(String deviceID, DeviceType deviceType, BatteryMonitor batteryMonitor, boolean promiscuous)
	{
		// Sets the Name of this Device (or creates one if NULL)
		this.deviceID = (deviceID != null) ? deviceID : "GCF_" + deviceType.toString() + "_" + System.currentTimeMillis();
		
		// Stats About the Device
		this.deviceType 	= deviceType;
		this.batteryMonitor = batteryMonitor;
		this.promiscuous    = promiscuous;
				
		// Creates the Registry of Available Context Providers
		providers = new HashMap<String, ContextProvider>();
		
		// Creates the Registry of Available Arbiters
		arbiters = new HashMap<Integer, Arbiter>();
		arbiters.put(ContextRequest.SINGLE_SOURCE,   new SingleSourceArbiter(ContextRequest.SINGLE_SOURCE));
		arbiters.put(ContextRequest.MULTIPLE_SOURCE, new MultiSourceArbiter(ContextRequest.MULTIPLE_SOURCE));
		arbiters.put(ContextRequest.LOCAL_ONLY, 	 new LocalSourceArbiter(ContextRequest.LOCAL_ONLY));
		
		// Creates the List of Requests
		internalRequests = new ArrayList<ContextRequest>();
		lastHeartbeat    = new HashMap<ContextRequest, Date>();
		
		// Creates the List of Subscribed Capabilities
		subscribedCapabilities = new HashMap<ContextRequest, ArrayList<ContextCapability>>();
		
		// Creates the List of Advertised (and Unprocessed) Capabilities
		receivedCapabilities = new ArrayList<ContextCapability>();
				
		// Creates the Registry of Unreliable Capabilities
		reliabilityInfo = new HashMap<String, ContextReliabilityInfo>();
	
		log(LOG_GENERAL, "Group Context Manager Created (v" + FRAMEWORK_VERSION + ")");
	}
		
	// CLASS METHODS --------------------------------------------------------------------------------------------------------
	/**
	 * Retrieves what the GCM Believes the Device's ID Is
	 * @return
	 */
	public String getDeviceID()
	{
		return deviceID;
	}
	
	/**
	 * Sets the Device ID
	 * WARNING:  This will Reset All Context Subscriptions
	 * @param newDeviceID
	 */
	public void setDeviceID(String newDeviceID)
	{
		if (!newDeviceID.equals(deviceID))
		{
			log(LOG_GENERAL, "Renaming GCM [" + deviceID + "] to [" + newDeviceID + "]");
			ArrayList<ContextRequest> requests = new ArrayList<ContextRequest>(internalRequests);
			
			// Kills All Context Requests
			for (ContextRequest r : requests)
			{
				this.cancelRequest(r.getContextType());
			}
			
			// Deletes All Active Subscriptions
			for (ContextProvider provider : providers.values())
			{
				provider.reboot();
			}
			
			// Changes the Name
			this.deviceID = newDeviceID;
			
			// Restarts the Request Process
			for (ContextRequest r : requests)
			{
				this.sendRequest(
						r.getContextType(), 
						r.getRequestType(), 
						r.getRefreshRate(), 
						r.getBatteryWeight(), 
						r.getSensorFitnessWeight(), 
						r.getForeignWeight(), 
						r.getProvidingWeight(), 
						r.getReliabilityWeight(), 
						r.getPayload(),
						r.getDestination());
			}	
		}
	}
	
	/**
	 * Retrieves the GCM's Battery Percentage Estimate
	 * @return
	 */
	public BatteryMonitor getBatteryMonitor()
	{
		return batteryMonitor;
	}
	
	/**
	 * Retreives Statistics about a Device's Past Reliability
	 * @param deviceID The deviceID of the device in question
	 * @return An object containing reliability information
	 */
	public ContextReliabilityInfo getContextReliabilityInfo(String deviceID)
	{
		if (reliabilityInfo.containsKey(deviceID))
		{
			return reliabilityInfo.get(deviceID);
		}
		else
		{
			return null;
		}
	}
	
	/**
	 * Retrieves the Arbiter for a Given Request Type
	 * @param requestType
	 * @return
	 */
	public Arbiter getArbiter(int requestType)
	{
		return arbiters.get(requestType);
	}
	
	// COMPUTE INSTRUCTIONS ----------------------------------------------------------------------------------------------------
	/**
	 * Sends a Compute Instruction to all Subscribed Devices
	 * @param contextType
	 * @param command
	 * @param instructions
	 */
	public void sendComputeInstruction(String contextType, String command, String[] instructions)
	{		
		this.log(LOG_COMPUTE, "Attempting to send " + command + " " + Arrays.toString(instructions));
		
		ContextRequest request = this.getRequest(contextType);
		
		if (request != null)
		{
			// Only allows a compute instruction to be sent if an active subscription exists
			if (subscribedCapabilities.containsKey(request))
			{
				ArrayList<String> destinationIDs = new ArrayList<String>();
				
				//System.out.println("Subscribed to " + subscribedCapabilities.get(request).size() + " providers for " + request.getContextType());
				
				for (ContextCapability capability : subscribedCapabilities.get(request))
				{
					destinationIDs.add(capability.getDeviceID());
				}	

				ComputeInstruction instruction = new ComputeInstruction(contextType, deviceID, destinationIDs.toArray(new String[0]), command, instructions);
				commManager.send(instruction);
			}
			else
			{
				this.log(LOG_COMPUTE, "Could not deliver compute instruction for " + contextType + ".  No Subscriptions.");
			}
		}
		else
		{
			this.log(LOG_COMPUTE, "Could not deliver compute instruction for " + contextType + ".  No Request for this Context Type.");
		}
	}
	
	/**
	 * Sends a Compute Instruction to Specific Devices
	 * WARNING:  Will Send Regardless of Subscription
	 * @param contextType
	 * @param destinationDevices
	 * @param command
	 * @param instructions
	 */
	public void sendComputeInstruction(String contextType, String[] destinationDevices, String command, String[] instructions)
	{	
		ComputeInstruction instruction = new ComputeInstruction(contextType, deviceID, destinationDevices, command, instructions);
		commManager.send(instruction);
	}

	/**
	 * Sends a Compute Instruction to Specified Devices on a Communications Socket
	 * WARNING:  Transmits Regardless of Whether or Not they are Subscribed
	 * @param connectionKey
	 * @param contextType
	 * @param destinationDevices
	 * @param command
	 * @param instructions
	 */
	public void sendComputeInstruction(String connectionKey, String contextType, String[] destinationDevices, String command, String[] instructions)
	{
		ComputeInstruction instruction = new ComputeInstruction(contextType, deviceID, destinationDevices, command, instructions);
		commManager.send(instruction, connectionKey);
	}
	
	/**
	 * Sends a Compute Instruction to Specified Devices on a Communications Socket AND Channel
	 * WARNING:  Transmits Regardless of Whether or Not they are Subscribed
	 * @param connectionKey
	 * @param channel
	 * @param contextType
	 * @param destinationDevices
	 * @param command
	 * @param instructions
	 */
	public void sendComputeInstruction(String connectionKey, String channel, String contextType, String[] destinationDevices, String command, String[] instructions)
	{
		ComputeInstruction instruction = new ComputeInstruction(contextType, deviceID, destinationDevices, command, instructions);
		commManager.send(instruction, connectionKey, channel);
	}
	
	// CONTEXT DATA ------------------------------------------------------------------------------------------------------------
 	/**
 	 * Broadcasts Context Information to Specific Devices
	 * @param contextType - the kind of context information being transmitted (i.e. GPS, Accelerometer, etc)
	 * @param description - a short description the data contained within this message (not officially used) 
	 * @param destinations - a list of the GCF device IDs that this message is intended for
	 * @param value - the context values
 	 */
 	public void sendContext(String contextType, String[] destinations, String[] value)
 	{
 		ContextData data = new ContextData(contextType, deviceID, destinations, value);
 		
 		// Throws an Event
 		this.onSendingData(data);
 		
 		// Actually Sends the Information
 		commManager.send(data);
 	}
 	
 	public void sendContext(String connectionKey, String channel, String contextType, String[] destinations, String[] value)
 	{
 		ContextData data = new ContextData(contextType, deviceID, destinations, value);
 		
 		// Throws an Event
 		this.onSendingData(data);
 		
 		// Actually Sends the Information
 		commManager.send(data, connectionKey, channel);
 	}
 	
 	public void sendContext(String channel, String contextType, String[] destinations, String[] value)
 	{
 		ContextData data = new ContextData(contextType, deviceID, destinations, value);
 		
 		// Throws an Event
 	 	this.onSendingData(data);
 	 	
 	 	// Actually Sends the Information
 	 	commManager.sendUsingChannel(data, channel);
 	}
 	
 	// CAPABILITY --------------------------------------------------------------------------------------------------------------
	/**
	 * Broadcasts the Device's Capability to Provide a Single Context
	 * @param type - The type of Context this device can provide
	 * @param parameters - Details Regarding this Particular Context Type
	 * @param fitness - A numerical value representing how well suited the device is to satisfy the request
	 */
	protected void sendCapability(ContextProvider provider, ContextRequest request)
	{
		double fitness = provider.getFitness(request.getPayload());
		
		// Creates the Context Capability Message
		ContextCapability capability = new ContextCapability(deviceID, deviceType.toString(), 
				request.getContextType(), provider.getHeartbeatRate(request), batteryMonitor.getBatteryTimeRemaining(), 
				fitness, provider.isInUse(), new String[] { request.getDeviceID() });
			
		// Allows the Provider to add Custom Parameters if Necessary
		provider.setCapabilityParameters(capability);
			
		// Transmits the Capability
		commManager.send(capability);
	}
	
	/**
	 * Subscribes to One or More Devices
	 * @param updateType SUBSCRIBE (ContextSubscription.SubscriptionUpdateType.Subscribe) or UNSUBSCRIBE (ContextSubscription.SubscriptionUpdateType.Unsubscribe)
	 * @param request The Context Request associated with this subscription update
	 * @param capabilities The specific context capabilities to subscribe/unsubscribe to
	 */
	protected void sendSubscriptions(ContextSubscription.SubscriptionUpdateType updateType, ContextRequest request, ContextCapability[] capabilities)
	{
		ArrayList<String> deviceIDs     = new ArrayList<String>();
		int 			  heartbeatRate = Integer.MAX_VALUE;
		
		if (updateType.equals(ContextSubscription.SubscriptionUpdateType.Subscribe))
		{	
			// Creates the List if it does not already exist
			if (!subscribedCapabilities.containsKey(request))
			{
				subscribedCapabilities.put(request, new ArrayList<ContextCapability>());
			}
			
			for (ContextCapability capability : capabilities)
			{
				if (!deviceIDs.contains(capability.getDeviceID()))
				{
					deviceIDs.add(capability.getDeviceID());
					heartbeatRate = Math.min(heartbeatRate, capability.getHeartbeatRate());
				}
				
				// Adds the Capability to the List of Subscribed Capabilities
				if (!subscribedCapabilities.get(request).contains(capability))
				{
					subscribedCapabilities.get(request).add(capability);
				}
					
				// Remembers when the message was sent for timeout purposes
				ContextReliabilityInfo cri = reliabilityInfo.get(capability.getDeviceID());
				cri.setSubscriptionStartDate(capability.getContextType());
							
				log(LOG_SUBSCRIPTION, "Subscribing to " + capability.getDeviceID() + "'s " + request.getContextType() + " provider.");
				onCapabilitySubscribe(capability);
			}
		}
		else if (updateType.equals(ContextSubscription.SubscriptionUpdateType.Unsubscribe))
		{
			for (ContextCapability capability : capabilities)
			{
				if (!deviceIDs.contains(capability.getDeviceID()))
				{
					deviceIDs.add(capability.getDeviceID());
					heartbeatRate = Math.min(heartbeatRate, capability.getHeartbeatRate());
				}
				
				if (subscribedCapabilities.containsKey(request))
				{
					// Removes this Capability to the List of Subscribed Capabilities
					subscribedCapabilities.get(request).remove(capability);
						
					// Removes the List Entirely if it is Empty
					if (subscribedCapabilities.get(request).size() == 0)
					{
						subscribedCapabilities.remove(request);
					}
				}
				
				log(LOG_SUBSCRIPTION, "Unsubscribing to " + capability.getDeviceID() + "'s " + request.getContextType() + " provider.");
				onCapabilityUnsubscribe(capability);	
			}
		}
		
		// Creates and Sends the Subscription Message
		ContextSubscription subscriptionUpdate = new ContextSubscription(updateType, deviceID, deviceIDs.toArray(new String[0]), request.getContextType(), request.getRefreshRate(), heartbeatRate, request.getPayload());
		commManager.send(subscriptionUpdate);
	}
	
	// REQUESTS --------------------------------------------------------------------------------------------------------------	
	/**
	 * Broadcasts a Request for Context Information from Specific Devices
	 * @param contextType
	 * @param deviceID
	 * @param refreshRate
	 */
	public void sendRequest(String contextType, int requestType, String[] deviceIDs, int refreshRate, String[] parameters)
	{				
		// Sends the Request using the Default Weights
		sendRequest(contextType, 
				requestType, 
				refreshRate, 
				DEFAULT_WEIGHT_BATTERY,
				DEFAULT_WEIGHT_SENSOR_QUALITY, 
				DEFAULT_WEIGHT_IS_FOREIGN, 
				DEFAULT_WEIGHT_PROVIDING, 
				DEFAULT_WEIGHT_RELIABILITY, 
				parameters,
				deviceIDs);
	}
	
	/**
	 * Broadcasts a Request for Context Information Using the Provided Weights
	 * @param contextType
	 * @param optimizable
	 * @param refreshRate
	 * @param w_battery
	 * @param w_sensorFitness
	 * @param w_foreign
	 * @param w_providing
	 * @param w_reliability
	 * @param parameters
	 */
	public void sendRequest(String contextType, int requestType, int refreshRate, double w_battery, double w_sensorFitness, double w_foreign, double w_providing, double w_reliability, String[] parameters, String[] deviceIDs)
	{
		// Sets the Destination on Context Request Messages
		if (requestType == ContextRequest.LOCAL_ONLY)
		{
			deviceIDs = new String[] { this.getDeviceID() };
		}
		
		// Gets the Existing Request if it Exists
		ContextRequest request = this.getRequest(contextType);
		
		// This either adds a new request to memory, OR updates the existing one
		if (request == null)
		{			
			// Creates the Context Request
			request = new ContextRequest(deviceID, contextType, requestType, deviceIDs, refreshRate, w_battery, w_sensorFitness, w_foreign, w_providing, w_reliability, parameters);
			
			// Adds the Request to the System
			internalRequests.add(request);
		}
		else
		{
			// TODO:  Update the Request
			request.setDestination(deviceIDs);
		}
		
		// Removes Devices that are Currently Subscribed, but Not in the Devices List
		if (subscribedCapabilities.containsKey(contextType))
		{	
			for (ContextCapability capability : subscribedCapabilities.get(contextType))
			{
				if (!request.isDestination(capability.getDeviceID()))
				{
					cancelRequest(request, capability, false);
				}
			}
		}
		
		// Checks to make sure the comm manager exists before trying to send it
		if (commManager != null)
		{			
			// Sends Request
			commManager.send(request);
		}
		else
		{
			log(LOG_ERROR, "Communications Manager is NULL");
		}
	}
	
	/**
	 * Retrieves a list of all Requests made by this Device
	 * @return - a list of all requests currently being tracked by this device
	 */
	public ArrayList<ContextRequest> getRequests()
	{
		return new ArrayList<ContextRequest>(internalRequests);
	}
	
	/**
	 * Returns a Request for a Specific Context
	 * @param contextType
	 * @return
	 */
	public ContextRequest getRequest(String contextType)
	{
		for (ContextRequest r : internalRequests)
		{
			if (r.getContextType().equals(contextType))
			{
				return r;
			}
		}
		
		return null;
	}
	
	/**
	 * Removes a Request for Context Data from the Queue
	 * @param contextType the type of context ("LOC")
	 */
	public void cancelRequest(String type)
	{	
		ContextRequest request = this.getRequest(type);
		
		if (request != null)
		{
			cancelRequest(request, false);
		}
	}
	
	/**
	 * Cancels an Entire Request and Unsubscribes from any Subscribed Capabilities
	 * @param request the Request to cancel
	 * @param dueToTimeout TRUE if this cancelation was caused by a timeout; FALSE otherwise
	 */
	private void cancelRequest(ContextRequest request, boolean dueToTimeout)
	{	
		// Cancels any Existing Subscription(s) Tied to this Request
		if (subscribedCapabilities.containsKey(request))
		{
			for (ContextCapability capability : new ArrayList<ContextCapability>(subscribedCapabilities.get(request)))
			{
				cancelRequest(request, capability, dueToTimeout);
			}
		}
		
		// Removes the Request
		if (internalRequests.contains(request))
		{
			internalRequests.remove(request);	
		}
				
		log(LOG_GENERAL, "Request (" + request.getContextType() + ") Canceled.");
	}
	
	/**
	 * Cancels a Subscription to a Currently Subscribed Capability
	 * @param request
	 * @param capability
	 * @param dueToTimeout
	 */
	private void cancelRequest(ContextRequest request, ContextCapability capability, boolean dueToTimeout)
	{	
		// If Currently Subscribed, Unsubscribes from the Capability
		if (subscribedCapabilities.containsKey(request) && subscribedCapabilities.get(request).contains(capability))
		{
			sendSubscriptions(ContextSubscription.SubscriptionUpdateType.Unsubscribe, request, new ContextCapability[] { capability });	
		}
			
		// Performs Cleanup on the List of Subscribed Capabilities
		if (!subscribedCapabilities.containsKey(request))
		{
			internalRequests.remove(request);
		}
		
		// Performs Cleanup on the Heartbeat Checker
		lastHeartbeat.remove(request);
		
		// Remembers if this Cancellation was Due to a Provider Timeout
		if (dueToTimeout)
		{
			// Adds an Entry for this Timeout
			reliabilityInfo.get(capability.getDeviceID()).addContextTimeout(capability.getContextType());
		}
	}

	// CONTEXT PROVIDER METHODS -----------------------------------------------------------------------------------------------
	/**
	 * Registers Up a Context Provider Which is to be Shared with Other Devices
	 * @param contextType
	 * @param provider
	 */
	public void registerContextProvider(ContextProvider provider)
	{
		if (!providers.containsKey(provider.getContextType()))
		{
			providers.put(provider.getContextType(), provider);
			log(LOG_GENERAL, "Provider " + provider.getContextType() + " successfully registered (" + providers.size() + " total)");
		}
		else
		{
			log(LOG_ERROR, "Provider " + provider.getContextType() + " could not be added:  Duplicate Key");
		}
	}
	
	/**
	 * Unregisters a Context Provider (Effectively Preventing it from being Shared with Other Devices).
	 * Note:  You can also set a context provider so that it is not sharable without having to use this method.
	 * @param type
	 */
	public void unregisterContextProvider(String type)
	{
		if (providers.containsKey(type))
		{
			providers.remove(type);
			log(LOG_GENERAL, "Provider " + type + " successfully unregistered");
		}
		else
		{
			log(LOG_ERROR, "Provider " + type + " could not be unregistered:  No Such Key Found");
		}
	}

	/**
	 * Returns the Context Provider with the Specified Context Type
	 * @param type
	 * @return
	 */
	public ContextProvider getContextProvider(String type)
	{
		return providers.get(type);
	}
	
	/**
	 * Retrieves All Context Providers Registered to this Object
	 * @return
	 */
	public ContextProvider[] getRegisteredProviders()
	{
		return providers.values().toArray(new ContextProvider[0]);
	}
	
	// ARBITER METHODS --------------------------------------------------------------------------------------------------------
	/**
	 * Registers an Arbiter Used to Pick Context Capability
	 * @param arbiter
	 * @param requestType
	 */
	public void registerArbiter(Arbiter arbiter)
	{
		if (!arbiters.containsKey(arbiter.getRequestType()))
		{
			arbiters.put(arbiter.getRequestType(), arbiter);
			log(LOG_GENERAL, "Arbiter " + arbiter.getRequestType() + " successfully registered (" + arbiters.size() + " total)");
		}
		else
		{
			log(LOG_ERROR, "Arbiter " + arbiter.getRequestType() + " could not be added:  Duplicate Key");
		}
	}
	
	/**
	 * Removes an Arbiter
	 * @param requestType
	 */
	public void unregisterArbiter(int requestType)
	{
		if (arbiters.containsKey(requestType))
		{
			arbiters.remove(requestType);
			log(LOG_GENERAL, "Arbiter for request type " + requestType + " successfully unregistered (" + arbiters.size() + " remaining)");
		}
		else
		{
			log(LOG_ERROR, "Provider " + requestType + " could not be removed:  No Request Type Found");
		}
	}
		
	// COMMUNICATIONS METHODS -----------------------------------------------------------------------------
	/**
	 * Creates a Communications Object for a Single Socket.  Will not create duplicate sockets.
	 * @param communicationsMode
	 * @param ipAddress
	 * @param port
	 * @return - A Unique KEY representing this comm thread
	 */
	public String connect(CommMode communicationsMode, String ipAddress, int port)
	{
		String connectionKey = null;
		
		if (commManager != null)
		{
			connectionKey = this.commManager.connect(communicationsMode, ipAddress, port);
		}

		return connectionKey;
	}
	
	/**
	 * Returns the Communications Thread Key for the Specified Mode, Ip Address, and Port
	 * @param communicationsMode
	 * @param ipAddress
	 * @param port
	 * @return
	 */
	public String getConnectionKey(CommMode communicationsMode, String ipAddress, int port)
	{
		return this.commManager.getCommThreadKey(communicationsMode, ipAddress, port);
	}
	
	/**
	 * Checks to See if a Communications Channel is Open
	 * @param communicationsMode
	 * @param ipAddress
	 * @param port
	 * @return
	 */
	public boolean isConnected(CommMode communicationsMode, String ipAddress, int port)
	{
		if (commManager != null)
		{
			return commManager.isConnected(communicationsMode, ipAddress, port);
		}
		else
		{
			log(LOG_ERROR, "No Comm Manager Initialized");
		}
		
		return false;
	}
		
	/**
	 * Subscribes to a single channel.
	 * @param connectionKey
	 * @param channel
	 * @return
	 */
	public boolean subscribe(String connectionKey, String channel)
	{
		if (commManager != null)
		{
			//System.out.println("Attempting to subscribe to " + channel);
			return commManager.subscribe(connectionKey, channel);
		}
		else
		{
			System.out.println("No Comm Manager Initialized");
		}
		
		// If the code makes it here, the subscription failed.
		return false;
	}

	/**
	 * Unsubscribes from a single channel
	 * @param connectionKey
	 * @param channel
	 * @return
	 */
	public boolean unsubscribe(String connectionKey, String channel)
	{
		if (commManager != null)
		{
			return commManager.unsubscribe(connectionKey, channel);
		}
		else
		{
			System.out.println("No Comm Manager Initialized");
		}
		
		// If the code makes it here, the unsubscription failed.
		return false;
	}
	
	/**
	 * Disconnects ALL Comm Threads at Once
	 */
	public void disconnect()
	{
		if (commManager != null)
		{
			// Disconnects ALL of the threads at once
			this.commManager.disconnect();
		}
	}
	
	/**
	 * Disconnects a Specific Comm Thread
	 * @param connectionKey
	 */
	public void disconnect(String connectionKey)
	{
		if (commManager != null)
		{
			this.commManager.disconnect(connectionKey);	
		}
	}
	
	// MESSAGE PROCESSING --------------------------------------------------------------------------------------------------
	/**
	 * Processes Messages Received from the Communications Thread
	 * @param message - the message to be processed
	 */
	public void onMessage(CommMessage message) 
	{	
		// Makes Sure that the Message Complies with Version Limitations
		if (message.getMessageVersion() >= MIN_MESSAGE_VERSION)
		{
			// Makes Sure that the Message is Intended for this Device
			if (message.isDestination(deviceID))
			{
				if (message instanceof ContextData)
				{
					onContextDataMessage((ContextData)message);
				}
				else if (message instanceof ContextRequest)
				{
					onContextRequestMessage((ContextRequest)message);
				}
				else if (message instanceof ContextCapability)
				{
					onContextCapabilityMessage((ContextCapability)message);
				}
				else if (message instanceof ContextSubscription)
				{
					onContextSubscriptionMessage((ContextSubscription)message);
				}
				else if (message instanceof ComputeInstruction)
				{
					onComputeInstructionMessage((ComputeInstruction)message);
				}
				else if (message != null)
				{
					log(LOG_ERROR, "Unhandled Message Type:  " + message.getMessageType());
				}		
			}
			else if (message instanceof ContextData && this.promiscuous)
			{
				onContextDataMessage((ContextData)message);
			}
		}
		else
		{
			log(LOG_ERROR, "Encountered Deprecated Message (Min API = " + MIN_MESSAGE_VERSION + ")");
		}
	}

	/**
	 * Processes Context Data Received from Other Devices
	 * @param data
	 */
	private void onContextDataMessage(ContextData data) 
	{
		// Flag Used to Track Whether the Device Requested this Data
		boolean requestMatch = false;
		
		// Determines if this Device Requested the Data
		for (ContextRequest r : internalRequests)
		{
			if (r.getContextType().equals(data.getContextType()) && subscribedCapabilities.containsKey(r))
			{
				for (ContextCapability subscribedCapability : subscribedCapabilities.get(r))
				{
					if (data.getDeviceID().equals(subscribedCapability.getDeviceID()))
					{
						requestMatch = true;
						
						// Update Reliability Info to Show that this Device Recently Contacted Us
						reliabilityInfo.get(subscribedCapability.getDeviceID()).addDataUpdate(data.getContextType());
						
						// Sends the Data to the Application and Breaks from the Loop
						// This will send the data even if 0 bytes were delivered
						// This will also check to make sure that the context was created for this device
						if (data.getDestination().length == 0)
						{
							// If Not Specified, Assume Message is for Everyone
							onContextDataReceived(data, r);
						}
						else
						{
							// If Destination IS Specified, Check to Make Sure Device ID Matches
							if (data.isDestination(deviceID))
							{
								onContextDataReceived(data, r);
								break;
							}

//							for (String destinationDeviceID : data.getDestination())
//							{
//								if (destinationDeviceID.equals(deviceID))
//								{
//									onContextDataReceived(data, r);
//									break;
//								}
//							}
						}
						
						break;
					}
				}	
			}
			
			if (requestMatch)
			{
				break;
			}
		}
			
		// Still Forwards the Data to the Application if GCM is Set to Promiscuous
		if (promiscuous && !requestMatch)
		{
			//System.out.println("Promiscuous Mode: " + data);
			onContextDataReceived(data, null);
		}	
	}

	/**
	 * Processes Context Requests
	 * @param request
	 */
	private void onContextRequestMessage(ContextRequest request)
	{
		log(LOG_REQUEST, request.toString());
			
		// Allows the Application to See Context Requests from Other Devices
		if (!request.getDeviceID().equals(deviceID))
		{
			this.onRequestReceived(request);	
		}
		
		// Determines if this GCM has a Registered Context Provider of the Specified Type
		if (providers.containsKey(request.getContextType()))
		{			
			// Grabs the Context Provider
			ContextProvider provider = providers.get(request.getContextType());
			
			// Only Processes the Request under the following conditions
			// 1.  The provider is set to share context data with other devices (ISSHARABLE = TRUE)
			// 2.  The request is from the device hosting the context provider
			// 3.  The requesting device is already subscribed
			if (provider.isSharable() || (request.getDeviceID().equals(deviceID) || provider.isSubscribed(deviceID)))
			{
				// Registers the Heartbeat if Already Subscribed
				if (provider.isSubscribed(request.getDeviceID()))
				{
					provider.onHeartbeat(request);
				}

				// Formulates the Provider Capability Advertisement
				if (provider.sendCapability(request))
				{	
					// Broadcasts Capability/Fitness to Recipient
					sendCapability(provider, request);	
				}
			}
		}
	}

	/**
	 * Processes Context Capabilities Provided by Other Devices
	 * @param capability
	 */
	private void onContextCapabilityMessage(ContextCapability capability)
	{
		log(LOG_CAPABILITY, "Received " + capability);
		
		// Creates a Reliability Info Record for this Capability/Device
		if (!reliabilityInfo.containsKey(capability.getDeviceID()))
		{
			reliabilityInfo.put(capability.getDeviceID(), new ContextReliabilityInfo(capability.getDeviceID()));
		}
		
		// Removes Any Identical or Expired Capabilities
		for (ContextCapability advertisedCapability : new ArrayList<ContextCapability>(receivedCapabilities))
		{
			if ((capability.getDeviceID().equals(advertisedCapability.getDeviceID()) && 
				 capability.getContextType().equals(advertisedCapability.getContextType())))
			{
				//log(LOG_CAPABILITY, "Found Older Entry . . . Removing " + advertisedCapability);
				receivedCapabilities.remove(advertisedCapability);
			}
		}
		
		// Adds the Capability to the List of Capabilities
		receivedCapabilities.add(capability);
		
		// Raises the Event
		this.onCapabilityReceived(capability);
	}

	/**
	 * Processes Requests to Subscribe to a Context Provider from this Device
	 * @param subscription
	 */
	private void onContextSubscriptionMessage(ContextSubscription subscription)
	{
		// Makes Sure that the Subscription Request is Intended for this Device (or ALL Devices)
		if (subscription.isDestination(deviceID))
		{	
			// Looks for the Context Provider this Subscription is For (if it Exists)
			if (providers.containsKey(subscription.getContextType()))
			{
				ContextProvider provider = providers.get(subscription.getContextType());
				
				// Saves the Previous Number of Subscriptions
				int oldSubscriptionCount = provider.getNumSubscriptions();
				
				// A Subscription Message Can Be One of Multiple Types; This Code Handles Each Type
				if (subscription.getUpdateType() == ContextSubscription.SubscriptionUpdateType.Subscribe)
				{	
					int numSubscriptions = provider.getNumSubscriptions();
					
					// Adds the Subscribing Device (Assuming it is not already added).
					if (!provider.isSubscribed(subscription.getDeviceID()))
					{
						log(LOG_SUBSCRIPTION, subscription.getDeviceID() + " subscribing to " + subscription.getContextType());
						
						provider.addSubscription(subscription.getDeviceID(), subscription.getRefreshRate(), subscription.getHeartbeatRate(), subscription.getPayload());	
						
						// Raises an Event
						onProviderSubscribe(provider);
					}
					else
					{
						log(LOG_SUBSCRIPTION, subscription.getDeviceID() + " sending duplicate subscription for " + subscription.getContextType());
						
						// Just Have the Provider Send Context Information Again
						provider.sendContext();
					}
					
					// Starts the Provider if No One is Currently Subscribed
					if (numSubscriptions == 0)
					{
						provider.start();
					}
				}
				else if (subscription.getUpdateType() == ContextSubscription.SubscriptionUpdateType.Unsubscribe)
				{
					// Examines the Subscription
					if (provider.isSubscribed(subscription.getDeviceID()))
					{
						log(LOG_SUBSCRIPTION, subscription.getDeviceID() + " unsubscribing from " + subscription.getContextType());
						
						// Removes the Device from the List of Subscribers
						provider.removeSubscription(subscription.getDeviceID());
					
						// Stops the Provider if No One is Currently Subscribed
						if (provider.getNumSubscriptions() == 0)
						{
							provider.stop();
						}
						
						// Raises an Event
						onProviderUnsubscribe(provider);
					}
				}
				else
				{
					log(LOG_ERROR, "Unhandled Subscription Type: " + subscription.getUpdateType());
				}		
				
				log(LOG_SUBSCRIPTION, String.format("Provider %s:  %d->%d subscription(s)", provider.getContextType(), oldSubscriptionCount, provider.getNumSubscriptions()));
			}
		}
	}

	/**
	 * Processes Compute Instructions for a Context Provider
	 * @param instruction
	 */
	private void onComputeInstructionMessage(ComputeInstruction instruction)
	{	
		// Looks for the Context Provider this Instruction is For (if it Exists)
		if (providers.containsKey(instruction.getContextType()))
		{
			//log(LOG_COMPUTE, "Looking for Provider: " + instruction.getContextType());
			ContextProvider provider = providers.get(instruction.getContextType());
			
			// Makes sure that the device is already subscribed to this device (or does not require a subscription), AND
			// that the Destination Device ID matches this device
			if ((!provider.isSubscriptionRequiredForCompute() || provider.isSubscribed(instruction.getDeviceID())) &&
				(instruction.isDestination(deviceID)))
			{
				//log(LOG_COMPUTE, instruction.getDeviceID() + " sending compute instructions to " + instruction.getContextType());
				provider.onComputeInstruction(instruction);
			}
			else
			{
				log(LOG_COMPUTE, "Ignoring Compute Instruction: " + instruction.getDeviceID());
			}
		}
		else
		{
			log(LOG_COMPUTE, "No Provider Found: " + instruction.getContextType());
		}
	}
	
	// SCHEDULED TASKS -----------------------------------------------------------------------------------------------------		
	/**
	 * Runs all Scheduled Tasks ONCE
	 * @return - the amount of time in milliseconds that it took to run all scheduled tasks
	 */
	public long runScheduledTasks()
	{		
		// Manages all Requests (and determines how long it took to do so)
		long startRequestTime 	   = new Date().getTime();
		long requestDelay 		   = manageRequests();
		long requestProcessingTime = new Date().getTime() - startRequestTime;
		
		// Manages all Subscriptions (and determines how long it took to do so)
		long startSubscriptionTime      = new Date().getTime();
		long subscriptionDelay          = manageSubscriptions();
		long subscriptionProcessingTime = new Date().getTime() - startSubscriptionTime;
		
		// Manages all Context Delivery (and determines how long it took to do so)
		long startContextDeliveryTime = new Date().getTime();
		long deliveryDelay 			  = deliverContext();
		long deliveryProcessingTime   = new Date().getTime() - startContextDeliveryTime;
		
		// Calculates How Long Until the Next Scheduled Task Should Occur
		long delay = Math.min(requestDelay, Math.min(deliveryDelay, subscriptionDelay)) - (requestProcessingTime + subscriptionProcessingTime);
		
		// Provides Warning if Performance is Slower than Expected
		log(LOG_PERFORMANCE, "SCHEDULED TASKS: (REQ = " + requestProcessingTime + " ms; SUB = " + subscriptionProcessingTime + " ms; CONTEXT = " + deliveryProcessingTime + " ms): " + delay + " ms until next action.");
		
		return delay;
	}
	
	/**
	 * Tracks Requests Made by this Device
	 */
	private int manageRequests()
	{
		Date currentTime = new Date();
		int  delayTime 	 = Integer.MAX_VALUE;
		
		// Examines Each Request
		for (ContextRequest request : new ArrayList<ContextRequest>(internalRequests))
		{	
			log(LOG_REQUEST, "Processing request for " + request.getContextType());
			
			boolean sendRequest = false;
						
			// Context Capability Evaluation
			if (receivedCapabilities.size() > 0)
			{				
				useArbiter(request);
			}
			
			// Determines the Last Time a Heartbeat was Sent for a Particular Request
			Date heartbeatDate = (lastHeartbeat.containsKey(request)) ? lastHeartbeat.get(request) : new Date(0);
			
			// Timeout Check for Subscribed Capabilities
			if (subscribedCapabilities.containsKey(request))
			{
				for (ContextCapability subscribedCapability : new ArrayList<ContextCapability>(subscribedCapabilities.get(request)))
				{
					// Pulls Up Reliability Data for this Provider
					ContextReliabilityInfo cri = reliabilityInfo.get(subscribedCapability.getDeviceID());
				
					// Calculates Time Since the Last Update (The time since the provider provided data)
					long timeSinceLastUpdate = currentTime.getTime() - cri.getLastContact(request.getContextType()).getTime();
					
					// Calculates Time Since the Last Heartbeat (The time this device transmitted a message to the provider)
					long timeSinceLastHeartbeat = currentTime.getTime() - heartbeatDate.getTime();
					
					// If the Request Has Not Received Data for a Long Time, Cancel the Subscription
					if (timeSinceLastUpdate > request.getTimeoutDuration())
					{
						onRequestTimeout(request, subscribedCapability);
						
						// Cancels the Current Request
						cancelRequest(request, subscribedCapability, true);
						log(LOG_REQUEST, "Resending request for " + request.getContextType() + " due to TIMEOUT (" + timeSinceLastUpdate + " ms)");
						sendRequest = true;
						delayTime = Math.min(delayTime, ADVERTISEMENT_DELAY);
					}		
					else
					{
						int timeoutDelay = (int)(request.getTimeoutDuration() - timeSinceLastUpdate);
						delayTime   	 = Math.min(delayTime, timeoutDelay);
					}
					
					// Determines if a Heartbeat Needs to be Sent
					if (timeSinceLastHeartbeat > subscribedCapability.getHeartbeatRate())
					{
						//log("GCM-Request", "Resending request for " + request.getContextType() + " due to HEARTBEAT (" + timeSinceLastHeartbeat    + " ms)");
						lastHeartbeat.remove(request);
						lastHeartbeat.put(request, new Date());
						sendRequest = true;
						delayTime   = Math.min(delayTime, subscribedCapability.getHeartbeatRate());
					}
					else
					{
						int heartbeatDelay = (int)(subscribedCapability.getHeartbeatRate() - timeSinceLastHeartbeat);
						delayTime   	   = Math.min(delayTime, heartbeatDelay);
					}
				}
			}
			else
			{
				log(LOG_REQUEST, "Resending request for " + request.getContextType() + " due to NO SUBSCRIPTION");
				sendRequest = true;
				delayTime = Math.min(delayTime, NO_SUBSCRIPTION_DELAY);
			}
			
			// Determines if we have to send a request
			if (sendRequest)
			{
				sendRequest(request.getContextType(), 
						request.getRequestType(), 
						request.getRefreshRate(), 
						request.getBatteryWeight(),
						request.getSensorFitnessWeight(), 
						request.getForeignWeight(), 
						request.getProvidingWeight(), 
						request.getReliabilityWeight(), 
						request.getPayload(), 
						request.getDestination());
			}
		}
		
		// Removes all Unused Capabilities from Consideration
		// Prevents list from growing if there are no active requests for a context
		receivedCapabilities.clear();
		
		return delayTime;
	}
	
	/**
	 * Uses the Assigned Arbiter with the Given Request
	 * @param request
	 */
	private void useArbiter(ContextRequest request)
	{		
		// Gets the Arbiter Associated with this Request Type
		Arbiter arbiter = arbiters.get(request.getRequestType());
		
		if (arbiter != null)
		{
			// The list of providers this device is already subscribed to
			ArrayList<ContextCapability> subscribed = (subscribedCapabilities.containsKey(request)) ? new ArrayList<ContextCapability>(subscribedCapabilities.get(request)) : new ArrayList<ContextCapability>();
			
			// The list of all providers that have provided an advertisement
			ArrayList<ContextCapability> candidates = new ArrayList<ContextCapability>();
			
			// The list of all providers that the arbiter has decided this device should describe to
			ArrayList<ContextCapability> toSubscribe;
			ArrayList<ContextCapability> capabilitiesToUnsubscribe = new ArrayList<ContextCapability>(); 
			
			// Populates the List of Candidates
			for (ContextCapability c : new ArrayList<ContextCapability>(receivedCapabilities))
			{
				if (c.getContextType().equals(request.getContextType()))
				{
					candidates.add(c);
					receivedCapabilities.remove(c);
				}
			}
			
			if (candidates.size() > 0)
			{
				// Has the Arbiter Determine Which Capabilities to Subscribed To
				toSubscribe = arbiter.selectCapability(request, this, new ArrayList<ContextCapability>(subscribed), new ArrayList<ContextCapability>(candidates));
				log(LOG_REQUEST, "Arbiter [" + arbiter.getRequestType() + "] examined " + candidates.size() + " entries, and returned " + toSubscribe.size() + " capabilities for " + request.getContextType());
			}
			else
			{
				// Maintains the current list of subscriptions
				toSubscribe = new ArrayList<ContextCapability>(subscribed);
				log(LOG_REQUEST, "No new capabilities for " + request.getContextType());
			}		
						
			for (ContextCapability s : subscribed)
			{
				boolean found = false;
				
				for (ContextCapability ts : new ArrayList<ContextCapability>(toSubscribe))
				{					
					// Updates the Capability Entry
					if (s.getDeviceID().equals(ts.getDeviceID()))
					{
						found = true;
						
						// This performs the Update
						subscribedCapabilities.get(request).remove(s);
						subscribedCapabilities.get(request).add(ts);
						
						// We're already subscribed.  No need to subscribe again
						toSubscribe.remove(ts);
						
						// TODO:  Need a way to do this, but capability messages need to say who is subscribed to them
						// Checks to see if we need to resend the subscription
						// Occurs if we are subscribed, but the provider doesn't think we are subscribed
						
						break;
					}
				}
				
				if (!found)
				{
					// Unsubscribes if the provider is not in the list selected by the Arbiter
					// TODO:  Verify that this won't cause accidental unsubscriptions if a provider is late!
					capabilitiesToUnsubscribe.add(s);
				}
			}
			
			// Sends ONE Message to Unsubscribe to Everything
			if (capabilitiesToUnsubscribe.size() > 0)
			{
				sendSubscriptions(ContextSubscription.SubscriptionUpdateType.Unsubscribe, request, capabilitiesToUnsubscribe.toArray(new ContextCapability[0]));
			}
						
			// Debug Statement
			log(LOG_REQUEST, "After processing, " + toSubscribe.size() + " capabilities have yet to be subscribed to.");
			
			// Subscribes to all Context Capabilities that Haven't Been Subscribed to Yet
			if (toSubscribe.size() > 0)
			{
				sendSubscriptions(ContextSubscription.SubscriptionUpdateType.Subscribe, request, toSubscribe.toArray(new ContextCapability[0]));
			}
		}
		else
		{
			log(LOG_REQUEST, "ERROR:  No Arbiter Found for Request " + request);
		}
	}
	
	/**
	 * Tracks Context Subscriptions for this Device
	 */
	private long manageSubscriptions()
	{
		String activeProviders = "";
		
		// Looks at Each Providers' Subscriptions, and Cancels After a Set Amount of Inactivity
		for (ContextProvider provider : providers.values())
		{	
			for (ContextSubscriptionInfo subscription : provider.getSubscriptions())
			{	
				// Checks to see if the requester sent a heartbeat within the allotted time
				if (subscription.isTimeout())
				{
					onSubscriptionTimeout(subscription);
					
					log(LOG_TIMEOUT, "Subscription " + subscription.getDeviceID() + " (" + provider.getContextType() + ") has timed out.");
					provider.removeSubscription(subscription.getDeviceID());
					
					if (provider.getNumSubscriptions() == 0)
					{
						provider.stop();
					}
				}
			}
			
			// Creates a String that Shows the Active Providers on this Device
			if (provider.getNumSubscriptions() > 0)
			{
				activeProviders += "[" + provider.getContextType() + " (" + provider.getNumSubscriptions() + ")] ";
			}
		}
		
		if (activeProviders.length() > 0)
		{
			log(LOG_SUBSCRIPTION, "Active Providers: " + activeProviders);
		}
		
		// TODO:  Replace with More Intelligent Determination
		return 1000 * 300; // 5 minutes
	}
	
	/**
	 * Delivers Context to the Application
	 * @return the amount of time (in ms) before the next piece of context needs to be delivered
	 */
	private long deliverContext()
	{
		long refreshRate = Long.MAX_VALUE;
		Date currentDate = new Date();
		
		for (ContextProvider provider : providers.values())
		{
			if (provider.isInUse())
			{
				long timeElapsed = currentDate.getTime() - provider.getLastTransmissionDate().getTime();
				
				if (timeElapsed > provider.getRefreshRate())
				{
					// Sends the Most Recent Reading
					provider.sendContext();	
					
					// Sets the Transmission Date
					provider.setLastTransmissionDate(currentDate);	
				}
				
				refreshRate = Math.min(refreshRate, provider.getRefreshRate());
			}
		}
		
		// Returns the Minimum Refresh Rate of all Context Providers
		return refreshRate;
	}

	/**
	 * Prints debug information. You should use this instead of System.out.println so that it is standardized for each platform 
	 * @param category
	 * @param message
	 */
	public abstract void log(String category, String message);
	
	// (ABSTRACT) SYSTEM EVENTS -----------------------------------------------------------------------------------------------------
	protected abstract void onContextDataReceived(ContextData data, ContextRequest request);
	
	protected abstract void onCapabilityReceived(ContextCapability capability);
	
	protected abstract void onCapabilitySubscribe(ContextCapability capability);
	
	protected abstract void onCapabilityUnsubscribe(ContextCapability capability);
	
	protected abstract void onRequestReceived(ContextRequest request);
	
	protected abstract void onRequestTimeout(ContextRequest request, ContextCapability capability);
	
	protected abstract void onSubscriptionTimeout(ContextSubscriptionInfo subscription);
	
	protected abstract void onProviderSubscribe(ContextProvider provider);
	
	protected abstract void onProviderUnsubscribe(ContextProvider provider);
	
	protected abstract void onSendingData(ContextData data);
}
