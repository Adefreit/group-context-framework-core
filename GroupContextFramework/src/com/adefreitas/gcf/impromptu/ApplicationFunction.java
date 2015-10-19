package com.adefreitas.gcf.impromptu;

import java.util.ArrayList;

public class ApplicationFunction extends ApplicationElement
{	 
	private String 						 functionDescription;
	private ArrayList<ApplicationObject> objectsRequired;
	private String						 callbackCommand;
	
	/**
	 * Constructor
	 * @param functionName
	 * @param functionDescription
	 */
	public ApplicationFunction(String appID, String name, String functionDescription, String callbackCommand)
	{
		super(appID, name);
		this.functionDescription = functionDescription;
		this.objectsRequired     = new ArrayList<ApplicationObject>();
		this.callbackCommand     = callbackCommand;
	}
	
	public String getDescription()
	{
		return functionDescription;
	}
		
	public ApplicationObject[] getObjectsRequired()
	{
		return objectsRequired.toArray(new ApplicationObject[0]);
	}

	public String getCallbackCommand()
	{
		return callbackCommand;
	}
	
	public ArrayList<ApplicationObject> getRequiredObjects()
	{
		return objectsRequired;
	}
	
	public void addRequiredObject(ApplicationObject obj)
	{
		if (!objectsRequired.contains(obj))
		{
			objectsRequired.add(obj);
		}
	}
	
	public boolean isCompatible(ApplicationObject[] objects)
	{
		// Creates a Copy
		ArrayList<ApplicationObject> tmp = new ArrayList<ApplicationObject>(objectsRequired);
		
		if (this.objectsRequired.size() >= objects.length)
		{
			for (ApplicationObject o : objects)
			{
				for (ApplicationObject requiredObject : tmp)
				{
					if (o.getType().equals(requiredObject.getType()))
					{
						tmp.remove(requiredObject);
						break;
					}
				}
			}
		}
		
		// Only Return True if ALL Required Objects are Satisfied
		return tmp.size() == 0;
	}
}
