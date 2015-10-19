package com.adefreitas.gcf.android.providers;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

/**
* Service that receives ActivityRecognition updates. It receives updates
* in the background, even if the main Activity is not visible.
*/
public class ActivityRecognitionIntentService extends IntentService 
{  
	public static String ACTION_ACTIVITY_UPDATE = "ACTION_ACTIVITY_UPDATE";
	public static String EXTRA_ACTIVITY			= "ACTIVITY";
	public static String EXTRA_CONFIDENCE		= "CONFIDENCE";
	
	/**
	 * Constructor
	 */
 	public ActivityRecognitionIntentService() 
	{
		super("ActivityRecognitionService");
	}

 	 /**
 	   * Called when a new activity detection update is available.
 	   */
	@Override
	protected void onHandleIntent(Intent intent) 
	{	
	  // If the intent contains an update
      if (ActivityRecognitionResult.hasResult(intent)) 
      {
          // Get the update
          ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);

          DetectedActivity mostProbableActivity = result.getMostProbableActivity();

           // Get the confidence % (probability)
          int confidence = mostProbableActivity.getConfidence();

          // Get the type 
          int activityType = mostProbableActivity.getType();
          
          Intent i = new Intent(ACTION_ACTIVITY_UPDATE);
          i.putExtra(EXTRA_ACTIVITY, getActivityName(activityType));
          i.putExtra(EXTRA_CONFIDENCE, confidence);
          this.sendBroadcast(i);
      }
	}
	
	/**
	 * Converts an Activity Type to an Activity Name
	 * @param activityType
	 * @return
	 */
	private String getActivityName(int activityType)
	{
		switch(activityType)
		{
			case DetectedActivity.STILL:
				return "still";
			case DetectedActivity.ON_FOOT:
				return "on_foot";
			case DetectedActivity.WALKING:
				return "walking";
			case DetectedActivity.RUNNING:
				return "running";
			case DetectedActivity.ON_BICYCLE:
				return "on_bicycle";
			case DetectedActivity.IN_VEHICLE:
				return "in_vehicle";
			case DetectedActivity.TILTING:
				return "tilting";
			case DetectedActivity.UNKNOWN:
				return "unknown";
		}
		
		return "unknown";
	}
}