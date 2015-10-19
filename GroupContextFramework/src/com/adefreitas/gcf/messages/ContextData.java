package com.adefreitas.gcf.messages;

import java.util.Arrays;

public class ContextData extends CommMessage
{
	// Serialization Labels (Needed for Android Intents)
	public static final String CONTEXT_TYPE = "CONTEXT_TYPE";
	public static final String DEVICE_ID    = "DEVICE_ID";
	public static final String PAYLOAD      = "PAYLOAD";
	
	// Attributes
	private String contextType;
	
	/**
	 * Constructor
	 * @param contextType
	 * @param deviceID
	 * @param payload
	 */
	public ContextData(String contextType, String deviceID, String[] payload)
	{
		super(CommMessage.MESSAGE_TYPE_DATA);
		this.contextType = contextType;
		this.deviceID    = deviceID;
		this.destination = new String[0];
		this.putPayload(payload);
	}
	
	public ContextData(String contextType, String deviceID, String[] destinations, String[] payload)
	{
		this(contextType, deviceID, payload);
		this.destination = destinations;
	}
	
	public String getContextType()
	{
		return contextType;
	}
	
	public String toString()
	{
		String result = String.format("DEVICE: %s; CONTEXT: %s; DEST: %s; VALUES: %s", deviceID, contextType, Arrays.toString(destination), Arrays.toString(this.getPayload()));
		
		return result;
	}
}
