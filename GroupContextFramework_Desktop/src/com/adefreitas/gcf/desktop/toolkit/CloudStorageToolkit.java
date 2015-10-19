package com.adefreitas.gcf.desktop.toolkit;

import java.io.File;
import java.util.Date;

public abstract class CloudStorageToolkit 
{
	private static String DOWNLOAD_DIRECTORY = "";

	public File getDownloadDirectory() 
	{
		File storageDir = new File(DOWNLOAD_DIRECTORY);
        
		if (storageDir != null) 
		{
              if (!storageDir.mkdirs()) 
              {
                    if (!storageDir.exists())
                    {
                          System.out.println("failed to create directory: " + DOWNLOAD_DIRECTORY);
                          return null;
                    }
              }
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
	
	public abstract void downloadFile(String sourcePath, String destinationPath);

	public abstract Date getLastModified(String path);
}
