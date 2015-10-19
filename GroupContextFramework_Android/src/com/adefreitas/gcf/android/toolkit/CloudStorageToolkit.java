package com.adefreitas.gcf.android.toolkit;

import java.io.File;

import android.os.Environment;
import android.util.Log;

public abstract class CloudStorageToolkit 
{
	// Intent Variables
	public static final String ACTION_CLOUD_UPLOAD_COMPLETE   = "UPLOAD_COMPLETE";
	public static final String ACTION_CLOUD_DOWNLOAD_COMPLETE = "DOWNLOAD_COMPLETE";
	public static final String EXTRA_CLOUD_UPLOAD_PATH        = "UPLOAD_PATH";
	public static final String EXTRA_CLOUD_TIME_ELAPSED       = "TIME_ELAPSED";
	public static final String EXTRA_CLOUD_DOWNLOAD_PATH      = "DOWNLOAD_PATH";
	public static final String EXTRA_CLOUD_UPLOAD_SOURCE      = "UPLOAD_SOURCE";
	public static final String EXTRA_CLOUD_DOWNLOAD_SOURCE    = "DOWNLOAD_SOURCE";
	
	private static String DOWNLOAD_DIRECTORY = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"CloudStorage").getAbsolutePath() + "/";

	public File getDownloadDirectory() 
	  {
	      File storageDir = null;
	      
	      if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
	            storageDir = new File(DOWNLOAD_DIRECTORY);
	            if (storageDir != null) {
	                  if (!storageDir.mkdirs()) {
	                        if (!storageDir.exists()){
	                              Log.e("DropboxToolkit", "failed to create directory: " + DOWNLOAD_DIRECTORY);
	                              return null;
	                        }
	                  }
	            }    
	      } 
	      else 
	      {
	            Log.e("DropboxToolkit", "External storage is not mounted READ/WRITE.");
	      }
	      return storageDir;
	  }
	
	public void setDownloadDirectory(String fullPathToDirectory)
	{
		DOWNLOAD_DIRECTORY = fullPathToDirectory;
		
		if (!DOWNLOAD_DIRECTORY.endsWith("/"))
		{
			DOWNLOAD_DIRECTORY += "/";
		}
	}
	
	public abstract void uploadFile(String folderPath, File file);
	
	public abstract void uploadFile(String folderPath, File file, String callbackIntent);
	
	public abstract void downloadFile(String fullPath);
	
	public abstract void downloadFile(String fullPath, String callbackIntent);
}
