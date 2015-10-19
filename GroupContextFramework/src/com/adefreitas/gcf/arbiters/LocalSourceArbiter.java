package com.adefreitas.gcf.arbiters;

import java.util.ArrayList;
import java.util.Locale;

import com.adefreitas.gcf.Arbiter;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.messages.ContextCapability;
import com.adefreitas.gcf.messages.ContextRequest;

public class LocalSourceArbiter extends Arbiter
{

	public LocalSourceArbiter(int requestType) 
	{
		super(requestType);
	}

	@Override
	public ArrayList<ContextCapability> selectCapability(
			ContextRequest request, 
			GroupContextManager gcm, 
			ArrayList<ContextCapability> subscribedCapabilities, 
			ArrayList<ContextCapability> receivedCapabilities) 
	{
		ArrayList<ContextCapability> result = new ArrayList<ContextCapability>();
		
		ContextCapability 			 currentCapability    = (subscribedCapabilities.size() == 1) ? subscribedCapabilities.get(0) : null;
		ContextCapability 			 bestCapability 	  = currentCapability;
		ArrayList<ContextCapability> candidates 		  = new ArrayList<ContextCapability>();
		boolean 		  			 foundCurrent		  = false;
		double 			  			 bestWeightedFitness  = -100000.0;
		double 			  			 maxBattery  		  = -100000.0;
		double 						 maxHeartbeat 		  = -100000.0;
					
		// Determines the Ceiling Values
		// This is needed for the Weighted Capability Calculations
		for (ContextCapability capability : receivedCapabilities)
		{		
			if (capability.getDeviceID().equals(gcm.getDeviceID()) && capability.getContextType().equals(request.getContextType()))
			{
				// Finds the Max Battery 
				maxBattery = (capability.getBatteryLife() > maxBattery) ? capability.getBatteryLife() : maxBattery;
				
				// Finds the Max Heartbeat
				maxHeartbeat = (capability.getHeartbeatRate() > maxHeartbeat) ? capability.getHeartbeatRate() : maxHeartbeat;
				
				// Also Determines if the Currently Subscribed Capability has Sent an Updated Advertisement
				foundCurrent = (currentCapability != null && currentCapability.getDeviceID().equals(capability.getDeviceID())) ? true : foundCurrent;
				
				// Adds to the List of Candidates
				candidates.add(capability);
			}
		}
		
		// Ensures that the Currently Subscribed Capability is Considered when Looking for the Best Provider
		if (currentCapability != null && !foundCurrent)
		{
			// Finds the Max Battery 
			maxBattery = (currentCapability.getBatteryLife() > maxBattery) ? currentCapability.getBatteryLife() : maxBattery;
			
			// Finds the Max Heartbeat
			maxHeartbeat = (currentCapability.getHeartbeatRate() > maxHeartbeat) ? currentCapability.getHeartbeatRate() : maxHeartbeat;
			
			// Adds the Current Capability to the List
			candidates.add(currentCapability);
		}
		
		// Evaluates the "Fitness" of Each Advertised Capability
		for (ContextCapability capability : candidates)
		{		
			// Extracts Each of the Decision Factors from the Capability Advertisement
			double     battery          = capability.getBatteryLife() / maxBattery;
			double     sensorFitness    = capability.getSensorFitness();
			double 	   commFrequency    = capability.getHeartbeatRate() / maxHeartbeat;
			boolean    alreadyProviding = capability.isAlreadyProviding();
			double     reliability 	    = gcm.getContextReliabilityInfo(capability.getDeviceID()).getReliabilityRatio(capability.getContextType());
			
			// Performs the weighted calculation
			double weightedBattery     = request.getBatteryWeight() * battery;
			double weightedSensor      = request.getSensorFitnessWeight() * sensorFitness;
			//double weightedForeign     = (isSelf) ? 0.0 : request.getForeignWeight();
			double weightedComm		   = request.getForeignWeight() * commFrequency;
			double weightedProviding   = (alreadyProviding) ? request.getProvidingWeight() : 0.0;
			double weightedReliability = request.getReliabilityWeight() * reliability;
			double weightedFitness 	   = weightedBattery + weightedSensor + weightedComm + weightedProviding + weightedReliability;
				
//			gcm.log("GCM-Comparison", String.format(Locale.getDefault(), "%s (%s) - Bat=%1.2f (%1.2f) Fitness=%1.2f (%1.2f) Providing=%s (%1.2f) Self=%s (%1.2f) Reliability=%1.2f (%1.2f)  Total=%1.3f", 
//					capability.getContextType(), capability.getDeviceID(),
//					battery, weightedBattery, sensorFitness, weightedSensor, 
//					alreadyProviding, weightedProviding, isSelf, weightedForeign, reliability, weightedReliability, weightedFitness));
			
			gcm.log("GCM-Comparison", String.format(Locale.getDefault(), "%s (%s) - Bat=%1.2f (%1.2f) Fitness=%1.2f (%1.2f) Providing=%s (%1.2f) Comm=%1.2f (%1.2f) Reliability=%1.2f (%1.2f)  Total=%1.3f", 
					capability.getContextType(), capability.getDeviceID(),
					battery, weightedBattery, sensorFitness, weightedSensor, 
					alreadyProviding, weightedProviding, commFrequency, weightedComm, reliability, weightedReliability, weightedFitness));			
			
			// Remembers the Best Weighted Fitness Rating
			if (weightedFitness > bestWeightedFitness)
			{
				//gcm.log("GCM-Comparison", "  New winner [" + capability.getContextType() + "]: " + weightedFitness + " vs " + bestWeightedFitness);
				bestCapability 	    = capability;
				bestWeightedFitness = weightedFitness;
			}
//			else
//			{
//				gcm.log("GCM-Comparison", "  Ignoring [" + capability.getContextType() + "]: " + weightedFitness + " vs " + bestWeightedFitness);
//			}
		}
		
		// Adds the Best Capability to the Array
		result.add(bestCapability);
		
		return result;
	}

}
