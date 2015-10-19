package com.adefreitas.gcf.android;

import com.adefreitas.gcf.android.bluewave.JSONContextParser;
import com.adefreitas.gcf.messages.ContextData;

/**
 * Interface so that Activities can Get Context Without Having to Listen to Intents (the Application does this for them) 
 * @author adefreit
 */
public interface ContextReceiver
{
	public void onContextData(ContextData data);
	
	public void onBluewaveContext(JSONContextParser parser);
}
