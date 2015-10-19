package com.adefreitas.gcf.android.impromptu;

import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.impromptu.ApplicationProvider;

public abstract class AndroidApplicationProvider extends ApplicationProvider
{	
	/**
	 * Constructor
	 * @param application
	 * @param groupContextManager
	 * @param contextType
	 * @param name
	 * @param description
	 * @param contextsRequired
	 * @param preferencesToRequest
	 * @param logoPath
	 * @param commMode
	 * @param ipAddress
	 * @param port
	 */
	public AndroidApplicationProvider(
			GroupContextManager groupContextManager,
			String contextType, 
			String name, 
			String description,
			String category,
			String[] contextsRequired, 
			String[] preferencesToRequest,
			String logoPath, 
			int lifetime,
			CommMode commMode, 
			String ipAddress, 
			int port)
	{
		super(groupContextManager, contextType, name, description, category, contextsRequired, preferencesToRequest, logoPath, lifetime, commMode, ipAddress, port);
	}

	public AndroidApplicationProvider(
			GroupContextManager groupContextManager,
			String contextType, 
			String name, 
			String description,
			String category,
			String[] contextsRequired, 
			String[] preferencesToRequest,
			String logoPath, 
			int lifetime,
			CommMode commMode, 
			String ipAddress, 
			int port,
			String channel)
	{
		super(groupContextManager, contextType, name, description, category, contextsRequired, preferencesToRequest, logoPath, lifetime, commMode, ipAddress, port, channel);
	}
	
	@Override
	public void onSubscription(ContextSubscriptionInfo newSubscription)
	{
		super.onSubscription(newSubscription);
		
		// Sends the UI Immediately
		//sendMostRecentReading();
		
		// Determines Credentials
//		String username = CommMessage.getValue(newSubscription.getParameters(), "credentials");
//		Toast.makeText(application, "Subscription: " + username, Toast.LENGTH_SHORT);
	}
	
	@Override
	public void onSubscriptionCancelation(ContextSubscriptionInfo subscription)
	{
		// Determines Credentials
		//String username = CommMessage.getValue(subscription.getParameters(), "credentials");
		//Toast.makeText(application, "Unsubscription: " + username, Toast.LENGTH_SHORT);
	}
}
