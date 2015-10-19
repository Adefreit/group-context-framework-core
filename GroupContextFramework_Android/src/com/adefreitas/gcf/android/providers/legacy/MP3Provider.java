package com.adefreitas.gcf.android.providers.legacy;

import java.io.File;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.util.Log;
import android.widget.Toast;

import com.adefreitas.gcf.ContextProvider;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.android.toolkit.CloudStorageToolkit;
import com.adefreitas.gcf.android.toolkit.DropboxToolkit;
import com.adefreitas.gcf.messages.CommMessage;
import com.adefreitas.gcf.messages.ComputeInstruction;

/**
 * Allows for Remote Access to a Phone's Speakers
 * Command:  PLAY
 * Parameters:  dropboxPath = /folder to file
 *              dropboxFile = filename
 *              
 * Command:  STOP
 * @author adefreitas
 */
public class MP3Provider extends ContextProvider
{
	private static final String CONTEXT_TYPE = "MP3";
	private static final String LOG_NAME     = "GCF-ContextProvider [" + CONTEXT_TYPE + "]";
	
	private ContextWrapper cw;
	private MediaPlayer    mp;
	private String         currentMP3;
	
	// Dropbox Variables
	private CloudStorageToolkit	  cloudToolkit;
	private DropboxIntentReceiver dbReceiver;
	private IntentFilter 		  filter;
	
	public MP3Provider(ContextWrapper cw, CloudStorageToolkit cloudToolkit, GroupContextManager groupContextManager) 
	{
		super(CONTEXT_TYPE, groupContextManager);
		mp = new MediaPlayer();
		
		this.cw 			= cw;
		this.cloudToolkit = cloudToolkit;
		dbReceiver 		    = new DropboxIntentReceiver(cw);
		filter     		    = new IntentFilter();
		
		filter.addAction(DropboxToolkit.ACTION_CLOUD_DOWNLOAD_COMPLETE);
		filter.addAction(DropboxToolkit.ACTION_CLOUD_UPLOAD_COMPLETE);
		cw.registerReceiver(dbReceiver, filter);
	}
	
	@Override
	public void start() 
	{
		
	}

	@Override
	public void stop() 
	{
		
	}

	@Override
	public double getFitness(String[] parameters) 
	{
		return 1.0;
	}

	@Override
	public void sendContext() 
	{
		if (!mp.isPlaying())
		{
			currentMP3 = "";
		}
		
		this.getGroupContextManager().sendContext(this.getContextType(), new String[0], new String[] { currentMP3 });
	}

	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{		
		System.out.println("COMMAND: " + instruction);
		
		try
		{
			if (cloudToolkit != null)
			{
				if (instruction.getCommand().equalsIgnoreCase("PLAY"))
				{
					System.out.println("a");
					
					String filePath = instruction.getPayload("filePath");
					
					if (filePath != null)
					{
						System.out.println("File Path = " + filePath);
						
						// Downloads the File
						File albumF = cloudToolkit.getDownloadDirectory();
					    File file   = new File(albumF.getAbsolutePath() + "/" + filePath.substring(filePath.lastIndexOf("/")+1));
					    
					    System.out.println("Looking for file: " + file.getAbsolutePath());
					    
					    if (!file.exists())
					    {
					    	System.out.println("Not Found!  Downloading. ");
					    	cloudToolkit.downloadFile(filePath);	
					    }
					    else
					    {
					    	System.out.println("File already downloaded.");
					    	playFile(file.getAbsolutePath());
					    }
					}
					else
					{
						System.out.println("File Path Not Provided");
					}
				}
				else if (instruction.getCommand().equalsIgnoreCase("STOP"))
				{
					//mp.stopBluetoothScan();
					mp.reset();
				}
			}
			else
			{
				System.out.println("Ummm . . .");
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	private void playFile(String absolutePath)
	{
		System.out.println("PLAYING " + absolutePath);
		
		if (!currentMP3.equals(absolutePath))
		{
			//mp.stopBluetoothScan();
			mp.reset();	
		
			try
			{
				mp.setDataSource(absolutePath);
				mp.prepare();
				mp.start();
				currentMP3 = absolutePath;
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
	}
	
	/**
	 * Handles Messages Passed from Dropbox
	 * @author adefreit
	 */
	class DropboxIntentReceiver extends BroadcastReceiver
	{
		private ContextWrapper context;
		
		public DropboxIntentReceiver(ContextWrapper cw)
		{
			context = cw;
		}
		
		@Override
		public void onReceive(Context context, Intent intent) 
		{	
			if (intent.getAction().equals(DropboxToolkit.ACTION_CLOUD_DOWNLOAD_COMPLETE))
			{
				String downloadPath = intent.getStringExtra(DropboxToolkit.EXTRA_CLOUD_DOWNLOAD_PATH);
				
				System.out.println(downloadPath);
				System.out.println(this.context);
				
				Toast.makeText(this.context, "Downloaded " + downloadPath, Toast.LENGTH_SHORT).show();
				
				if (downloadPath != null)
				{
					String[] args     = downloadPath.split("/");
					String   filename = (args.length > 0) ? args[args.length-1] : "";
			        playFile(downloadPath);
				}
				else
				{
					System.out.println("No download path found.");
				}
			}
			else if (intent.getAction().equals(DropboxToolkit.ACTION_CLOUD_UPLOAD_COMPLETE))
			{
				System.out.println("UPLOAD COMPLETE!");
			}
			else
			{
				Log.e("", "Unknown Action: " + intent.getAction());
			}
		}
	}
}
