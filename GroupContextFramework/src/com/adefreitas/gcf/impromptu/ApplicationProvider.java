package com.adefreitas.gcf.impromptu;

import java.util.ArrayList;

import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.ContextProvider;
import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.messages.ComputeInstruction;

public abstract class ApplicationProvider extends ContextProvider
{	
	// Context Configuration
	protected String APP_CHANNEL  = "cmu/gcf_application";
	protected String CONTEXT_TYPE = "UNKNOWN_APP";
	protected String LOG_NAME 	  = "";
	
	// App Configuration and Settings
	protected String   appID;
	protected String   name;
	protected String   description;
	protected String   category;
	protected String[] contextsRequired;
	protected String[] preferencesToRequest;
	protected String   logoPath;
	protected int	   lifetime;
	protected CommMode commMode;
	protected String   ipAddress;
	protected int      port;
	protected String   channel;
	protected String   proxyDeviceID;
	
	/**
	 * Constructor
	 * @param groupContextManager
	 */
	public ApplicationProvider(GroupContextManager groupContextManager, 
			String contextType, 
			String name, 
			String description, 
			String category,
			String[] contextsRequired, 
			String[] preferencesToRequest, 
			String logoPath, 
			int lifetime,
			CommMode commMode,
			String ipAddress, 
			int port) 
	{
		super(contextType, groupContextManager);
		
		// Allows Devices to Send Compute Instructions Without Having to be Connected
		// Needed by LOS_DNS
		this.setSubscriptionRequiredForCompute(false);
		
		// Saves Values
		CONTEXT_TYPE = contextType;
		LOG_NAME 	 = "GCF-MagicApp [" + CONTEXT_TYPE    + "]";
		
		// Creates a Unique ID
		this.appID = "APP_" + contextType;
		
		// Saves App Settings
		this.name 				  = name;
		this.description 		  = description;
		this.category 			  = category;
		this.contextsRequired 	  = contextsRequired;
		this.preferencesToRequest = preferencesToRequest;
		this.logoPath 			  = logoPath;
		this.lifetime			  = lifetime;
		this.commMode			  = commMode;
		this.ipAddress 			  = ipAddress;
		this.port 				  = port;
		this.channel			  = contextType;
	}
	
	/**
	 * Constructor (Allows you to Specify a Custom Communications Channel)
	 * @param groupContextManager
	 * @param contextType
	 * @param name
	 * @param description
	 * @param category
	 * @param contextsRequired
	 * @param preferencesToRequest
	 * @param logoPath
	 * @param lifetime
	 * @param commMode
	 * @param ipAddress
	 * @param port
	 * @param channel
	 */
	public ApplicationProvider(GroupContextManager groupContextManager, 
			String contextType, 
			String name, 
			String description, 
			String category,
			String[] contextsRequired, 
			String[] preferencesToRequest, 
			String logoPath, 
			int lifetime,
			CommMode commMode,
			String ipAddress, 
			int port,
			String channel) 
	{	
		this(groupContextManager, contextType, name, description, category, contextsRequired, preferencesToRequest, logoPath, lifetime, commMode, ipAddress, port);
		
		this.channel = channel;
	}

	@Override
	public void start() 
	{
		this.getGroupContextManager().log(LOG_NAME, this.getContextType() + " Provider Started");
	}

	@Override
	public void stop() 
	{
		this.getGroupContextManager().log(LOG_NAME, this.getContextType() + " Provider Stopped");
	}
	
	@Override
	public void onSubscription(ContextSubscriptionInfo newSubscription)
	{
		super.onSubscription(newSubscription);
				
		// Sends the UI Immediately
		//sendContext();
		
		// Stores Context
		//String context = CommMessage.getValue(newSubscription.getParameters(), "context");
		
		// Determines Credentials
		//String username = CommMessage.getValue(newSubscription.getParameters(), "credentials");
		//System.out.println("Subscription: " + username);
	}
	
	@Override
	public void onSubscriptionCancelation(ContextSubscriptionInfo subscription)
	{
//		// Determines Credentials
//		System.out.print("Unsubscription: ");
//		String username = CommMessage.getValue(subscription.getParameters(), "credentials");
//		System.out.println(username + "\n");
	}
	
