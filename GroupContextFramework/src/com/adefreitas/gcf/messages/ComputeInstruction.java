package com.adefreitas.gcf.messages;

import java.util.Arrays;

public class ComputeInstruction extends CommMessage
{	
	// Serialization Labels (Needed for Android Intents)
	public static final String COMPUTE_CONTEXT_TYPE = "CONTEXT_TYPE";
	public static final String COMPUTE_SENDER 	    = "SENDER";
	public static final String COMPUTE_DESTINATION  = "DESTINATION";
	public static final String COMPUTE_COMMAND 	    = "COMMAND";
	public static final String COMPUTE_PARAMETERS   = "PARAMETERS";
	
	private String contextType;
	private String command;
	
	public ComputeInstruction(String contextType, String sourceDeviceID, String[] destination, String command, String[] payload) 
	{
		super(CommMessage.MESSAGE_TYPE_INSTRUCTION);
		this.contextType = contextType;
		this.deviceID    = sourceDeviceID;
		this.destination = destination;
		this.command 	 = command;
		this.putPayload(payload);
	}
	
	public String getContextType()
	{
		return contextType;
	}
			
	public String getCommand()
	{
		return command;
	}
	
	public String toString()
	{
		return String.format("DEVICE: %s; CONTEXT: %s; COMMAND: %s; PARAMETERS: %s", deviceID, contextType, command, Arrays.toString(this.getPayload()));
	}
}
