package com.adefreitas.gcf.android.toolkit;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import android.content.ContextWrapper;
import android.content.Intent;
import android.os.AsyncTask;

import com.adefreitas.gcf.FrameworkSettings;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class SftpToolkit extends CloudStorageToolkit
{
	private ContextWrapper cw;
	
	// User Credentials
	private String USERNAME;
	private String PASSWORD;
	private String SERVER;
	private int    PORT;
	
	/**
	 * Constructor
	 * @param cw
	 */
 	public SftpToolkit(String server, int port, String username, String password, ContextWrapper cw)
	{
 		this.SERVER   = server;
 		this.PORT     = port;
 		this.USERNAME = username;
 		this.PASSWORD = password;
		this.cw = cw;
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
		System.out.println("Sftp uploading " + file.getAbsolutePath() + " to " + folderPath);
		
		new UploadFilesTask().execute(new UploadInstruction(folderPath, file, callbackIntent));
	}

	public void uploadFiles(String folderPath, File[] files, String callbackIntentIndividual)
	{
		UploadInstruction[] instructions = new UploadInstruction[files.length];
		
		for (int i=0; i<instructions.length; i++)
		{
			instructions[i] = new UploadInstruction(folderPath, files[i], callbackIntentIndividual);
		}
		
		new UploadFilesTask().execute(instructions);
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
		
		System.out.println("Sftp downloading " + fullPath + " to " + destinationFolderPath);
		
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
	
	public void downloadFilesToLocation(String[] fullPaths, String destinationFolderPath, String callbackIntentIndividual)
	{
		DownloadInstruction[] instructions = new DownloadInstruction[fullPaths.length];
		
		for (int i=0; i<instructions.length; i++)
		{
			instructions[i] = new DownloadInstruction(fullPaths[i], destinationFolderPath, callbackIntentIndividual);
		}
		
		new DownloadFilesTask().execute(instructions);
	}
	
	// HELPER CLASSES ----------------------------------------------------------------
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
	
	// ASYNCHRONOUS TASKS
	private class UploadFilesTask extends AsyncTask<UploadInstruction, Integer, Long> 
	{
		private ArrayList<UploadInstruction> uploadInstructions = new ArrayList<UploadInstruction>(); 
		private Date 						 startTime 			= new Date();

		protected Long doInBackground(UploadInstruction... instructions) 
	    {
    		Session 	session = null;
    	    Channel 	channel = null;
    	    ChannelSftp sftp	= null;
    	    JSch 		ssh 	= null;
    	    
	        for (int i = 0; i < instructions.length; i++) 
	        {
	        	UploadInstruction instruction = instructions[i];
	        	String 			  filename    = instruction.getFile().getAbsolutePath().substring(instruction.getFile().getAbsolutePath().lastIndexOf("/") + 1);
	        	
	        	try
	        	{
	        		if (session == null || channel == null || sftp == null || ssh == null)
	        		{
	        			System.out.println("Creating SFTP Connection");
	        			
	        			ssh = new JSch();
	        	        
		    	        session = ssh.getSession(USERNAME, SERVER, PORT);
		    	        session.setConfig("StrictHostKeyChecking", "no");
		    	        session.setPassword(PASSWORD);
		    	        session.connect();
		    	        
		    	        channel = session.openChannel("sftp");
		    	        channel.connect();
		    	        
		    	        sftp = (ChannelSftp)channel;
	        		}
	        		
	        		// Performs the Actual SFTP Operation
	    	        sftp.put(instruction.getFile().getAbsolutePath(), instruction.getDestination() + filename);
	    	        
	    	        // Creates an Intent for Individual Files
		 	 		Intent dataUploadIntent = new Intent(instruction.getCallbackIntent());
		 	    	 
		 	 		// Includes the File Paths
		 	 		dataUploadIntent.putExtra(EXTRA_CLOUD_UPLOAD_SOURCE, instruction.getSource());
		 	 		dataUploadIntent.putExtra(EXTRA_CLOUD_UPLOAD_PATH, instruction.getDestination() + instruction.getFile().getName());
		 	 		dataUploadIntent.putExtra(EXTRA_CLOUD_TIME_ELAPSED, new Date().getTime() - startTime.getTime());
		 	 		
		 	 		System.out.println("BROADCASTING INTENT: " + dataUploadIntent.getAction());
		 	 		
		 	    	// Sends the Intent
		 	 		cw.sendBroadcast(dataUploadIntent);
	        	}
	        	catch (Exception ex)
	        	{
	        		System.out.println("Problem occurred while uploading " + instruction.getSource() + " to " + instruction.getDestination());
	        		ex.printStackTrace();
	        	}
	        	finally
	        	{
	        		uploadInstructions.add(instruction);
	        	}
	        	
	        	// Updates Progress
	        	publishProgress((int) ((i / (float) instructions.length) * 100));
	             
	            // Escape early if cancel() is called
	            if (isCancelled()) break;
	        }
	        
	        // Kills all Connections
        	if (channel != null) 
	        {
	            channel.disconnect();
	        }
	        if (session != null) 
	        {
	            session.disconnect();
	        }
	        if (sftp != null)
	        {
	        	sftp.disconnect();
	        }
	        
	        return (long)instructions.length;
	     }

		protected void onProgressUpdate(Integer... progress) 
	    {
			// Do Nothing
	    }

	    protected void onPostExecute(Long result) 
	    {

	    }
	 }
	
	/**
	 * Asynchronous Task to Download Files
	 * @author adefreit
	 */
	private class DownloadFilesTask extends AsyncTask<DownloadInstruction, Integer, Long> 
	{
		private ArrayList<DownloadInstruction> downloadInstructions = new ArrayList<DownloadInstruction>(); 
		private Date 						   startTime 			= new Date();
		
		protected Long doInBackground(DownloadInstruction... instructions) 
	    {
        	Session 	session = null;
    	    Channel 	channel = null;
    	    ChannelSftp sftp    = null;
			
	        for (int i = 0; i < instructions.length; i++) 
	        {
	        	DownloadInstruction instruction = instructions[i];
	        	        	
	        	try
	        	{
	        		if (session == null || channel == null || sftp == null)
	        		{
	        			JSch ssh = new JSch();
		    	        session = ssh.getSession(USERNAME, SERVER, PORT);
		    	        session.setConfig("StrictHostKeyChecking", "no");
		    	        session.setPassword(PASSWORD);
		    	        session.connect();
		    	        
		    	        channel = session.openChannel("sftp");
		    	        channel.connect();
		    	        
		    	        sftp = (ChannelSftp)channel;
	        		}
	    	        
	    	        sftp.get(instruction.getSource(), instruction.getDestination());
	        	}
	        	catch (Exception ex)
	        	{
	        		System.out.println("Problem occurred while downloading " + instruction.getSource() + " to " + instruction.getDestination());
	        		ex.printStackTrace();
	        	}
	        	finally
	        	{   
				    downloadInstructions.add(instruction);
	        	}
	        	
	        	// Updates Progress
	        	publishProgress((int) ((i / (float) instructions.length) * 100));
	             
	            // Escape early if cancel() is called
	            if (isCancelled()) break;
	        }
	        
	        // Kills the Connections
    		if (channel != null) 
	        {
	            channel.disconnect();
	        }
	        if (session != null) 
	        {
	            session.disconnect();
	        }
	        if (sftp != null)
	        {
	        	sftp.disconnect();
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
		 		dataDeliveryIntent.putExtra(EXTRA_CLOUD_TIME_ELAPSED, new Date().getTime() - startTime.getTime());
		 		
		    	// Sends the Intent
		 		cw.sendBroadcast(dataDeliveryIntent);
	    	 }
	     }
	 }
}
