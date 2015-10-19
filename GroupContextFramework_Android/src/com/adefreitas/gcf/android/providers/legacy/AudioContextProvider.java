package com.adefreitas.gcf.android.providers.legacy;

import java.io.File;

import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

import com.adefreitas.gcf.ContextProvider;
import com.adefreitas.gcf.GroupContextManager;

public class AudioContextProvider extends ContextProvider
{
	// Media Recorder Variables
	private MediaRecorder recorder;
	
	// File I/O
	private File directory;
	private File audiofile;
		
	/**
	 * Constructor
	 * @param groupContextManager
	 */
	public AudioContextProvider(GroupContextManager groupContextManager) 
	{
		super("AUD", groupContextManager);
		
		try
		{
			directory = Environment.getExternalStorageDirectory();
			audiofile = null;
		}
	    catch (Exception ex)
	    {
	    	Log.e("GCF-ContextProvider", ex.toString());
	    }
	}

	@Override
	public void start() 
	{
		if (prepareAudioFile())
		{
			try
			{
				recorder.prepare();
				recorder.start();
				
				// Turns on the Reporting Thread
				//t = new ContextReportingThread(this);
				//t.start();
				
				Log.d("GCM-ContextProvider", "Audio Recording Started");
			}
			catch (Exception ex)
			{
				Log.e("GCF-ContextProvider", ex.toString());
			}	
		}
	}

	@Override
	public void stop() 
	{	
		// Stops the Recorder
		recorder.stop();
		recorder.reset();
		recorder.release();

		// Deletes the Temporary Audio File
		audiofile.delete();
		
		Log.d("GCM-ContextProvider", "Audio Recording Stopped");
		
//		// Halts the Reporting Thread
//		if (t != null)
//		{	
//			// Halts the Reporting Thread
//			//t.halt();
//			//t = null;	
//			
//			
//		}
//		else
//		{
//			Log.d("GCM-ContextProvider", "Audio Recording Cannot Stop Because it Never Started");
//		}
		
	}

	@Override
	public double getFitness(String[] parameters) 
	{
		return 1.0;
	}

	@Override
	public void sendContext() 
	{
		this.getGroupContextManager().sendContext(getContextType(), new String[0], new String[] { Integer.toString(recorder.getMaxAmplitude()) });
	}
	
	private boolean prepareAudioFile()
	{
		try 
	    {
	      audiofile = File.createTempFile("sound", ".3gp", directory);
	    } 
	    catch (Exception e) 
	    {
	      Log.e("GCF-ContextProvider", "sdcard access error " + e.toString());
	      return false;
	    }
		
	    if (audiofile != null)
	    {
			recorder = new MediaRecorder();
			recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
			recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		    recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
		    recorder.setOutputFile(audiofile.getAbsolutePath());	
			Log.d("GCF-ContextProvider", "Starting Recording");
			
			return true;
	    }
	    else
	    {
	    	return false;
	    }
	}
}
