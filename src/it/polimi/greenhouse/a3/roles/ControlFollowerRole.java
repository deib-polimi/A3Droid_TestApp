package it.polimi.greenhouse.a3.roles;

import it.polimi.greenhouse.activities.MainActivity;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import a3.a3droid.A3FollowerRole;
import a3.a3droid.A3Message;

/**
 * This class is the role the followers of "control" group play.
 * @author Francesco
 *
 */
public class ControlFollowerRole extends A3FollowerRole {

	private Map<String, Set<Integer>> launchedGroups;
	
	public ControlFollowerRole() {
		super();
	}

	@Override
	public void onActivation() {
		launchedGroups = new ConcurrentHashMap<String, Set<Integer>>();
	}

	@Override
	public void logic() {
		showOnScreen("[CtrlFolRole]");
		node.sendToSupervisor(new A3Message(MainActivity.NEW_PHONE, node.getUUID()), "control");
		active = false;
	}

	@Override
	public void receiveApplicationMessage(A3Message message) {
		
		switch(message.reason){
		
		case MainActivity.ADD_MEMBER:			
			String content [] = ((String)message.object).split("_");
			String type = content[0];
			int experimentId = Integer.valueOf(content[1]);
			if(launchedGroups.containsKey(type)){
				if(!launchedGroups.get(type).contains(experimentId))
					launchedGroups.get(type).add(experimentId);
			}else{
				Set <Integer> experiments = Collections.synchronizedSet(new HashSet<Integer>());
				experiments.add(experimentId);
				launchedGroups.put(type, experiments);
			}
			
			for(String gType : launchedGroups.keySet())
				for(int i : launchedGroups.get(gType))
					if(node.isConnectedForApplication(gType + "_" + i) && node.isSupervisor(gType + "_" + i))
						node.sendToSupervisor(message,
							gType + "_" + i);
			break;
			
		case MainActivity.START_EXPERIMENT:
		case MainActivity.LONG_RTT:		
		case MainActivity.SET_PARAMS:
			
			for(String gType : launchedGroups.keySet())
				for(int i : launchedGroups.get(gType))
					if(node.isConnectedForApplication(gType + "_" + i) && node.isSupervisor(gType + "_" + i))
						node.sendToSupervisor(message,
							gType + "_" + i);
			break;
		
		case MainActivity.STOP_EXPERIMENT:

			showOnScreen("--- STOP_EXPERIMENT: ATTENDERE 10s CIRCA ---");
			
			for(String gType : launchedGroups.keySet())
				for(int i : launchedGroups.get(gType))
					if(node.isConnectedForApplication(gType + "_" + i) && !node.isSupervisor(gType + "_" + i))
						node.disconnect(gType + "_" + i, true);
						
			synchronized(this){
				try {
					wait(10000);
				} catch (InterruptedException e) {}
			}

			showOnScreen("--- DISCONNESSIONE SUPERVISORI IN CORSO ---");
			
			for(String gType : launchedGroups.keySet())
				for(int i : launchedGroups.get(gType))
					if(node.isConnectedForApplication(gType + "_" + i))
							node.disconnect(gType + "_" + i, true);
			
			launchedGroups.clear();
			showOnScreen("--- ESPERIMENTO TERMINATO ---");
			break;
		}
	}
}
