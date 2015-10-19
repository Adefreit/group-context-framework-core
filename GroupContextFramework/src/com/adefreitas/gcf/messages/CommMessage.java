package com.adefreitas.gcf.messages;

import java.util.ArrayList;

import com.google.gson.Gson;

public class CommMessage 
{
	public static final String MESSAGE_TYPE_DATA 	     = "D";
	public static final String MESSAGE_TYPE_REQUEST      = "R";
	public static final String MESSAGE_TYPE_CAPABILITY   = "C";
	public static final String MESSAGE_TYPE_SUBSCRIPTION = "S";
	public static final String MESSAGE_TYPE_INSTRUCTION  = "I";
	
	// Used to Differentiate Each Type of Message
	protected String messageType;
	   
	// Used by the System to Make Sure Communications Protocols are Up to Date
	protected final int version = 1;
	
	// The device that Generated this Message
	protected String deviceID;
	
	// Used to Allow the Device to Send a Message to Everyone
	protected String[] destination;
	
	// Attaches a Payload to Each Message
	protected ArrayList<String> payload;
	
	/**
	 * Constructor
	 * @param messageType 
	 */
	public CommMessage(String messageType)
	{
		this.messageType = messageType;
		this.destination = new String[0];
		this.payload     = null;
	}

	/**
	 * Converts JSON to Java Classes
	 * @param json
	 * @return 
	 */
	public static CommMessage jsonToMessage(String json)
	{      
		// Creates a GSON Object to Translate Json
		Gson gson = new Gson();

		// Converts the JSON into a Basic Message Class      
		try
		{
			CommMessage tmp    = gson.fromJson(json.trim(), CommMessage.class);
			CommMessage result = null;
			
			// Converts the Message to the Lowest Possible Class Using Message Types
			if (tmp.messageType.equalsIgnoreCase(MESSAGE_TYPE_DATA))
			{
				result = gson.fromJson(json.trim(), ContextData.class);
			}
			else if (tmp.messageType.equalsIgnoreCase(MESSAGE_TYPE_REQUEST))
			{
				result = gson.fromJson(json.trim(), ContextRequest.class);
			}
			else if (tmp.messageType.equalsIgnoreCase(MESSAGE_TYPE_CAPABILITY))
			{
				result = gson.fromJson(json.trim(), ContextCapability.class);
			}
			else if (tmp.messageType.equalsIgnoreCase(MESSAGE_TYPE_SUBSCRIPTION))
			{
				result = gson.fromJson(json.trim(), ContextSubscription.class);
			}
			else if (tmp.messageType.equalsIgnoreCase(MESSAGE_TYPE_INSTRUCTION))
			{
				result = gson.fromJson(json.trim(),  ComputeInstruction.class);
			}
			else
			{
				System.out.println("Invalid GCF Message:\n" + json);
			}

			return result;
		}
		catch (Exception e)
		{
			return null;
		}
	}

	/**
	 * DEPRECATED: Retrieves a Value from a String Array of the form "parameterName=value"
	 * @param values
	 * @param parameterName
	 * @return
	 */
	public static String getValue(String[] values, String parameterName)
	{
		if (values != null && parameterName != null)
		{
			for (String value : values)
			{
				//System.out.println(value);
				if (value.startsWith(parameterName))
				{
					String tmp = value.substring(parameterName.length());
					tmp = tmp.trim();
					
					if (tmp.startsWith("=") && tmp.length() > 1)
					{
						String v = tmp.substring(1).trim();
						//System.out.println("PARAMETER: " + parameterName + "; VALUE: " + v);
						return v;
					}
				}
			}	
		}
		
		return null;
	}
	
	/**
	 * DEPRECATED: Retrieves a Series of Values from a String Array of the form "parameterName=value1,value2,value3 . . ."
	 * @param values
	 * @param parameterName
	 * @return
	 */
	public static ArrayList<String> getValues(String[] values, String parameterName)
	{
		String value  = getValue(values, parameterName);
		
		if (value != null)
		{
			String[] o = value.split(",");
			
			ArrayList<String> result = new ArrayList<String>();
			
			for (String s : o)
			{
				result.add(s.trim());
			}
			
			//System.out.println("Found " + result.size() + " values for parameter " + parameterName);
			return result; 
		}
		
		return null;
	}
	
