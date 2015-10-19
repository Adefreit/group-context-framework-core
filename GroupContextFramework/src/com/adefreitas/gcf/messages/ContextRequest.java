package com.adefreitas.gcf.messages;

public class ContextRequest extends CommMessage
{	
	// Used to Denote the Type of Request
	public static final int LOCAL_ONLY      = 1;
	public static final int SINGLE_SOURCE   = 2;
	public static final int MULTIPLE_SOURCE = 3;
	
	// The Unique String Representing the Type of Context Being Requested ("LOC", "ACC")
	private String contextType;
	
	// The Integer Code Representing the Type of Request
	private int	requestType;
	
	// The Rate (in Milliseconds) at which Context Should be Requested
	private int refreshRate;
	
	// TRANSIENT PROPERTIES (NOT SERIALIZED OR COMMUNICATED)
	// Weights (Used by Single Source Arbiter)
	private transient double w_battery;
	private transient double w_sensorFitness;
	private transient double w_foreign;
	private transient double w_providing;
	private transient double w_reliability;
	
	/**
	 * Constructor
	 * @param deviceID
	 * @param contextType
	 * @param requestType
	 * @param refreshRate
	 * @param w_battery
	 * @param w_sensorFitness
	 * @param w_foreign
	 * @param w_providing
	 * @param w_reliability
	 * @param parameters
	 */
	public ContextRequest(String deviceID, String contextType, int requestType, String[] destination, 
						  int refreshRate, double w_battery, double w_sensorFitness, double w_foreign, 
						  double w_providing, double w_reliability, String[] payload)
	{
		super(CommMessage.MESSAGE_TYPE_REQUEST);
		this.deviceID        = deviceID;
		this.contextType     = contextType;
		this.requestType     = requestType;
		this.refreshRate     = refreshRate;
		this.w_battery       = w_battery;
		this.w_sensorFitness = w_sensorFitness;
		this.w_foreign 	  	 = w_foreign;
		this.w_providing 	 = w_providing;
		this.w_reliability 	 = w_reliability;
				
		// Sets Destination
		this.destination = destination;
		this.putPayload(payload);
	}
	
	/**
	 * Compares Two Requests to See if they are the Same
	 * @param otherRequest
	 * @return
	 */
	public boolean equals(ContextRequest otherRequest)
	{
		return contextType.equals(otherRequest.getContextType()) && 
			   (requestType == otherRequest.getRequestType());
	}
	
	/**
	 * Converts this Message into a String
	 */
	public String toString()
	{
		String payloadContents = "{";
		
		for (String parameter : getPayload())
		{
			payloadContents += parameter + " ";
		}
		
		payloadContents += "}";
		
		return String.format("REQUEST [%s] (Device=%s; RequestType=%s; Refresh=%d):  %d byte payload", contextType, deviceID, requestType, refreshRate, payloadContents.length()-2);
	}

	// -----------------------------------------------------------------------
	// SETTERS
	// -----------------------------------------------------------------------
	
	
	// -----------------------------------------------------------------------
	// GETTERS
	// -----------------------------------------------------------------------
	public String getContextType()
	{
		return contextType;
	}
	
	public int getRequestType()
	{
		return requestType;
	}

	public int getRefreshRate()
	{
		return refreshRate;
	}	
	
	public int getTimeoutDuration()
	{
		return Math.max(10000, refreshRate * 3);
	}

	public double getBatteryWeight()
	{
		return w_battery;
	}
	
	public double getSensorFitnessWeight()
	{
		return w_sensorFitness;
	}
	
	public double getForeignWeight()
	{
		return w_foreign;
	}
	
	public double getProvidingWeight()
	{
		return w_providing;
	}
	
	public double getReliabilityWeight()
	{
		return w_reliability;
	}
		
}