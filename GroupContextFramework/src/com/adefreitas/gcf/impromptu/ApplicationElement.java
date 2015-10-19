package com.adefreitas.gcf.impromptu;

import com.google.gson.Gson;

public abstract class ApplicationElement 
{
	private String parentAppID;
	private String name;

	public ApplicationElement(String parentAppID, String name)
	{
		this.parentAppID = parentAppID;
		this.name  = name;
	}
	
	public String getParentAppID()
	{
		return parentAppID;
	}
	
	public String getName()
	{
		return name;
	}
	
	public String toJSON()
	{
		try
		{
			Gson gson = new Gson();
			return gson.toJson(this);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return null;
		}
	}
	
	public static String toJSONArray(ApplicationElement[] elements)
	{
		String result = "[";
		
		for (int i=0; i<elements.length; i++)
		{
			// Adds the JSON String
			result += elements[i].toJSON();
			
			// Adds a Comma to Each Element
			if (i < elements.length - 1)
			{
				result += ",";
			}
		}
		
		// Adds the Final Bracket
		result += "]";
		
		return result;
	}
}
