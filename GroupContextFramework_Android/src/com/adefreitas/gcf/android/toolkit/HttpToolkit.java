package com.adefreitas.gcf.android.toolkit;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

/**
 * Class Used to Perform HTTP Operations in the Background
 * (Updated to Use the Volley Library)
 * @author adefreit
 */
@SuppressLint("NewApi")
public class HttpToolkit
{
	// Intents (Actions and Extras) Provided by this Intent
	public static final String EXTRA_HTTP_URL 	   = "HTTP_URL";
	public static final String EXTRA_HTTP_RESPONSE = "HTTP_RESPONSE";
	
	// Internal Constants
	private static final boolean DEBUG	  = true;
	private static final String  LOG_NAME = "HTTP_TOOLKIT";
		
	// A Broadcaster for Intents
	private ContextWrapper cw;
	
	// The Queue of HTTP Requests
	private RequestQueue queue;
	
	// The Download Manager
	private DownloadManager manager;
	
	/**
	 * Constructor
	 * @param cw
	 */
	public HttpToolkit(ContextWrapper cw)
	{
		this.cw 	 = cw;
		this.queue	 = Volley.newRequestQueue(cw);
		this.manager = (DownloadManager)cw.getSystemService(Context.DOWNLOAD_SERVICE);
	}
	
	/**
	 * HTTP Get Request
	 * @param url the URL to the website
	 * @param callbackIntent the Android Intent to be sent when the request is complete
	 */
	public void get(final String url, final String callbackIntent)
	{		
		// Creates the GET Request
		StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
	      new Response.Listener<String>() 
	      {
		    @Override
		    public void onResponse(String response) 
		    {
				deliverResponse(callbackIntent, url, response);
		    }
		  }, 
		  new Response.ErrorListener() 
		  {
		    @Override
		    public void onErrorResponse(VolleyError error) 
		    {
		        // Error handling
		    	log(LOG_NAME, "HTTP GET ERROR: " + url);
		    	log(LOG_NAME, "ERROR MESSAGE:  " + error.getMessage());
		    }
		});
		
		// Add the request to the queue
		queue.add(stringRequest);
	}

	/**
	 * HTTP Post Request
	 * @param url the URL to the website
	 * @param body the data to be included in the HTML body
	 * @param callbackIntent the Android Intent to be sent when the request is complete
	 */
	public void post(final String url, final Map<String, String> map, final String callbackIntent)
	{
		// Creates the POST Request
		StringRequest postRequest = new StringRequest(Request.Method.POST, url,
		        new Response.Listener<String>() 
		        {
		            @Override
		            public void onResponse(String response) 
		            {
		            	deliverResponse(callbackIntent, url, response);
		            }
		        },
		        new Response.ErrorListener() {
		            @Override
		            public void onErrorResponse(VolleyError error) 
		            {
				        // Error handling
				    	log(LOG_NAME, "HTTP POST ERROR: " + url);
				    	log(LOG_NAME, "ERROR MESSAGE:  " + error.getMessage());
		            }
		        }
		) {
		    @Override
		    protected Map<String, String> getParams()
		    {
		    	return map;
//		        Map<String, String>  params = new HashMap<>();
//		        params.put("data", body);
//		        return params;
		    }
		};

		// Adds the Request to the Queue
		queue.add(postRequest);
	}
		
	/**
	 * HTTP Put Request
	 * @param url the URL to the website
	 * @param body the data to be included in the HTML request
	 * @param callbackIntent the Android Intent to be sent when the request is complete
	 */
	public void put(final String url, final String body, final String callbackIntent)
	{
		// Creates the POST Request
		StringRequest putRequest = new StringRequest(Request.Method.PUT, url,
		        new Response.Listener<String>() 
		        {
		            @Override
		            public void onResponse(String response) 
		            {
		            	deliverResponse(callbackIntent, url, response);
		            }
		        },
		        new Response.ErrorListener() {
		            @Override
		            public void onErrorResponse(VolleyError error) 
		            {
				        // Error handling
				    	log(LOG_NAME, "HTTP POST ERROR: " + url);
				    	log(LOG_NAME, "ERROR MESSAGE:  " + error.getMessage());
		            }
		        }
		) {
		    @Override
		    protected Map<String, String> getParams()
		    {
		        Map<String, String>  params = new HashMap<>();
		        params.put("data", body);
		        return params;
		    }
		};

		// Adds the Request to the Queue
		queue.add(putRequest);
	}
	
	/**
	 * HTTP Download
	 * DownloadManager.ACTION_DOWNLOAD_COMPLETE is Broadcast when finished.  Use the getDownloadedFile() method to obtain the name of the file.
	 * @param url the URL to the file to be downloaded
	 * @param destination the exact location (in Android's file system) where the file is to be downloaded, to include the file name
	 * @param showProgressBar TRUE if you want to see a progress bar, FALSE otherwise
	 */
	@SuppressLint("NewApi")
	public void download(final String url, final String destination, final boolean showProgressBar)
	{	
	    File   dir   	= new File(destination.substring(0, destination.lastIndexOf("/") + 1));
	    String filename = (destination.substring(destination.lastIndexOf("/")+1));
	    File   f     	= new File(dir + "/" + filename);

    	try 
	    {	
    		DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
    		request.setTitle("Downloading File");
    		request.setDescription(url);
    		    		
    		log(LOG_NAME, "Downloading . . .\nURL: " + url + "\nDestination: " + f.getAbsolutePath());
    		
    		if (!dir.exists())
    		{
    			dir.mkdirs();
    		}
    		
    		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) 
    		{
    		    request.allowScanningByMediaScanner();
    		    
    		    if (showProgressBar)
    		    {
    		    	request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);	
    		    }
    		    else
    		    {
    		    	request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
    		    }
    		}
    		
    		Uri uri = Uri.fromFile(f);
    		request.setDestinationUri(uri);

    		// get download service and enqueue file
    		manager.enqueue(request);
	    }
    	catch (Exception ex)
    	{
    		Log.e(LOG_NAME, "Downloading Problem: " + ex.getMessage());
    		ex.printStackTrace();
    	}
	}
	
	/**
	 * Returns the Uri of a downloaded file
	 * @param id
	 * @return
	 */
	public Uri getDownloadedFile(long id)
	{
		return manager.getUriForDownloadedFile(id);
	}
	
	private void deliverResponse(String callbackIntent, String url, String response)
	{
		if (response != null)
		{
			// Creates a Substring of the Response to Display in the Log
			String s = response.substring(0, Math.min(response.length(), 100));
			
			// Creates the Intent
		 	Intent dataDeliveryIntent = new Intent(callbackIntent);
		 	log(LOG_NAME, "PREPARING CALLBACK: " + callbackIntent);
		 	
			log(LOG_NAME, "REQUEST: " + url);
		    log(LOG_NAME, "RESPONSE: " + ((response != null) ? s + " [" + response.length() + " bytes]" : "null"));	
		 	
		 	// Includes the HTTP Response
		 	dataDeliveryIntent.putExtra(EXTRA_HTTP_URL, url);
		 	dataDeliveryIntent.putExtra(EXTRA_HTTP_RESPONSE, response);
		 	
		 	// Delivers the HTTP Response
		 	cw.sendBroadcast(dataDeliveryIntent);	
		}
	}
	
	private static void log(String tag, String text)
	{
		if (DEBUG)
		{
			Log.d(tag, text);
		}
	}
	
}
