package com.adefreitas.gcf.android.bluewave;

import java.util.Date;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.android.toolkit.HttpToolkit;
import com.adefreitas.gcf.toolkit.SHA1;

public class BluewaveManager
{
	// Log Constant
	private static final String LOG_NAME = "Bluewave";
	
	// Protocol Constants
	public static final String ID_TAG		  = "GCF";
	public static final String NAME_SEPARATOR = "::";
	
	// JSON Constants
	public static final String DEVICE_TAG = "device";
	public static final String DEVICE_ID  = "deviceID";
	public static final String TIMESTAMP  = "timestamp";
	public static final String CONTEXTS   = "contextproviders";
	public static final String DEVICES	  = "devices";
	
	// Intents (and Extras)
	protected static final String ACTION_USER_CONTEXT_DOWNLOADED 	   = "USER_CONTEXT_DOWNLOADED";
	protected static final String ACTION_OTHER_USER_CONTEXT_DOWNLOADED = "OTHER_USER_CONTEXT_DOWNLOADED";
	protected static final String ACTION_BLUETOOTH_SCAN_UPDATE 		   = "BT_UPDATE";
	public    static final String ACTION_USER_CONTEXT_UPDATED	 	   = "USER_CONTEXT_UPDATED";
	public    static final String ACTION_OTHER_USER_CONTEXT_RECEIVED   = "OTHER_USER_CONTEXT_RECEIVED";
	public 	  static final String ACTION_COMPUTE_INSTRUCTION_RECEIVED  = "PCP_COMPUTE_INSTRUCTION";
	public    static final String EXTRA_USER_CONTEXT				   = "USER_CONTEXT";
	public    static final String EXTRA_OTHER_USER_CONTEXT		       = "OTHER_USER_CONTEXT";
	public    static final String EXTRA_IS_NEW_CONTEXT				   = "NEW_CONTEXT";
	public    static final String EXTRA_RSSI						   = "RSSI";
		
	protected static final String EXTRA_OTHER_USER_ID		    	   = "OTHER_USER_ID";
	protected static final String BLUETOOTH_SCAN_RESULT       		   = "BT_RESULT";
	protected static final String BLUETOOTH_RSSI_RESULT       		   = "RSSI_RESULT";
	
	// Group Context Framework
	private GroupContextManager gcm;
	
	// Application Context
	private Context context;
	
	// HTTP Toolkit
	private HttpToolkit httpToolkit;
		
	// Bluewave Components
	private boolean					keepScanning;
	private String					url;
	private BluetoothScanner		bluetoothScanner;
	private PersonalContextProvider pcp;
	private ContextListener			contextListener;	
	
	// Bluewave Credentials
	private String 	 appID;
	private String[] contextsRequested;
	
	/**
	 * Constructor
	 * @param context - the Android application context
	 * @param gcm - the group context manager
	 * @param url - the base URL containing the bluewave php files
	 * @param discoverable - sets whether or not the bluetooth adapter is discoverable
	 */
	public BluewaveManager(Context context, GroupContextManager gcm, String url)
	{
		this.context = context;
		this.gcm 	 = gcm;
		this.url	 = url;
		
		// Creates the HTTP Toolkit
		this.httpToolkit = new HttpToolkit((Application)context.getApplicationContext());
		
		// Creates the Bluetooth Scanner
		this.bluetoothScanner = new BluetoothScanner(this, context);
				
		// Creates the Personal Context Provider
		this.pcp = new PersonalContextProvider(context, this, gcm, httpToolkit, url);
		this.gcm.registerContextProvider(this.pcp);
		
		// Initializes Credentials
		this.appID 			   = null;
		this.contextsRequested = null;
		
		// Creates the Context Listener
		this.contextListener = new ContextListener(context, this, httpToolkit);
		
		// Updates the Device's Bluetooth Name to Note that it has been Initialized
		updateBluetoothName();
	}
	
	/**
	 * This is used by applications to identify themselves to other devices
	 * @param appID
	 * @param contextsRequested
	 */
	public void receiveContext(String appID, String[] contextsRequested)
	{
		this.appID 			   = appID;
		this.contextsRequested = contextsRequested;
	}
	
	/**
	 * Gets the Personal Context Provider Used by Bluewave
	 * @return
	 */
	public PersonalContextProvider getPersonalContextProvider()
	{
		return pcp;
	}
	
