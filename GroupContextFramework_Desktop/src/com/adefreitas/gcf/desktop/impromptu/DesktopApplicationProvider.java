package com.adefreitas.gcf.desktop.impromptu;

import java.awt.Graphics2D;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.desktop.toolkit.GeoMathToolkit;
import com.adefreitas.gcf.desktop.toolkit.JSONContextParser;
import com.adefreitas.gcf.desktop.toolkit.SQLToolkit;
import com.adefreitas.gcf.impromptu.ApplicationProvider;
import com.adefreitas.gcf.messages.CommMessage;
import com.adefreitas.gcf.messages.ComputeInstruction;
import com.adefreitas.gcf.toolkit.SHA1;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public abstract class DesktopApplicationProvider extends ApplicationProvider
{
	// Java Robot
	private Robot robot;
	
	// Link to the Folder Where APP Data is Stored
	public static String APP_DATA_FOLDER = "appData/liveOS/";
	
	// DEBUG:  Used to Log Important Events
	private SQLToolkit sqlEventLogger;
	
	/**
	 * Constructor
	 * @param groupContextManager
	 */
	public DesktopApplicationProvider(GroupContextManager groupContextManager, 
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
		super(groupContextManager, contextType, name, description, category, contextsRequired, preferencesToRequest, logoPath, lifetime, commMode, ipAddress, port);
		
		try
		{
			robot = new Robot();
		}
		catch (Exception ex)
		{
			System.out.println("A problem occurred while creating the robot: " + ex.getMessage());
		}
	}
	
	public DesktopApplicationProvider(GroupContextManager groupContextManager, 
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
		super(groupContextManager, contextType, name, description, category, contextsRequired, preferencesToRequest, logoPath, lifetime, commMode, ipAddress, port, channel);
		
		try
		{
			System.out.print("Initializing Application " + name + " . . . ");
			robot = new Robot();
			System.out.println("DONE!");
		}
		catch (Exception ex)
		{
			System.out.println("A problem occurred while creating the robot: " + ex.getMessage());
		}
	}

	@Override
	public void onSubscription(ContextSubscriptionInfo newSubscription)
	{
		super.onSubscription(newSubscription);
		
		// Logs the Event
		log("APP_START", "Started by USER " + newSubscription.getDeviceID());
		
		// Sends the UI Immediately
		//sendContext();
		
		// Determines Credentials
		String username = CommMessage.getValue(newSubscription.getParameters(), "credentials");
		System.out.println("Subscription [" + this.getContextType() + "]: " + username);
	}
	
	@Override
	public void onSubscriptionCancelation(ContextSubscriptionInfo subscription)
	{
		// Determines Credentials
		String username = CommMessage.getValue(subscription.getParameters(), "credentials");
		System.out.println("Unsubscription [" + this.getContextType() + "]: " + username);
	}
	
	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		// Do Nothing By Default!
	}
		
	public void setSQLEventLogger(SQLToolkit toolkit)
	{
		this.sqlEventLogger = toolkit;
	}
	
	public void log(String tag, String message)
	{
		if (sqlEventLogger != null)
		{
			// Generates the Update Query
			String sql = String.format("INSERT INTO impromptu_eventLog (date, timestamp, app, tag, message) VALUES (now(), CURRENT_TIMESTAMP, '%s', '%s', '%s')", this.appID, tag, message);
			
			// Updates the Database
			sqlEventLogger.runUpdateQuery(sql);
		}
		else
		{
			System.out.println("Log Entry Ignored (SQL Event Logger is NULL");
		}
	}
	
	// HELPER METHODS ---------------------------------------------------------------------------------
	/**
	 * Retrieves the Java Robot, which can press keys/move the mosue
	 * @return
	 */
	public Robot getRobot()
	{
		return robot;
	}
	
	/**
	 * Runs a Command Line Instruction (i.e. "ls -a")
	 * @param command
	 */
	protected void executeRuntimeCommand(String command)
	{
		try
		{
			System.out.print("Trying to execute: " + command + " -- ");
			
			Process p = null;
			p = Runtime.getRuntime().exec(command);
			//p.waitFor();
			
			System.out.println("Done!");
        }
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	/**
	 * Retrieves the Application Specific Folder Where Data Can be Stored
	 * @return
	 */
	public String getLocalStorageFolder()
	{
		File folder = new File(APP_DATA_FOLDER + this.getContextType() + "/");
		
		// Creates Folders if they do not yet exist.
		if (!folder.exists())
		{
			folder.mkdirs();
		}
		
		return APP_DATA_FOLDER + this.getContextType() + "/"; 
	}
	
	/**
	 * Creates a Resized Image
	 * @param originalImage
	 * @param type
	 * @param Width
	 * @param Height
	 * @return
	 */
	public static File resizeImage(String picturePath, String outputName, int Width, int Height)
	{
		File outputFile = null;
		
		try
		{
			BufferedImage originalImage = ImageIO.read(new File(picturePath));
			int 		  type 		    = BufferedImage.TYPE_INT_RGB;
			
			BufferedImage resizedImage = new BufferedImage(Width, Height, type);
		    Graphics2D g = resizedImage.createGraphics();
		    g.drawImage(originalImage, 0, 0, Width, Height, null);
		    g.dispose();

		    outputName = (outputName.endsWith(".jpeg")) ? outputName : outputName + ".jpeg";
			outputFile = new File(outputName);
			ImageIO.write(resizedImage, "jpeg", outputFile);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	    
	    return outputFile;
	}
	
	// JSON CONTEXT HELPER METHODS --------------------------------------------------------------------
	/**
	 * Retrieves the List of Devices from the JSON Context File
	 * @param parser
	 * @return
	 */
	protected ArrayList<String> getDevices(JSONContextParser parser)
	{
		ArrayList<String> result = new ArrayList<String>();
		
		JsonArray devices = parser.getJSONObject("device").get("devices").getAsJsonArray();
		
		for (int i=0; i<devices.size(); i++)
		{
			result.add(devices.get(i).getAsString());
		}
		
		return result;
	}
	
	/**
	 * Retrieves a List of Context Providers from the JSON Context File
	 * @param parser
	 * @return
	 */
	protected ArrayList<String> getContextProviders(JSONContextParser parser)
	{
		ArrayList<String> result = new ArrayList<String>();
		
		JsonArray devices = parser.getJSONObject("device").get("contextproviders").getAsJsonArray();
		
		for (int i=0; i<devices.size(); i++)
		{
			result.add(devices.get(i).getAsString());
		}
		
		return result;
	}

	/**
	 * Retrieves the Device Name Listed on this Context File
	 * @param parser
	 * @return
	 */
	protected String getDeviceID(JSONContextParser parser)
	{
		return parser.getJSONObject("device").get("deviceID").getAsString();
	}

	/**
	 * Returns the Latitude Coordinate Specified in this Context
	 * @param parser
	 * @return
	 */
	protected double getLatitude(JSONContextParser parser)
	{
		try
		{
			return parser.getJSONObject("location").get("LATITUDE").getAsDouble();	
		}
		catch (Exception ex)
		{
			return 0.0;
		}
	}

	/**
	 * Returns the Longitude Coordinate Specified in this Context
	 * @param parser
	 * @return
	 */
	protected double getLongitude(JSONContextParser parser)
	{
		try
		{
			return parser.getJSONObject("location").get("LONGITUDE").getAsDouble();	
		}
		catch (Exception ex)
		{
			return 0.0;
		}
	}

	protected String getActivity(JSONContextParser parser)
	{
		String activity = "unspecified";
		
		try
		{
			activity = parser.getJSONObject("activity").get("type").getAsString();
		}
		catch (Exception ex)
		{
			// Do Nothing!
		}
		
		return activity;
	}
	
	protected int getActivityConfidence(JSONContextParser parser)
	{
		int confidence = 0;
		
		try
		{
			confidence = parser.getJSONObject("activity").get("confidence").getAsInt();
		}
		catch (Exception ex)
		{
			// Do Nothing!
		}
		
		return confidence;
	}
	
	protected boolean signedDisclaimer(JSONContextParser parser)
	{
		JsonObject preferences = parser.getJSONObject("preferences");
		
		return preferences != null && preferences.get("disclaimer") != null;
	}
	
	/**
	 * Calculates the Distance between the Device's Location and a Specified Point
	 * @param parser
	 * @return
	 */
	protected double getDistance(JSONContextParser parser, double startLatitude, double startLongitude)
	{
		JsonObject locationObject = parser.getJSONObject("location");
		
		if (locationObject != null && locationObject.has("LATITUDE") && locationObject.has("LONGITUDE"))
		{
			double latitude  = getLatitude(parser);
			double longitude = getLongitude(parser);
			
			return GeoMathToolkit.distance(startLatitude, startLongitude, latitude, longitude, 'K');
		}
		else
		{
			return Double.MAX_VALUE;
		}
	}
	
	/**
	 * Determines if a particular email address is inside of this context file
	 * @param parser
	 * @param emailAddress
	 * @return
	 */
	protected boolean hasEmailAddress(JSONContextParser parser, String emailAddress)
	{
		if (parser.getJSONObject("identity") != null && parser.getJSONObject("identity").has("email"))
		{
			String 	  hash 			 = SHA1.getHash(emailAddress);
			JsonArray emailAddresses = parser.getJSONObject("identity").get("email").getAsJsonArray();
			
			for (int i=0; i<emailAddresses.size(); i++)
			{
				if (emailAddresses.get(i).getAsString().equals(hash))
				{
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Determines if ONE of the following email addresses is inside of this context file
	 */
	protected boolean hasEmailAddress(JSONContextParser parser, String[] emailAddresses)
	{
		if (parser.getJSONObject("identity") != null && parser.getJSONObject("identity").has("email"))
		{
			for (String emailAddress : emailAddresses)
			{
				String 	  hash 			     = SHA1.getHash(emailAddress);
				JsonArray userEmailAddresses = parser.getJSONObject("identity").get("email").getAsJsonArray();
				
				for (int i=0; i<userEmailAddresses.size(); i++)
				{
					if (userEmailAddresses.get(i).getAsString().equals(hash))
					{
						return true;
					}
				}	
			}
		}
		
		return false;
	}
	
	/**
	 * Determines if a particular email domain is inside of this context
	 * @param parser
	 * @param emailDomain
	 * @return
	 */
	protected boolean hasEmailDomain(JSONContextParser parser, String[] domains)
	{
		JsonArray emailDomains = parser.getJSONObject("identity").get("emailDomains").getAsJsonArray();
		
		for (int i=0; i<emailDomains.size(); i++)
		{
			for (String domain : domains)
			{
				if (emailDomains.get(i).getAsString().equalsIgnoreCase(domain))
				{
					return true;
				}	
			}
		}
		
		return false;
	}

	/**
	 * Determines if a user is in a vehicle
	 * @param parser
	 * @return
	 */
	protected boolean inVehicle(JSONContextParser parser)
	{
		// Checks Activity to See if the User is in a Vehicle
		if (parser.getJSONObject("activity") != null && parser.getJSONObject("activity").get("type").getAsString().equals("in_vehicle"))
		{
			return true;
		}
		
		return false;
	}
}
