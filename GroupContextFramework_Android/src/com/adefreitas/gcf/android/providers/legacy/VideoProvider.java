package com.adefreitas.gcf.android.providers.legacy;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import android.content.Context;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.adefreitas.gcf.ContextProvider;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.messages.CommMessage;

/**
 * This is a template for a context provider.
 * COPY AND PASTE; NEVER USE
 * @author adefreit
 */
public class VideoProvider extends ContextProvider
{
	// Context Configuration
	private static final String FRIENDLY_NAME = "VIDEO RECORDER";	
	private static final String CONTEXT_TYPE  = "VID";
	private static final String LOG_NAME      = "GCF-ContextProvider [" + CONTEXT_TYPE + "]";
	
	// Internal Logic
	private boolean       recording;
	private String        filename;
	private Context       context;
	private SurfaceView   surfaceView;
	private SurfaceHolder holder;
	private boolean 	  mInitSuccesful;
	
	// Camera Components
	private Camera 		  		camera;
	private MediaRecorder 		mediaRecorder;
	
	public VideoProvider(Context context, SurfaceView frame, GroupContextManager groupContextManager) 
	{
		super(CONTEXT_TYPE, groupContextManager);
		
		// Initializes Flag
		this.context 	 = context;
		recording    	 = false;
		filename     	 = "";
		camera 			 = Camera.open();
		this.surfaceView = frame;
		this.holder  	 = surfaceView.getHolder();
	}

	@Override
	public void start() 
	{
		this.getGroupContextManager().log(LOG_NAME, this.getContextType() + " Provider Started");
		
		if (prepareForVideoRecording()) 
		{
            mediaRecorder.start();
            recording = true;
        } 
		else 
		{
            // Something has gone wrong! Release the camera
            releaseMediaRecorder();
            Toast.makeText(context, "Sorry: couldn't start video", Toast.LENGTH_LONG).show();
        }
	}

	@Override
	public void stop() 
	{
		this.getGroupContextManager().log(LOG_NAME, this.getContextType() + " Provider Stopped");
		
		if (mediaRecorder != null)
		{
			mediaRecorder.stop();
	        releaseMediaRecorder();
	        camera.lock();
		}
		
        recording = false;
	}
	
	@Override
	public double getFitness(String[] parameters) 
	{
		return 1.0;
	}

	@Override
	public void sendContext() 
	{
		this.getGroupContextManager().sendContext(this.getContextType(), new String[0], new String[] { "recording=" + recording, "filename=" + filename, "battery=" + this.getGroupContextManager().getBatteryMonitor().getBatteryPercent() });
	}
	
	protected boolean prepareForVideoRecording() 
	{
		  camera.unlock();
		  mediaRecorder = new MediaRecorder();
		  mediaRecorder.setCamera(camera);
		  mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		  mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		  mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_LOW));
		  mediaRecorder.setOutputFile(getOutputFile());
		  
		  mediaRecorder.setMaxDuration(60000 * 60); // Set max duration 60 sec.
	      mediaRecorder.setMaxFileSize(1000000000); // Set max file size 5M
		  
		  mediaRecorder.setPreviewDisplay(surfaceView.getHolder().getSurface());
		  
		  try 
		  {
			  mediaRecorder.prepare();
		  } 
		  catch (IllegalStateException e) 
		  {
			  Log.e(LOG_NAME, "IllegalStateException when preparing MediaRecorder " + e.getMessage());
			  e.getStackTrace();
			  releaseMediaRecorder();
			  return false;
		  } 
		  catch (IOException e) 
		  {
			    Log.e(LOG_NAME, "IOException when preparing MediaRecorder " + e.getMessage());
			    e.getStackTrace();
				releaseMediaRecorder();
				return false;
		  }
		  
		  return true;
		}
	
	private void releaseMediaRecorder() 
	{
		  if (mediaRecorder != null) 
		  {
			  mediaRecorder.reset();
			  mediaRecorder.release();
			  mediaRecorder = null;
		      camera.lock();
		  }
		}
	
	private String getOutputFile()
	{
		// Sets a Unique Filename
	    if (this.getSubscriptionParameters().length > 0)
	    {
	    	System.out.println("Received Parameters");
	    	String requestedFilename = CommMessage.getValue(this.getSubscriptionParameters(), "filename");
	    	filename = (requestedFilename != null) ? this.getGroupContextManager().getDeviceID() + "-" + requestedFilename : new Date().getTime()  +  "";
	    }
	    else
	    {
	    	System.out.println("No Filename Parameters");
	    	filename = new Date().getTime()  +  ".mp4";
	    }
	    
	    // Looks for an Unused Filename
	    int  sequence = 1;
	    File tmp 	  = new File(Environment.getExternalStorageDirectory().getPath() + "/Download/" + filename + "-" + sequence + ".mp4");
	    
	    while (tmp.exists())
	    {
	    	sequence++;
	    	tmp = new File(Environment.getExternalStorageDirectory().getPath() + "/Download/" + filename + "-" + sequence + ".mp4");
	    }
	    
	    // Sets the Output File
	    filename = filename + "-" + sequence + ".mp4";
	    
	    System.out.println("Writing to " + tmp.getAbsolutePath());
	    return tmp.getAbsolutePath();
	}
	
	private void initRecorder(Surface surface) throws IOException 
	{
		System.out.println("Initializing Recorder");
		
	    // It is very important to unlock the camera before doing setCamera
	    // or it will results in a black preview
	    if (camera == null) 
	    {
	        camera = Camera.open();
	        camera.unlock();
	    }

	    if (mediaRecorder == null)
	    {
	    	mediaRecorder = new MediaRecorder();
	    }
	        
	    mediaRecorder.setCamera(camera);
	    
	    mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
	    mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
	    mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_LOW));
	    
	    
	    mediaRecorder.setOutputFile(Environment.getExternalStorageDirectory().getPath() + "/Download/" + filename);

	    mediaRecorder.setPreviewDisplay(holder.getSurface());
	    
	    try 
	    {
	        mediaRecorder.prepare();
	        Thread.sleep(1000);
	    } 
	    catch (Exception e) 
	    {
	        // This is thrown if the previous calls are not called with the
	        // proper order
	        e.printStackTrace();
	    }

	    mInitSuccesful = true;
	}

}
