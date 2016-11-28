package it.polimi.greenhouse.a3.roles;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import it.polimi.deepse.a3droid.a3.A3Message;
import it.polimi.deepse.a3droid.a3.A3FollowerRole;
import it.polimi.deepse.a3droid.a3.exceptions.A3ChannelNotFoundException;
import it.polimi.deepse.a3droid.a3.exceptions.A3SupervisorNotElectedException;
import it.polimi.greenhouse.util.AppConstants;


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
		try {
			node.sendToSupervisor(new A3Message(AppConstants.NEW_PHONE, node.getUID()), "control");
		} catch (A3SupervisorNotElectedException e) {
			e.printStackTrace();
		}
		postUIEvent(0, "[CtrlFolRole]");
	}

	@Override
	public void receiveApplicationMessage(A3Message message) {
		
		switch(message.reason){
		
		case AppConstants.ADD_MEMBER:			
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

			sendToConnectedSupervisors(message);
			break;
			
		case AppConstants.START_EXPERIMENT:
			postUIEvent(0, "wrong place to start experiment control follower role");

		case AppConstants.LONG_RTT:		
		case AppConstants.SET_PARAMS:

			sendToConnectedSupervisors(message);
			break;
		
		case AppConstants.STOP_EXPERIMENT:

			//showOnScreen("--- STOP_EXPERIMENT: ATTENDERE 10s CIRCA ---");
			
			for(String gType : launchedGroups.keySet())
				for(int i : launchedGroups.get(gType))
					if(node.isConnected(gType + "_" + i) && !node.isSupervisor(gType + "_" + i))
						try {
							node.disconnect(gType + "_" + i);
						} catch (A3ChannelNotFoundException e) {
							e.printStackTrace();
						}

			synchronized(this){
				try {
					wait(10000);
				} catch (InterruptedException e) {}
			}

			//showOnScreen("--- DISCONNESSIONE SUPERVISORI IN CORSO ---");
			
			for(String gType : launchedGroups.keySet())
				for(int i : launchedGroups.get(gType))
					if(node.isConnected(gType + "_" + i))
						try {
							node.disconnect(gType + "_" + i);
						} catch (A3ChannelNotFoundException e) {
							e.printStackTrace();
						}

			launchedGroups.clear();
			//showOnScreen("--- ESPERIMENTO TERMINATO ---");
			break;
			
		default:
			break;
		}
	}

	private void sendToConnectedSupervisors(A3Message message) {
		for(String gType : launchedGroups.keySet())
            for(int i : launchedGroups.get(gType))
                if(node.isConnected(gType + "_" + i) && node.isSupervisor(gType + "_" + i))
					try {
						node.sendToSupervisor(message,
                            gType + "_" + i);
					} catch (A3SupervisorNotElectedException e) {
						e.printStackTrace();
					}
	}
}
