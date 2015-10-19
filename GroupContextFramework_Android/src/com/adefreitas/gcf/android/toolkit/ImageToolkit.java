package com.adefreitas.gcf.android.toolkit;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

public class ImageToolkit 
{
	private static final String LOG_NAME = "IMAGE_TOOLKIT";
	
	/**
	 * Creating file uri to store image/video
	 * Returns the URI, or NULL if a problem is encountered
	 */
	public static Uri getOutputMediaFileUri(File file) 
	{
		try
		{
			return Uri.fromFile(file);	
		}
	    catch (Exception ex)
	    {
	    	Log.e(LOG_NAME, "Problem Getting Output Media File URI: " + ex.getMessage());
	    }
		
		// Returns null if there is a problem
		return null;
	}
		 
	/**
	 * Generate a Subdirectory and a File within the Pictures Directory 
	 * @param imageDirectoryName
	 * @param filename
	 * @return
	 */
	public static File getOutputMediaFile(String imageDirectoryName, String filename) 
	{
	    // External sdcard location
	    File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), imageDirectoryName);
	 
	    // Create the storage directory if it does not exist
	    if (!mediaStorageDir.exists()) 
	    {
	        if (!mediaStorageDir.mkdirs()) 
	        {
	            Log.e(LOG_NAME, "Oops! Failed to create " + imageDirectoryName + " directory");
	            return null;
	        }
	    }
	 
	    // Create a media file name
	    File mediaFile = new File(mediaStorageDir.getPath() + File.separator + filename);
	 
	    return mediaFile;
	}

	/**
	 * Gets a Subdirectory from the Pictures Folder with the Specified Name
	 * @param imageDirectoryName
	 * @return
	 */
	public static File getPictureDirectory(String imageDirectoryName)
	{
		File   storageDir = null;
		String dirPath    = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), imageDirectoryName).getAbsolutePath() + "/";
	      
	      if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) 
	      {
	            storageDir = new File(dirPath);
	            if (storageDir != null) 
	            {
	                  if (!storageDir.mkdirs()) 
	                  {
	                        if (!storageDir.exists())
	                        {
	                              Log.e(LOG_NAME, "failed to create directory: " + dirPath);
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

	/**
	 * Resizes the Specified Image
	 * @param originalFile
	 * @param newWidth
	 * @param newHeight
	 * @return
	 */
	public static Bitmap resizeImage(ContentResolver cr, File originalFile, int newWidth, int newHeight)
	{
		try
		{
	    	Bitmap originalBitmap = android.provider.MediaStore.Images.Media.getBitmap(cr, ImageToolkit.getOutputMediaFileUri(originalFile));
	    	return resizeImage(originalBitmap, newWidth, newHeight);
		}
		catch (Exception ex)
		{
			Log.e(LOG_NAME, "Problem Resizing Image: " + ex.getMessage());
		}
		
		return null;
	}

	/**
	 * Resizes the Specified Bitmap
	 * @param originalBitmap
	 * @param newWidth
	 * @param newHeight
	 * @return
	 */
	public static Bitmap resizeImage(Bitmap originalBitmap, int newWidth, int newHeight)
	{
		try
		{
	    	Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true);
	    	
	    	return resizedBitmap;
		}
		catch (Exception ex)
		{
			Log.e(LOG_NAME, "Problem Resizing Image: " + ex.getMessage());
		}
		
		return null;
	}
	
	/**
	 * Writes the Bitmap to a File
	 * @param bitmap
	 */
	public static File writeBitmapToJPEG(Bitmap bitmap, String imageDirectoryName, String filename)
	{				
    	try
    	{
    		ByteArrayOutputStream stream = new ByteArrayOutputStream();
    		bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);	
    		
    		File 			 dir  = ImageToolkit.getPictureDirectory(imageDirectoryName);
    		File 			 file = new File(dir.getAbsolutePath() + "/" + filename);
    	    FileOutputStream fos  = new FileOutputStream(file);
    	    fos.write(stream.toByteArray());
    	    fos.close();	
    	    
    	    return file;
    	}
    	catch (Exception ex)
    	{
    		Log.e(LOG_NAME, "Problem Writing Bitmap to JPEG: " + ex.getMessage());
    	}
    	
    	return null;
	}
}
