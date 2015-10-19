package com.adefreitas.gcf.android.providers;

import java.util.Date;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.adefreitas.gcf.ContextProvider;
import com.adefreitas.gcf.GroupContextManager;
import com.google.gson.Gson;

/**
 * Audio Amplitude Context Provider [AUD]
 * Accesses the Device's Microphone
 * Implementation Based On Code Provided by: http://developer.samsung.com/technical-doc/view.do;jsessionid=ynxSVrmLdC1hyKrcQRLvqHTM2CKwyJYpLztddyyKY7gcWstKXd6c!1065508478?v=T000000086
 * Author: Adrian de Freitas
 */
public class AudioContextProvider extends ContextProvider
{
	// GCF Context Configuration
	private static final String CONTEXT_TYPE = "AUD";
	private static final String LOG_NAME     = "GCF-ContextProvider [" + CONTEXT_TYPE + "]";
	private static final String DESCRIPTION  = "Shares audio amplitude data from your device's smartphone.  No conversation is data is recorded.  This process consumes additional power.";
	
	// Constants
	private static final int SAMPLE_RATE 			= 8000;
	private static final int BUFFER_SIZE 			= 100;
	private static final int TIME_BETWEEN_THRESHOLD = 1000;
	
	// Context Provider Attributes
	private Gson		gson;
	private AudioRecord recorder;	
	private short[] 	buffer;
	private boolean     recording;
	private int[]		amplitudes;
	private int 	    amplitude_index = 0;	
	
	// Behavior Flags
	private int  threshold;
	private Date lastThresholdNotification;
	
	/**
	 * Constructor
	 * @param groupContextManager - A link to the application's group context manager
	 */
	public AudioContextProvider(GroupContextManager groupContextManager) 
	{
		super(CONTEXT_TYPE, DESCRIPTION, groupContextManager);
		
		// Initializes Values
		gson       = new Gson();
		recording  = false;
		amplitudes = new int[BUFFER_SIZE];
		threshold  = Integer.MAX_VALUE;
		lastThresholdNotification = new Date(0);
	}

	@Override
	public void start() 
	{
		if (!recording)
		{
			// Sets the Flag
			recording = true;	
			
			// Initializes the Recorder
			initRecorder();
			
			startBufferedWrite();
		}
		
		this.getGroupContextManager().log(LOG_NAME, this.getContextType() + " Provider Started");
	}

	@Override
	public void stop() 
	{
		if (recording && recorder != null)
		{
			// Sets the Flag
			recording = false;
			
			// Releases Resources
			recorder.stop();
			recorder.release();
		}
				
		this.getGroupContextManager().log(LOG_NAME, this.getContextType() + " Provider Stopped");
	}

	@Override
	public double getFitness(String[] parameters) 
	{
		return SAMPLE_RATE;
	}

	@Override
	public void sendContext() 
	{	
		int[] data = null;
		
		System.out.println("Audio Threshold: " + threshold);
		
		if (this.getSubscriptionParameter("THRESHOLD") == null)
		{
			// Determines Whether to Send the WHOLE Amplitudes Array, or just a Part
			if (amplitude_index == amplitudes.length-1)
			{
				data = amplitudes;
			}
			else
			{
				data = new int[amplitude_index+1];
				System.arraycopy(amplitudes, 0, data, 0, amplitude_index+1);
			}
			
			// Sends the Data as a JSON Array
			this.sendContext(this.getSubscriptionDeviceIDs(), new String[] { "DATA=" + gson.toJson(data) });
		}
		else
		{
			if (System.currentTimeMillis() - this.lastThresholdNotification.getTime() > TIME_BETWEEN_THRESHOLD)
			{
				int maxValue = Integer.MIN_VALUE;
				
				for (int i=0; i<amplitude_index; i++)
				{
					maxValue = Math.max(maxValue, amplitudes[amplitude_index]);
				}
				
				// Only Sends a Message Stating that the Value was Encountered
				if (maxValue > threshold)
				{
					this.sendContext(this.getSubscriptionDeviceIDs(), new String[] { "THRESHOLD=" + threshold, "VALUE=" + maxValue });
					lastThresholdNotification = new Date();	
				}
			}
		}
		
		// Resets Audio Data
		amplitudes      = new int[BUFFER_SIZE];
		amplitude_index = 0;
	}
	
	public void updateConfiguration()
	{
		super.updateConfiguration();
		
		String thresholdParameter = this.getSubscriptionParameter("THRESHOLD");
		
		if (thresholdParameter != null)
		{
			this.threshold = Integer.parseInt(thresholdParameter);
		}
		else
		{
			this.threshold = Integer.MAX_VALUE;
		}
	}
	
	// PRIVATE METHODS -------------------------------------------------------------------------------
	private void initRecorder() 
	{
		int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
		buffer         = new short[bufferSize];
		recorder       = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
	}

	private void startBufferedWrite() 
	{		
		new Thread(new Runnable() 
		{		
			@Override
			public void run() 
			{
				Log.d(LOG_NAME, "Audio Context Provider Thread Started!");
				
				recorder.startRecording();
				
				try {
					while (recording) 
					{
						double sum = 0;
						int readSize = recorder.read(buffer, 0, buffer.length);
						for (int i = 0; i < readSize; i++) 
						{
							sum += buffer[i] * buffer[i];
						}
						if (readSize > 0) 
						{
							// Stores the Amplitude in Memoty
							amplitudes[amplitude_index] = (int)(sum / readSize);
							
							// Transmits the Data if/when Buffer is Full
							if (amplitude_index == amplitudes.length - 1)
							{
								sendContext();
							}
							else if (amplitudes[amplitude_index] > threshold)
							{
								sendContext();
							}
																					
							// Increments the Counter
							amplitude_index = (amplitude_index + 1) % amplitudes.length;
						}
					}
				} 
				catch (Exception e) 
				{
					Log.e(LOG_NAME, e.getMessage());
				}
				
				Log.d(LOG_NAME, "Audio Context Provider Thread Complete!");
			}
		}).start();
	}

}
