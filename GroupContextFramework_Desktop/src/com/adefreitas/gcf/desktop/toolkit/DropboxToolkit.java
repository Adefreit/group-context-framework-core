package com.adefreitas.gcf.desktop.toolkit;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import com.dropbox.core.DbxAppInfo;
import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxEntry;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.DbxWebAuthNoRedirect;
import com.dropbox.core.DbxWriteMode;

public class DropboxToolkit extends CloudStorageToolkit
{	
	// Debug Flag
	private boolean DEBUG = false;
	
	private String APP_KEY;
	private String APP_SECRET;
	private String AUTH_TOKEN;
	
	private DbxAppInfo 			 appInfo;
	private DbxRequestConfig 	 config;
	private DbxWebAuthNoRedirect webAuth;
	private DbxAuthFinish 		 authFinish;
	private DbxClient 			 client;
	
	public DropboxToolkit(String appKey, String appSecret, String authToken)
	{	
		APP_KEY    = appKey;
		APP_SECRET = appSecret;
		AUTH_TOKEN = authToken;
		
		try
		{
			appInfo = new DbxAppInfo(APP_KEY, APP_SECRET);
			config  = new DbxRequestConfig("GlassExplorer/1.0", Locale.getDefault().toString());
			webAuth = new DbxWebAuthNoRedirect(config, appInfo);
			//print("Explorer", webAuth.start());
			//authFinish = webAuth.finish(APP_CODE);
			client  = new DbxClient(config, AUTH_TOKEN);	
			//print("Explorer", "Token: " + authFinish.accessToken);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public void uploadFile(String path, File file)
	{
		final String dropboxPath = path.endsWith("/") ? path : path + "/";
		final File   dropboxFile = file;
		
		Thread t = new Thread()
		{
			public void run()
			{
				try
				{
					FileInputStream inputStream = new FileInputStream(dropboxFile);
				    DbxEntry.File uploadedFile  = client.uploadFile(dropboxPath + dropboxFile.getName(), DbxWriteMode.force(), dropboxFile.length(), inputStream);
				    print("Uploaded: " + uploadedFile.toString());
				    inputStream.close();	
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}
		};

		t.start();
	}
	
	public void downloadFile(String sourcePath, String destinationPath)
	{
		// Makes Sure that the Destination Location Exists
		String folderPath 		 = destinationPath.substring(0, destinationPath.lastIndexOf("/") + 1);
		File   destinationFolder = new File(folderPath);
					
		try
		{
			if (!destinationFolder.exists())
			{
				print("Folder " + destinationFolder + " does not exist.  Creating.");
				destinationFolder.mkdirs();
			}
			
			// Performs the Download
			print("Trying to download " + sourcePath + " to " + destinationPath);
			FileOutputStream outputStream = new FileOutputStream(destinationPath);
			DbxEntry.File downloadedFile  = client.getFile(sourcePath, null, outputStream);
			outputStream.close();
			print("  SUCCESS!");
		}
		catch (Exception ex)
		{
			print("A problem occurred while downloading " + sourcePath + " to " + destinationPath);
			ex.printStackTrace();
		}
	}
	
	public String[] getFileContents(String path)
	{
		ArrayList<String> result = new ArrayList<String>();
		
		try
		{
			DbxEntry.WithChildren listing = client.getMetadataWithChildren(path);
			
			for (DbxEntry child : listing.children) 
			{
			    if (child.isFile())
			    {
			    	result.add(child.name);
			    }
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		return result.toArray(new String[0]);
	}
	
	public String getMostRecent(String path, String extension)
	{
		String rootName = null;
		
		try
		{
			DbxEntry.WithChildren listing = client.getMetadataWithChildren(path);
			
			Date mostRecentEntry = new Date(0);
			
			for (DbxEntry child : listing.children) 
			{
			    if (child.isFile() && mostRecentEntry.getTime() < child.asFile().lastModified.getTime())
			    {
			    	extension = (extension.startsWith(".")) ? extension : "." + extension;
			    	
			    	if (child.name.endsWith(extension))
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

	public Date getLastModified(String path)
	{
		try
		{
			DbxEntry entry = client.getMetadata(path);
			
			if (entry.isFile())
			{
				return entry.asFile().lastModified;
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		return null;
	}

	public void setDebug(boolean newValue)
	{
		this.DEBUG = newValue;
		System.out.println("DropboxToolkit Debug set to " + DEBUG);
	}
	
	private void print(String s)
	{
		if (DEBUG)
		{
			System.out.println(s);
		}
	}
}
