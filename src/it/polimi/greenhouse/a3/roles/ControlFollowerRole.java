package it.polimi.greenhouse.a3.roles;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.polimi.greenhouse.activities.MainActivity;
import a3.a3droid.A3FollowerRole;
import a3.a3droid.A3Message;
import android.os.Environment;

/**
 * This class is the role the followers of "control" group play.
 * @author Francesco
 *
 */
public class ControlFollowerRole extends A3FollowerRole {

	private int runningExperiment;
	private Map<String, List<Integer>> launchedGroups;
	
	public ControlFollowerRole() {
		// TODO Auto-generated constructor stub
		super();
	}

	@Override
	public void onActivation() {
		// TODO Auto-generated method stub
		runningExperiment = 0;		
		launchedGroups = new HashMap<String, List<Integer>>();
	}

	@Override
	public void logic() {
		// TODO Auto-generated method stub
		showOnScreen("[CtrlFolRole]");
		node.sendToSupervisor(new A3Message(MainActivity.NEW_PHONE, ""), "control");
		active = false;
	}

	@Override
	public void receiveApplicationMessage(A3Message message) {
		// TODO Auto-generated method stub
		
		switch(message.reason){
		
		case MainActivity.LONG_RTT:						
								
			for(String gType : launchedGroups.keySet())
				for(int i : launchedGroups.get(gType))
					if(node.isConnectedForApplication(gType + "_" + i) && node.isSupervisor(gType + "_" + i))
						node.sendToSupervisor(message,
							gType + "_" + i);
			break;
			
		case MainActivity.CREATE_GROUP:
			String content [] = ((String)message.object).split("_");
			String type = content[0];
			int experimentId = Integer.valueOf(content[1]);
			if(launchedGroups.containsKey(type)){
				if(!launchedGroups.get(type).contains(experimentId))
					launchedGroups.get(type).add(experimentId);
			}else
				launchedGroups.put(type, Arrays.asList(new Integer [] {experimentId}));
			
			break;
			
		case MainActivity.START_EXPERIMENT:
			
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
