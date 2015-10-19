package template;

import com.adefreitas.gcf.CommManager;
import com.adefreitas.gcf.desktop.DesktopGroupContextManager;
import com.adefreitas.gcf.desktop.EventReceiver;
import com.adefreitas.gcf.messages.ContextCapability;
import com.adefreitas.gcf.messages.ContextData;
import com.google.gson.Gson;

/**
 * This is a simple example of how to use GCF in a desktop application
 * @author adefreit
 *
 */
public class GCFDesktopApplication implements EventReceiver
{
	// Creates a Unique Device ID (Needed for GCM to Operate, But You Can Change it to Anything Unique)
	public String deviceID;
	
	// GCF Communication Settings
	public static final CommManager.CommMode COMM_MODE  = CommManager.CommMode.MQTT;
	public static final String 				 IP_ADDRESS = "epiwork.hcii.cs.cmu.edu";
	public static final int    				 PORT 	    = 1883;
	
	// GCF Variables
	public DesktopGroupContextManager groupContextManager;
	
	// Gson
	public Gson gson = new Gson();
	
	/**
	 * Constructor:  Initializes the GCM
	 */
	public GCFDesktopApplication(String[] args)
	{
		// Assigns the Desktop Application's Name
		deviceID  = (args.length >= 1) ? args[0] : "DESKTOP_APP_" + (System.currentTimeMillis() % 1000);
		
		// Creates the Group Context Manager
		groupContextManager  = new DesktopGroupContextManager(deviceID, false);
		String connectionKey = groupContextManager.connect(COMM_MODE, IP_ADDRESS, PORT);

		// GCM Settings
		groupContextManager.registerEventReceiver(this);
			
		// TODO:  Create/Register Context Providers
		//groupContextManager.registerContextProvider(new ContextProvider());
	}
	
	/**
	 * This Method is Called Whenever the GCM Receives Data
	 */
	@Override
	public void onContextData(ContextData data) 
	{
		System.out.println(data);
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
	 * This is the Main Application
	 * @param args
	 */
	public static void main(String[] args) 
	{			
		new GCFDesktopApplication(args);
	}


}
