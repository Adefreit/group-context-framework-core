package impromptu_apps;

import java.util.ArrayList;

import com.adefreitas.gcf.CommManager;
import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.desktop.DesktopBatteryMonitor;
import com.adefreitas.gcf.desktop.DesktopGroupContextManager;
import com.adefreitas.gcf.desktop.EventReceiver;
import com.adefreitas.gcf.desktop.impromptu.DesktopApplicationProvider;
import com.adefreitas.gcf.desktop.impromptu.QueryApplicationProvider;
import com.adefreitas.gcf.desktop.toolkit.JSONContextParser;
import com.adefreitas.gcf.desktop.toolkit.SQLToolkit;
import com.adefreitas.gcf.impromptu.ApplicationSettings;
import com.adefreitas.gcf.messages.ContextCapability;
import com.adefreitas.gcf.messages.ContextData;
import com.google.gson.JsonObject;

public class Application implements EventReceiver
{
	// Creates a Unique Computer Name (Needed for GCM to Operate, But You Can Change it to Anything Unique)
	public String COMPUTER_NAME 		  = "";
	public int    UPDATE_THREAD_WAIT_TIME = 30000;
	
	// GCF Communication Settings (BROADCAST_MODE Assumes a Functional TCP Relay Running)
	public static final CommManager.CommMode COMM_MODE       = CommMode.MQTT;
	public static final String 				 IP_ADDRESS      = "IP_ADDRESS_GOES_HERE";
	public static final int    				 PORT 	         = 1883;
	
	// GCF Variables
	public DesktopBatteryMonitor      batteryMonitor;
	public DesktopGroupContextManager gcm;

	// List of All Application Providers Currently Loaded
	public ArrayList<DesktopApplicationProvider> appProviders = new ArrayList<DesktopApplicationProvider>();

	// Context Provider Representing the Application
	public QueryApplicationProvider queryProvider;
	
	// SQL Toolkit
	public static final String SQL_SERVER   = "epiwork.hcii.cs.cmu.edu";
	public static final String SQL_USERNAME = "adrian";
	public static final String SQL_PASSWORD = "@dr1@n1234";
	public static final String SQL_DB		= "gcf_impromptu";
	public SQLToolkit sqlToolkit;
	
	/**
	 * Constructor
	 * @param appName
	 * @param useBluetooth
	 */
	public Application(String appName, boolean useBluetooth)
	{
		COMPUTER_NAME  = (appName.length() > 0) ? appName : "APP_" + (System.currentTimeMillis() % 1000);
		gcm 		   = new DesktopGroupContextManager(COMPUTER_NAME, false);
		
		// Opens the Connection
		String connectionKey = gcm.connect(COMM_MODE, IP_ADDRESS, PORT);
		gcm.subscribe(connectionKey, ApplicationSettings.DNS_APP_CHANNEL);
		
		// Manages Debugging
		gcm.setDebugMode(false);
		
		// Registers Objects to Handle Requests and Messages
		gcm.registerEventReceiver(this);
		
		// Creates the Object that is Responsible for 
		queryProvider = new QueryApplicationProvider(gcm, connectionKey);
		gcm.registerContextProvider(queryProvider);

		// Creates the Individual Apps that this Application will Host
		initializeApps();
		
		// Initializes Communications Channel
		for (DesktopApplicationProvider app : appProviders)
		{
			System.out.print("Loading " + app.getAppID() + " [" + app.getContextType() + "] . . . ");
			app.setSQLEventLogger(sqlToolkit);
			gcm.registerContextProvider(app);
			gcm.subscribe(connectionKey, app.getContextType());
			System.out.println("Done!");
		}
				
		// And We're Done!
		System.out.println(appProviders.size() + " App(s) Initialized!\n");
	}

	/**
	 * This Creates all of the Apps that Impromptu Users will See
	 */
	private void initializeApps()
	{
		//appProviders.add(new App_Survey("AUGUST_24_FEEDBACK", gcm, COMM_MODE, IP_ADDRESS, PORT, sqlToolkit));
	}
		
	/**
	 * This Method gets Called when the Device Receives Context Data
	 */
	@Override
	public void onContextData(ContextData data) 
	{
		if (data.getContextType().equals("BLUEWAVE"))
		{
			String context = data.getPayload("CONTEXT");
			
			if (context != null)
			{
				JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_TEXT, data.getPayload("CONTEXT"));
				JsonObject locationObject = parser.getJSONObject("location");
				
				if (locationObject != null)
				{
					parser.getJSONObject("location").addProperty("SENSOR", data.getDeviceID());
				}
				
				this.queryProvider.processContext(parser.getJSONObject("device").get("deviceID").getAsString(), parser.toString(), "BLUEWAVE [" + data.getDeviceID() + "]");
			}
		}	
	}
	
	/**
	 * This method is Called Whenever the GCM Subscribes to a Context Provider
	 */
	@Override
	public void onCapabilitySubscribe(ContextCapability capability) {
		// TODO Auto-generated method stub
	}

	/**
	 * This method is Called Whenever the GCM Unsubscribes from a Context Provider
	 */
	@Override
	public void onCapabilityUnsubscribe(ContextCapability capability) {
		// TODO Auto-generated method stub
	}
	
	/**
	 * This Method Starts the Whole Application
	 * @param args
	 */
	public static void main(String[] args) 
	{
		if (args.length > 0)
		{
			new Application(args[0], false);
		}
		else
		{
			new Application("", false);
		}
	}
	
}
