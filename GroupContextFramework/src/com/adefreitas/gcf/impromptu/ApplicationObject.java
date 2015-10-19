package com.adefreitas.gcf.impromptu;

import java.util.ArrayList;

public class ApplicationObject extends ApplicationElement
{
	private String   		  objectType;
	private ArrayList<String> parameters;
	
	/**
	 * Constructor
	 * @param objectType
	 * @param objectName
	 * @param parameters
	 */
	public ApplicationObject(String appID, String objectType, String objectName)
	{
		super(appID, objectName);
		this.objectType = objectType;
		this.parameters = new ArrayList<String>();
	}
	
	public void addParameter(String name, String value)
	{
		parameters.add(name + "=" + value);
	}
	
	public String getType()
	{
		return objectType;
	}
	
	public String[] getParameters()
	{
		return parameters.toArray(new String[0]);
	}
}