	/**
	 * Begins Bluewave Scanning and Analyzing
	 */
	public void startScan(int scanInterval)
	{
		keepScanning = true;
		
		// Sets the Device's Discoverable State
		this.setDiscoverable(true);
		
		// Starts the Scan
		this.bluetoothScanner.startBluetoothScan(scanInterval);
	}
	
	/**
	 * Stops Bluewave Scanning
	 */
	public void stopScan()
	{
		keepScanning = false;
		
		bluetoothScanner.stopBluetoothScan();
	}

	/**
	 * Begins Bluewave LE Scanning and Analyzing
	 * @param scanInterval
	 */
	public void startLEScan(int scanInterval)
	{
		keepScanning = true;
		
		this.bluetoothScanner.startBluetoothLowEnergyScan(scanInterval);
	}

	/**
	 * Stopes Bluewave LE Scanning
	 */
	public void stopLEScan()
	{
		keepScanning = false;
		
		bluetoothScanner.stopBluetoothScan();
	}
	
	/**
	 * Returns TRUE if Bluewave is Scanning, FALSE otherwise
	 * @return
	 */
	public boolean isScanning()
	{
		return bluetoothScanner.isScanning();
	}
	
	/**
	 * Returns TRUE if Bluewave Scan Should Continue, FALSE otherwise
	 * @return
	 */
	public boolean keepScanning()
	{
		return keepScanning;
	}
	
	/**
	 * Updates the Device's Bluetooth Name
	 */
	public void updateBluetoothName()
	{
		// Creates the Main Bluetooth Name
		String bluetoothName = ID_TAG + NAME_SEPARATOR + gcm.getDeviceID() + NAME_SEPARATOR + pcp.getPublicContextURL() + NAME_SEPARATOR + pcp.getContextAccessKey();
		
		// Updates the Bluetooth Name
		bluetoothScanner.setBluetoothName(bluetoothName);
	}
	
	/**
	 * Returns the Device's Bluetooth Name
	 * @return
	 */
	public String getBluetoothName()
	{
		return bluetoothScanner.getBluetoothName();
	}
	
	/**
	 * Returns the Last Reported Context for a Given Device ID
	 * @param deviceID
	 * @return
	 */
	public JSONContextParser getContext(String deviceID)
	{
		return this.contextListener.getContext(deviceID);
	}
	
	/**
	 * Returns TRUE if the device is discoverable
	 * @return
	 */
	public boolean isDiscoverable()
	{
		return this.bluetoothScanner.isDiscoverable();
	}
	
	/**
	 * Disables Specific Bluewave Functionality
	 */
	public void setDiscoverable(boolean value)
	{
		this.bluetoothScanner.setDiscoverable(value);
	}
	
	/**
	 * Returns All Nearby Devices Since the Specified Number of Seconds
	 * @param seconds
	 * @return
	 */
	public String[] getNearbyDevices(int seconds)
	{
		Date since = new Date(new Date().getTime() - (seconds * 1000));
		return bluetoothScanner.getNearbyDevices(since);
	}

	/**
	 * Returns the RSSI Value (bigger = closer) of a given Bluetooth ID
	 * Returns 0 if no value is found
	 * @param deviceName
	 * @return
	 */
	public short getRSSI(String deviceID)
	{
		return bluetoothScanner.getRSSI(deviceID);
	}
	
	/**
	 * Returns this Application's Bluewave App ID
	 * You get this by registering with the Bluewave site
	 * @return
	 */
	public String getAppID()
	{
		return appID;
	}
	
	/**
	 * Returns the Contexts Requested by this App
	 * @return
	 */
	public String[] getContextsRequested()
	{
		return contextsRequested;
	}
	
	/**
	 * Determines if a Bluetooth Name is Bluewave Compatible
	 * @param bluetoothName
	 * @return
	 */
	public static boolean isBluewaveName(String bluetoothName)
	{
		if (bluetoothName != null)
		{
			String[] nameComponents = bluetoothName.split(NAME_SEPARATOR);
			boolean result = (nameComponents.length == 4 && nameComponents[0].equals(ID_TAG));
			
			//Log.d(LOG_NAME, bluetoothName + " - " + result);
			return result;	
		}
		else
		{
			return false;
		}
	}

	/**
	 * Returns a Device's Name (compensates for Bluewave Formatting)
	 * @param bluetoothName
	 * @return
	 */
	public static String getDeviceName(String bluetoothName)
	{
		if (BluewaveManager.isBluewaveName(bluetoothName))
		{
			String[] nameComponents = bluetoothName.split(NAME_SEPARATOR);
			return nameComponents[1];
		}
		else
		{
			return bluetoothName;
		}
	}
}
