package com.adefreitas.gcf.desktop.toolkit;

import java.io.File;
import java.util.Scanner;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class JSONContextParser 
{	
	// The JSON Being Parsed
	private JsonObject jsonRoot;
		
	// An Enumerated Type for the Type of Thing being Input
	public static final int JSON_TEXT = 1;
	public static final int JSON_FILE = 2;
	
	/**
	 * Constructor
	 * @param json
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
	
	public void parseJSON(String json)
	{
		try
		{
			JsonParser parser = new JsonParser();
			jsonRoot = (JsonObject)parser.parse(json);
			//System.out.println("JSON Parsed Successfully");
		}
		catch (Exception ex)
		{
			System.out.println("Could not parse JSON.  Creating Blank");
			jsonRoot = new JsonObject();
		}
	}
	
	public void parseJSONFile(String filePath)
	{
		System.out.print("Parsing File: " + filePath + " . . . ");
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
				System.out.println("Problem occurred while loading personal context.");
				ex.printStackTrace();
			}
		}
		else
		{
			System.out.println("No File Specified");
		}
	}

	public JsonObject getJSONObject(String name)
	{
		try
		{
			JsonObject result = jsonRoot.getAsJsonObject(name);
			return result;
		}
		catch (Exception ex)
		{
			return null;
		}
	}
	
	public void setJSONObject(String name, JsonObject obj)
	{
		try
		{
			jsonRoot.add(name, obj);
		}
		catch (Exception ex)
		{
			System.out.println("Problem occurred while loading personal context: " + ex.getMessage());
		}
	}
	
	public String getString(String name, JsonObject obj)
	{
		if (obj != null)
		{
			JsonElement element = obj.get(name);
			
			if (element != null)
			{
				//System.out.println("ELEMENT: " + element.toString());
				return element.getAsString();
			}
			else
			{
				//System.out.println("ELEMENT: NULL");
			}
		}
		
		return null;
	}
	
	public String toString()
	{
		try
		{
			if (jsonRoot != null)
			{
				return jsonRoot.toString();
			}
			else
			{
				return null;
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return null;
		}
	}
}
