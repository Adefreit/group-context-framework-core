package com.example.impromptu.beacon;

import java.util.Arrays;
import java.util.Date;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.adefreitas.gcf.ContextProvider;
import com.adefreitas.gcf.android.AndroidCommManager;
import com.adefreitas.gcf.android.AndroidGroupContextManager;
import com.adefreitas.gcf.android.GCFService;
import com.adefreitas.gcf.android.bluewave.BluewaveManager;
import com.adefreitas.gcf.impromptu.ApplicationProvider;

@SuppressLint("SetJavaScriptEnabled")
public class MainActivity extends AppCompatActivity
{
	// A Link to the Main Android Application Object
	private GCFApplication application;
		
	// Intent Receiver
	private IntentFilter   filter;
	private IntentReceiver intentReceiver;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		this.application = (GCFApplication)this.getApplication();
		
		// Sets Device Configuration
		TextView txtDeviceID 		  = (TextView)this.findViewById(R.id.txtDeviceID);
		TextView txtBluetoothName 	  = (TextView)this.findViewById(R.id.txtBluetoothName);
		TextView txtAppID 			  = (TextView)this.findViewById(R.id.txtAppID);
		TextView txtContextsRequested = (TextView)this.findViewById(R.id.txtContextsRequested);
		
		txtDeviceID.setText(application.getGroupContextManager().getDeviceID());
		txtBluetoothName.setText(application.getBluewaveManager().getBluetoothName());
		txtAppID.setText(application.getBluewaveManager().getAppID());
		txtContextsRequested.setText(Arrays.toString(application.getBluewaveManager().getContextsRequested()));
		
		// Creates and Registers the Intent Filter
		this.intentReceiver = new IntentReceiver();
		this.filter 		= new IntentFilter();
		filter.addAction(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluewaveManager.ACTION_OTHER_USER_CONTEXT_RECEIVED);
		this.registerReceiver(intentReceiver, filter);
	}

	protected void onResume()
	{
		super.onResume();
		
		this.registerReceiver(intentReceiver, filter);
		
		displayContextProviders();
		displayBluetoothScanResults();
	}
	
	protected void onPause()
	{
		super.onPause();
		
		this.unregisterReceiver(intentReceiver);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		
		if (id == R.id.action_settings) 
		{
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}

	private void displayContextProviders()
	{
		// Gets the Layout Container that Holds the Context Providers
		LinearLayout layoutContextProviders = (LinearLayout)this.findViewById(R.id.layoutContextProviders);
		layoutContextProviders.removeAllViews();
		
		// Gets the Custom Layout that Represents a Context Provider 
		LayoutInflater inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		for (ContextProvider p : application.getGroupContextManager().getRegisteredProviders())
		{
			View      cpView 					    = inflater.inflate(R.layout.layout_contextprovider, null);
			TextView  txtContextProviderName        = (TextView)cpView.findViewById(R.id.txtContextProviderName);
			TextView  txtContextProviderDescription = (TextView)cpView.findViewById(R.id.txtContextProviderDescription);
			ImageView imgIcon			  		    = (ImageView)cpView.findViewById(R.id.imgIcon);
			
			txtContextProviderName.setText(p.getContextType());
			
			if (p instanceof ApplicationProvider)
			{
				ApplicationProvider ap = (ApplicationProvider)p;
				imgIcon.setImageResource(R.drawable.impromptu_app);
				txtContextProviderDescription.setText(ap.getDescription());
			}
			else
			{
				imgIcon.setImageResource(R.drawable.gcf_1);
				txtContextProviderDescription.setText(p.getDescription());
			}
			
			layoutContextProviders.addView(cpView);
		}	
	}
	
	private void displayBluetoothScanResults()
	{
		final int TIME_TO_REMEMBER_DEVICE = 60;
		
		// Gets the Layout Container that Holds the Context Providers
		LinearLayout layoutBluetooth = (LinearLayout)this.findViewById(R.id.layoutBluetooth);
		layoutBluetooth.removeAllViews();
		
		TextView txtTimestamp = (TextView)this.findViewById(R.id.txtTimestamp);
		txtTimestamp.setText("Devices Detected Over Last " + TIME_TO_REMEMBER_DEVICE + " Seconds");
		
		// Gets the Custom Layout that Represents a Context Provider 
		LayoutInflater inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		for (String deviceID : application.getBluewaveManager().getNearbyDevices(TIME_TO_REMEMBER_DEVICE))
		{
			if (deviceID != null)
			{
				View      btView 		      = inflater.inflate(R.layout.layout_bluetoothdevice, null);
				TextView  txtBluetoothID      = (TextView)btView.findViewById(R.id.txtBluetoothID);
				TextView  txtBluetoothDetails = (TextView)btView.findViewById(R.id.txtBluetoothDetails);
				ImageView imgIcon			  = (ImageView)btView.findViewById(R.id.imgIcon);
				
				txtBluetoothID.setText(deviceID + " [RSSI = " + application.getBluewaveManager().getRSSI(deviceID) + "]");
				txtBluetoothDetails.setText(application.getLogEntry(deviceID));
				
				if (application.getBluewaveManager().getContext(deviceID) != null)
				{
					imgIcon.setImageResource(R.drawable.gcf);
				}
				else
				{
					imgIcon.setImageResource(R.drawable.bluetooth);
				}
				
				layoutBluetooth.addView(btView);	
			}
		}
	}

	public class IntentReceiver extends BroadcastReceiver
	{		
		@Override
		public void onReceive(Context context, Intent intent) 
		{	
			if (intent.getAction().equals(BluetoothDevice.ACTION_FOUND) || 
				intent.getAction().equals(BluewaveManager.ACTION_OTHER_USER_CONTEXT_RECEIVED))
			{
				displayBluetoothScanResults();
			}
			else
			{
				Log.e("", "Unknown Action: " + intent.getAction());
			}
		}
	}
}
