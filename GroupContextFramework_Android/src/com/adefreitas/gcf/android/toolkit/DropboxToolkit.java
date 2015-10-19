package com.adefreitas.gcf.android.toolkit;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import android.content.ContextWrapper;
import android.content.Intent;
import android.os.AsyncTask;

import com.dropbox.core.DbxAppInfo;
import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxEntry;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.DbxWebAuthNoRedirect;
import com.dropbox.core.DbxWriteMode;

public class DropboxToolkit extends CloudStorageToolkit
{	
	private static final String DROPBOX_LOG_NAME 		  = "Dropbox";
	
	private ContextWrapper cw;
	
	private String APP_KEY;
	private String APP_SECRET;
	private String AUTH_TOKEN;
	
	private DbxAppInfo 			 appInfo;
	private DbxRequestConfig 	 config;
	private DbxWebAuthNoRedirect webAuth;
	private DbxAuthFinish 		 authFinish;
	private DbxClient 			 client;
	
	/**
	 * Constructor
	 * @param appKey
	 * @param appSecret
	 * @param authToken
	 */
	public DropboxToolkit(ContextWrapper cw, String appKey, String appSecret, String authToken)
	{	
		this.cw   		= cw;
		this.APP_KEY    = appKey;
		this.APP_SECRET = appSecret;
		this.AUTH_TOKEN = authToken;
		
		try
		{
			appInfo = new DbxAppInfo(APP_KEY, APP_SECRET);
			config  = new DbxRequestConfig("GlassExplorer/1.0", Locale.getDefault().toString());
			webAuth = new DbxWebAuthNoRedirect(config, appInfo);
			//Log.d("Explorer", webAuth.start());
			//authFinish = webAuth.finish(APP_CODE);
			client  = new DbxClient(config, AUTH_TOKEN);	
			//Log.d("Explorer", "Token: " + authFinish.accessToken);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	/**
	 * Uploads a File to the Cloud with the Default Callback Intent
	 * @param folderPath
	 * @param file
	 */
	public void uploadFile(String folderPath, File file)
	{		
		uploadFile(folderPath, file, ACTION_CLOUD_UPLOAD_COMPLETE);
	}

	/**
	 * Uploads a File to the Cloud with a Custom Callback Intent
	 * @param folderPath
	 * @param file
	 * @param callbackIntent
	 */
	public void uploadFile(String folderPath, File file, String callbackIntent)
	{
		System.out.println("Dropbox uploading " + file.getAbsolutePath() + " to " + folderPath);
		
		new UploadFilesTask().execute(new UploadInstruction(folderPath, file, callbackIntent));
	}
	
	/**
	 * Downloads a File from the Cloud Into the Default Folder
	 * @param fullPath
	 */
	public void downloadFile(String fullPath)
	{
		downloadFileToLocation(fullPath, this.getDownloadDirectory().getAbsolutePath(), ACTION_CLOUD_DOWNLOAD_COMPLETE);
	}
	
	public void downloadFile(String fullPath, String callbackIntent)
	{
		downloadFileToLocation(fullPath, this.getDownloadDirectory().getAbsolutePath(), callbackIntent);
	}
	
	/**
	 * Downloads a File from the Cloud to the Specified Location
	 * @param fullPath
	 * @param destinationFilename
	 */
	public void downloadFileToLocation(String fullPath, String destinationFolderPath)
	{
		downloadFileToLocation(fullPath, destinationFolderPath, ACTION_CLOUD_DOWNLOAD_COMPLETE);
	}
	
	public void downloadFileToLocation(String fullPath, String destinationFolderPath, String callbackIntent)
	{
		destinationFolderPath += (destinationFolderPath.endsWith("/")) ? "" : "/";
		
		System.out.println("Dropbox downloading " + fullPath + " to " + destinationFolderPath);
		
		String filename = fullPath.substring(fullPath.lastIndexOf("/") + 1);
		
		// Makes Sure the Folder Exists
		File folder = new File(destinationFolderPath);
		
		if (!folder.exists())
		{
			folder.mkdir();
		}
		
		// Makes Sure an Empty File Exists
		File file = new File(folder.getAbsoluteFile() + "/" + filename);
		
		try
		{
			if (!file.exists())
			{
				file.createNewFile();
			}
		}
		catch (Exception ex)
		{
			System.out.println("Problem occurred while creating " + fullPath);
			ex.printStackTrace();
			file.delete();
		}
		
		// Creates the Asynchronous Task that will Download the Files
		new DownloadFilesTask().execute(new DownloadInstruction(fullPath, destinationFolderPath + filename, callbackIntent));
	}
	
	/**
	 * Returns the Name of the Most Recent File in the Specified Folder
	 * @param cloudPath
	 * @return
	 */
	public String getMostRecent(String cloudPath, String extension)
	{
		String rootName = null;
		
		try
		{
			DbxEntry.WithChildren listing = client.getMetadataWithChildren(cloudPath);
			
			Date mostRecentEntry = new Date(0);
			
			for (DbxEntry child : listing.children) 
			{
			    if (child.isFile() && mostRecentEntry.getTime() < child.asFile().lastModified.getTime())
			    {
			    	// Simple check to make sure the extension starts with a "."
			    	String tmp = (extension.startsWith(".")) ? extension : "." + extension;
			    	
			    	if (child.name.endsWith(tmp))
			    	{
			    		mostRecentEntry = child.asFile().lastModified;
			    		rootName 		= child.name.substring(0, child.name.indexOf("."));
			    	}
			    }
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		return rootName;
	}

	private class UploadInstruction
	{
		private File   file;
		private String destination;
		private String callbackIntent;
		
		public UploadInstruction(String destination, File file, String callbackIntent)
		{
			this.file 			= file;
			this.destination 	= destination;
			this.callbackIntent = callbackIntent;
		}
		
		public String getDestination()
		{
			return destination;
		}
		
		public File getFile()
		{
			return file;
		}
	
		public String getSource()
		{
			if (file.exists())
			{
				return file.getAbsolutePath();
			}
			else
			{
				return null;
			}
		}
	
		public String getCallbackIntent()
		{
			return callbackIntent;
		}
	}
	
	private class DownloadInstruction
	{
		private String source;
		private String destination;
		private String callbackIntent;
		
		public DownloadInstruction(String source, String destination, String callbackIntent)
		{
			this.source 	    = source;
			this.destination    = destination;
			this.callbackIntent = callbackIntent;
		}
		
		public String getSource()
		{
			return source;
		}
		
		public String getDestination()
		{
			return destination;
		}
	
		public String getCallbackIntent()
		{
			return callbackIntent;
		}
	}
		
	/**
	 * Asynchronous Task to Download Files
	 * @author adefreit
	 */
	private class DownloadFilesTask extends AsyncTask<DownloadInstruction, Integer, Long> 
	{
		private ArrayList<DownloadInstruction> downloadInstructions = new ArrayList<DownloadInstruction>(); 
		
		protected Long doInBackground(DownloadInstruction... instructions) 
	    {
	        for (int i = 0; i < instructions.length; i++) 
	        {
	        	DownloadInstruction instruction = instructions[i];
	        	        	
	        	try
	        	{
	        		FileOutputStream outputStream = new FileOutputStream(instruction.getDestination());        		
	        		client.getFile(instruction.getSource(), null, outputStream);
				    outputStream.close();
	        	}
	        	catch (Exception ex)
	        	{
	        		System.out.println("A problem occurred when downloading: " + instruction.getSource());
	        		ex.printStackTrace();
	        	}
			    
	        	publishProgress((int) ((i / (float) instructions.length) * 100));
	             
			    // Remembers the Download Paths
			    downloadInstructions.add(instruction);
	        	
	            // Escape early if cancel() is called
	            if (isCancelled()) break;
	        }
	        
	        return (long)instructions.length;
	     }

	     protected void onProgressUpdate(Integer... progress) 
	     {
	         
	     }

	     protected void onPostExecute(Long result) 
	     {	    	 
	    	 for (DownloadInstruction instruction : downloadInstructions)
	    	 {
	    		// Creates the Intent
	 	 		Intent dataDeliveryIntent = new Intent(instruction.getCallbackIntent());
	 	 	
	 	 		System.out.println("Downloading Completed [" + instruction.source + "].  Preparing intent: " + instruction.getCallbackIntent());
	 	 		
	 	 		// Includes the File Paths (Source and Destination)
	 	 		dataDeliveryIntent.putExtra(EXTRA_CLOUD_DOWNLOAD_SOURCE, instruction.source);
		 		dataDeliveryIntent.putExtra(EXTRA_CLOUD_DOWNLOAD_PATH, instruction.destination);
		 		
		    	// Sends the Intent
		 		cw.sendBroadcast(dataDeliveryIntent);
	    	 }
	     }
	 }

	/**
	 * Asynchronous Task to Download Files
	 * @author adefreit
	 */
	private class UploadFilesTask extends AsyncTask<UploadInstruction, Integer, Long> 
	{
		private ArrayList<UploadInstruction> uploadInstructions = new ArrayList<UploadInstruction>(); 
		
		protected Long doInBackground(UploadInstruction... instructions) 
	    {
	        for (int i = 0; i < instructions.length; i++) 
	        {
	        	UploadInstruction instruction = instructions[i];
	        	
	        	try
	        	{
					FileInputStream inputStream = new FileInputStream(instruction.getFile());
				    DbxEntry.File uploadedFile  = client.uploadFile(instruction.getDestination() + instruction.getFile().getName(), DbxWriteMode.force(), instruction.getFile().length(), inputStream);
				    System.out.println("Uploading: " + uploadedFile.toString());
				    inputStream.close();	
				    
				    uploadInstructions.add(instruction);
	        	}
	        	catch (Exception ex)
	        	{
	        		ex.printStackTrace();
	        	}
			    
	        	publishProgress((int) ((i / (float) instructions.length) * 100));
	             
	            // Escape early if cancel() is called
	            if (isCancelled()) break;
	        }
	        
	        return (long)instructions.length;
	     }

		protected void onProgressUpdate(Integer... progress) 
	    {
			// Do Nothing
	    }

	    protected void onPostExecute(Long result) 
	     {
	    	 for (UploadInstruction uploadInstruction : uploadInstructions)
	    	 {
	    		// Creates the Intent
	 	 		Intent dataUploadIntent = new Intent(uploadInstruction.getCallbackIntent());
	 	    	 
	 	 		// Includes the File Paths
	 	 		dataUploadIntent.putExtra(EXTRA_CLOUD_UPLOAD_SOURCE, uploadInstruction.getSource());
	 	 		dataUploadIntent.putExtra(EXTRA_CLOUD_UPLOAD_PATH, uploadInstruction.getDestination() + uploadInstruction.getFile().getName());
	 	 		
	 	 		System.out.println("BROADCASTING INTENT: " + dataUploadIntent.getAction());
	 	 		
	 	    	// Sends the Intent
	 	 		cw.sendBroadcast(dataUploadIntent);
	    	 }
	     }
	 }
}