	public void sendContext()
	{
		String connectionKey = this.getGroupContextManager().getConnectionKey(this.commMode, this.ipAddress, this.port);
		
		for (ContextSubscriptionInfo info : this.getSubscriptions())
		{
			//System.out.println("Sending Interface to: " + info.getDeviceID());
			//this.sendContext(new String[] { info.getDeviceID() }, getInterface(info));
			this.getGroupContextManager().sendContext(connectionKey, this.channel, this.getContextType(), new String[] { info.getDeviceID() }, getInterface(info));
		}
	}
	
	@Override
	public double getFitness(String[] parameters)
	{
		return 1.0;
	}
	
	/**
	 * Returns a String Array Containing Information Needed by the Mobile App
	 * @return
	 */
	public ArrayList<String> getInformation(String userContextJSON)
	{		
		ArrayList<String> result = new ArrayList<String>();
		
		result.add("APP_ID=" + appID);
		result.add("APP_CONTEXT_TYPE=" + CONTEXT_TYPE);
		result.add("DEVICE_ID=" + ((this.proxyDeviceID == null) ? this.getGroupContextManager().getDeviceID() : this.proxyDeviceID));
		result.add("NAME=" + getName(userContextJSON));
		result.add("DESCRIPTION=" + getDescription(userContextJSON));
		result.add("CATEGORY=" + getCategory(userContextJSON));
		result.add("CONTEXTS=" + arrayToString(contextsRequired));
		result.add("PREFERENCES=" + arrayToString(preferencesToRequest));
		result.add("LOGO=" + getLogoPath(userContextJSON));
		result.add("LIFETIME=" + getLifetime(userContextJSON));
		result.add("FUNCTIONS="	+ getFunctions());
		result.add("COMM_MODE="	+ commMode.toString());
		result.add("APP_ADDRESS=" + ipAddress);
		result.add("APP_PORT=" + Integer.toString(port));
		result.add("APP_CHANNEL=" + channel);
		
		return result;
	}
	
	// GETTERS/SETTERS --------------------------------------------------------------------------------
	public String getAppID()
	{
		return appID;
	}
	
	public String getName(String userContextJSON)
	{
		return name;
	}
	
	public void setName(String newName)
	{
		this.name = newName;
	}
	
	public String getCategory(String userContextJSON)
	{
		return category;
	}
	
	public String getDescription()
	{
		return description;
	}
	
	public String getDescription(String userContextJSON)
	{
		return description;
	}
	
	public void setDescription(String newDescription)
	{
		this.description = newDescription;
	}
	
	public int getLifetime(String userContextJSON)
	{
		return this.lifetime;
	}
	
	public void setLifetime(int newLifetime)
	{
		this.lifetime = newLifetime;
	}
	
	public String getLogoPath(String userContextJSON)
	{
		return logoPath;
	}
	
	public String getChannel()
	{
		return channel;
	}
	
	public void setProxyDeviceID(String deviceID)
	{
		this.proxyDeviceID = deviceID;
	}
	
	// METHODS TO IMPLEMENT/OVERRIDE ------------------------------------------------------------------
	public abstract String[] getInterface(ContextSubscriptionInfo subscription);
	
	public String getFunctions()
	{
		return null;
	}
	
	public abstract boolean sendAppData(String bluewaveContextJSON);
	
	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		System.out.println("Received Instruction: " + instruction.toString());
	}
	
	// HELPER METHODS ---------------------------------------------------------------------------------	
//	public void setUserPreference(String deviceID, String key, String value)
//	{
//		this.getGroupContextManager().sendComputeInstruction("PREF", new String[] { deviceID }, "SET_PREFERENCE", new String[] { key + "=" + value });
//	}
	
	private String arrayToString(String[] array)
	{
		String result = "";
		
		if (array != null)
		{
			for (String s : array)
			{
				result += s + ",";
			}
			
			if (result.length() > 0)
			{
				result = result.substring(0, result.length()-1);
			}	
		}
		
		return result;
	}
}
