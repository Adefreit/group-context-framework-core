package com.adefreitas.gcf.android.bluewave;

import java.io.File;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;

import android.util.Log;

import com.adefreitas.gcf.toolkit.SHA1;

public class JSONContextParser 
{	
	// Log Name
	private static final String LOG_NAME = "JSONContextParser";
	
	// The JSON Being Parsed
	private JSONObject jsonRoot;
		
	// An Enumerated Type for the Type of Thing being Input
	public static final int JSON_TEXT = 1;
	public static final int JSON_FILE = 2;
	
	/**
	 * Constructor
	 * @param data The JSON string representing the context
	 */
	public JSONContextParser(String data)
	{
		parseJSON(data);
	}
	
	/**
	 * Constructor
	 * @param mode Specifies if the data field contains a JSON string or file path 
	 * @param data The JSON string, or file path
	 */
	public JSONContextParser(int mode, String data)
	{
		if (mode == JSON_TEXT)
		{
			parseJSON(data);
		}
		else if (mode == JSON_FILE)
		{
			parseJSONFile(data);
		}
	}
	
	private void parseJSON(String json)
	{
		try
		{
			jsonRoot = new JSONObject(json);
			//Log.d(LOG_NAME, "JSON Parsed Successfully");
		}
		catch (Exception ex)
		{
			Log.e(LOG_NAME, "Could not parse " + json + " (from string).  Creating Blank");
			jsonRoot = new JSONObject();
		}
	}
	
	private void parseJSONFile(String filePath)
	{
		//Log.d(LOG_NAME, "Parsing File: " + filePath);
		File downloadedFile = new File(filePath);
		
		if (downloadedFile.exists())
		{
			try
			{
				// Extract the Contents of the File
				Scanner scanner  = new Scanner(downloadedFile);
				String  json     = "";
				
				while (scanner.hasNextLine())
				{
					json += scanner.nextLine();
				}
				
				scanner.close();
				
				parseJSON(json);
			}
			catch (Exception ex)
			{
				Log.e(LOG_NAME, "Could not parse " + filePath + " (from file).  Creating Blank");
				ex.printStackTrace();
			}
		}
		else
		{
			Log.e(LOG_NAME, "No File Specified");
		}
	}

	public JSONObject getJSONRoot()
	{
		return jsonRoot;
	}

	public JSONObject getJSONObject(String name)
	{
		try
		{
			JSONObject result = jsonRoot.getJSONObject(name);
			return result;
		}
		catch (Exception ex)
		{
			return null;
		}
	}
	
	public JSONArray getJSONArray(String name)
	{
		try
		{
			JSONArray result = jsonRoot.getJSONArray(name);
			return result;
		}
		catch (Exception ex)
		{
			return null;
		}
	}
	
	public void setJSONObject(String name, JSONObject obj)
	{
		try
		{
			jsonRoot.put(name, obj);
		}
		catch (Exception ex)
		{
			Log.e(LOG_NAME, "Problem occurred while setting JSON Object: " + ex.getMessage());
		}
	}
	
	public void setJSONArray(String name, JSONArray array)
	{
		try
		{
			jsonRoot.put(name, array);
		}
		catch (Exception ex)
		{
			Log.e(LOG_NAME, "Problem occurred while setting JSON Array: " + ex.getMessage());
		}
	}
	
	public void removeJSONObject(String name)
	{
		if (jsonRoot != null && jsonRoot.has(name))
		{
			jsonRoot.remove(name);	
		}
	}
	
	/**
	 * Returns the Device ID for this Device
	 * @return
	 */
	public String getDeviceID()
	{
		try
		{
			JSONObject deviceContext = jsonRoot.getJSONObject(BluewaveManager.DEVICE_TAG);
			return (deviceContext.has(BluewaveManager.DEVICE_ID)) ? deviceContext.getString(BluewaveManager.DEVICE_ID) : null; 
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return null;
		}
	}

	/**
	 * Determines if ONE of the following email addresses is inside of this context file
	 */
	public boolean hasEmailAddress(String[] emailAddresses)
	{
		try
		{
			if (this.getJSONObject("identity") != null && this.getJSONObject("identity").has("email"))
			{
				for (String emailAddress : emailAddresses)
				{
					String 	  hash 			     = SHA1.getHash(emailAddress);
					JSONArray userEmailAddresses = this.getJSONObject("identity").getJSONArray("email");
					
					for (int i=0; i<userEmailAddresses.length(); i++)
					{
						if (userEmailAddresses.getString(i).equals(hash))
						{
							return true;
						}
					}	
				}
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		return false;
	}
	
	/**
	 * Converts this Parser Back to JSON
	 */
	public String toString()
	{
		try
		{
			return jsonRoot.toString(2);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return null;
		}
	}
}