	/**
	 * DEPRECATED: Retrieves a Comma Delineated String
	 * @param array
	 * @return
	 */
	public static String toCommaString(String[] array)
	{
		String result = "";
		
		for (String element : array)
		{
			result += element + ",";
		}
		
		if (result.length() > 0)
		{
			return result.substring(0, result.length()-1);
		}
		else
		{
			return result;
		}
	}
	
	/**
	 * Retrieves the Message Type (i.e. "CDAT", etc)
	 * @return 
	 */
	public String getMessageType()
	{
		return messageType;
	}

	/**
	 * Returns the framework version of this message
	 */
	public int getMessageVersion()
	{
		return version;
	}

	/**
	 * Returns the Device ID of the Device that Generated this Message
	 * @return
	 */
	public String getDeviceID()
	{
		return deviceID;
	}
	
	/**
	 * Sets the Destination for this Message
	 * @param destination
	 */
	public void setDestination(String[] newDestination)
	{
		this.destination = newDestination;
	}
	
	/**
	 * Returns the Device IDs that this message is intended for!
	 * @return
	 */
	public String[] getDestination()
	{
		if (destination != null)
		{
			return destination;	
		}
				
		return new String[0];
	}
	
	/**
	 * Determines if the Specified Device ID is a Destination
	 * @param deviceID
	 * @return
	 */
	public boolean isDestination(String deviceID)
	{	
		if (destination == null || destination.length == 0)
		{
			// If no destination is specified, assume that it is for everyone
			return true;
		}
		else
		{
			// Otherwise, look for the specific destination
			for (String destinationID : destination)
			{
				//System.out.println("Comparing " + destinationID + " to " + deviceID);
				if (destinationID != null && destinationID.equals(deviceID))
				{
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Inserts a Value to the Payload WITH a Reference Key
	 * @param key
	 * @param value
	 */
	public void putPayload(String key, String value)
	{
		if (payload == null)
		{
			payload = new ArrayList<String>();
		}
		
		payload.add(key + "=" + value);
	}
	
	/**
	 * Inserts a Value to the Payload WITHOUT a Reference Key
	 * @param value
	 */
	public void putPayload(String value)
	{
		if (payload == null)
		{
			payload = new ArrayList<String>();
		}
		
		payload.add(value);
	}
	
	/**
	 * Inserts an Array of Values to the Payload (Does Not Check for Keys)
	 * @param values
	 */
	public void putPayload(String[] values)
	{
		if (payload == null)
		{
			payload = new ArrayList<String>();
		}
		
		for (String value : values)
		{
			payload.add(value);
		}
	}
	
	/**
	 * Retrieves a Value from the Payload (if it exists).  Returns null otherwise.
	 * @param key
	 * @return
	 */
	public String getPayload(String key)
	{
		for (String s : payload)
		{
			String[] elements = s.split("=");
			
			if (elements.length >= 2)
			{
				if (elements[0].trim().equals(key.trim()))
				{
					return s.substring(s.indexOf("=") + 1);
				}	
			}
		}
		
		// Returns NULL if Nothing Found
		return null;
	}
	
	/**
	 * Retrieves a Specific Value from the Payload
	 * @param index
	 * @return
	 */
	public String getPayload(int index)
	{
		if (index >= 0 && index <= payload.size()-1)
		{
			return payload.get(index);
		}
		
		return null;
	}
	
	/**
	 * Retrieves all Values from the Payload
	 * @return
	 */
	public String[] getPayload()
	{
		if (payload == null)
		{
			return new String[0];
		}
		else
		{
			return payload.toArray(new String[0]);
		}
	}
	
	/**
	 * Determines if a Value Exists in the Payload with the Specified Key
	 * @param key
	 * @return
	 */
	public boolean hasPayload(String key)
	{
		for (String s : payload)
		{
			String[] elements = s.split("=");
			
			if (elements.length >= 2)
			{
				if (elements[0].equals(key))
				{
					return true; 
				}	
			}
		}
		
		// Returns NULL if Nothing Found
		return false;
	}

	/**
	 * Removes All Values from the Payload
	 */
	public void clearPayload()
	{
		this.payload.clear();
	}
	
	/**
	 * Converts this Message into a String (primarily for debug purposes)
	 */
	public String toString()
	{
		return "COMM MESSAGE:  " + messageType + " (Version " + version + ")";
	}
}
