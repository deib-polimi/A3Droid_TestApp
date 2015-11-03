package it.polimi.greenhouse.a3.roles;

import it.polimi.greenhouse.activities.MainActivity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import a3.a3droid.A3Message;
import a3.a3droid.A3SupervisorRole;
import android.os.Environment;

/**
 * This class is the role the supervisor of "control" group plays.
 * @author Francesco
 *
 */
public class ControlSupervisorRole extends A3SupervisorRole {

	private Set<String> vmIds;
	private Map<String, Map<Integer, Set<String>>> launchedGroups;
	private int totalGroups;

	private File sd;
	private File f;
	private FileWriter fw;
	private BufferedWriter bw;
	private volatile int dataToWaitFor;
	private volatile String result;
	private int numberOfTrials;
	
	public ControlSupervisorRole(){
		super();
	}
	
	@Override
	public void onActivation() {
		vmIds = Collections.synchronizedSet(new HashSet<String>());
		launchedGroups = new ConcurrentHashMap<String, Map<Integer, Set<String>>>();
		totalGroups = 0;
		dataToWaitFor = 0;
		numberOfTrials = 1;
	}	

	@Override
	public void logic() {
		showOnScreen("[CtrlSupRole]");
		node.sendToSupervisor(new A3Message(MainActivity.NEW_PHONE, node.getUUID()), "control");		
		active = false;
	}

	@Override
	public void receiveApplicationMessage(A3Message message) {
		
		String content []; 
		switch(message.reason){
			
			case MainActivity.JOINED:		
				message.reason = MainActivity.ADD_MEMBER;
				channel.sendBroadcast(message);
				for(String gType : launchedGroups.keySet())
					for(int i : launchedGroups.get(gType).keySet())
						if(node.isConnectedForApplication(gType + "_" + i) && node.isSupervisor(gType + "_" + i))
							node.sendToSupervisor(message,
								gType + "_" + i);
				break;
				
			case MainActivity.ADD_MEMBER:
				content = ((String)message.object).split("_");
				String type = content[0];
				int experimentId = Integer.valueOf(content[1]);
				String uuid = content[2];
				if(launchedGroups.containsKey(type))
					if(launchedGroups.get(type).containsKey(experimentId))
						launchedGroups.get(type).get(experimentId).add(uuid);	
					else
						launchedGroups.get(type).put(experimentId, new HashSet<String>(Arrays.asList(new String [] {uuid})));
				else{
					Map<Integer, Set<String>> newGroup = new ConcurrentHashMap<Integer, Set<String>>();
					newGroup.put(experimentId, new HashSet<String>(Arrays.asList(new String [] {uuid})));
					launchedGroups.put(type, newGroup);
				}
				break;
				
			case MainActivity.NEW_PHONE:
				
				vmIds.add(message.object);
				numberOfTrials = 1;
				showOnScreen("Telefoni connessi: " + vmIds.size());
				break;
				
			case MainActivity.LONG_RTT:
				
				if(message.object.equals(channel.getChannelId()))
					break;
				
				sd = Environment.getExternalStorageDirectory();
				f = new File(sd, MainActivity.EXPERIMENT_PREFIX + "Greenhouse_" + vmIds.size() + ".txt");
	
				try {
					fw = new FileWriter(f, true);
					bw = new BufferedWriter(fw);
				} catch (IOException e) {
					showOnScreen(e.getLocalizedMessage());
				}
				message.object = channel.getChannelId();
				channel.sendBroadcast(message);
				for(String gType : launchedGroups.keySet())
					for(int i : launchedGroups.get(gType).keySet())
						if(node.isConnectedForApplication(gType + "_" + i) && node.isSupervisor(gType + "_" + i))
							node.sendToSupervisor(message,
								gType + "_" + i);
				break;
				
			case MainActivity.DATA:
							
				result = result + ((String)message.object).replace(".", ",") + "\n";
				dataToWaitFor --;
				
				if(dataToWaitFor <= 0){
					
					try {
						bw.write(result);
						bw.flush();
					} catch (IOException e) {showOnScreen("ECCEZIONE IN CtrlSupRole [bw.flush()]: " + e.getLocalizedMessage());}
					
					showOnScreen("--- TENTATIVO " + numberOfTrials + " TERMINATO ---");
					numberOfTrials ++;
				}
				
				break;
				
			case MainActivity.CREATE_GROUP_USER_COMMAND:
				message.reason = MainActivity.CREATE_GROUP;
				channel.sendBroadcast(message);
				for(String gType : launchedGroups.keySet())
					for(int i : launchedGroups.get(gType).keySet())
						if(node.isConnectedForApplication(gType + "_" + i) && node.isSupervisor(gType + "_" + i))
							node.sendToSupervisor(message,
								gType + "_" + i);
				break;
				
			case MainActivity.CREATE_GROUP:
				break;
				
			case MainActivity.START_EXPERIMENT_USER_COMMAND:
				
				showOnScreen("--- TENTATIVO " + numberOfTrials + "---");
				
				result = "";
				dataToWaitFor = 0;
				for(String gType : launchedGroups.keySet())
					if(gType.equals("monitoring"))
						for(int i : launchedGroups.get(gType).keySet())
							dataToWaitFor += launchedGroups.get(gType).get(i).size();
				
				if(launchedGroups.containsKey("actuators"))
					dataToWaitFor++;
				
				message.reason = MainActivity.START_EXPERIMENT;
				channel.sendBroadcast(message);
				for(String gType : launchedGroups.keySet())
					for(int i : launchedGroups.get(gType).keySet())
							if(node.isConnectedForApplication(gType + "_" + i) && node.isSupervisor(gType + "_" + i))
								node.sendToSupervisor(new A3Message(MainActivity.START_EXPERIMENT, ""),
										gType + "_" + i);
				break;
				
			case MainActivity.STOP_EXPERIMENT:
				
				showOnScreen("--- STOP_EXPERIMENT: ATTENDERE 10s CIRCA ---");
				
				channel.sendBroadcast(message);
				for(String gType : launchedGroups.keySet())
					for(int i : launchedGroups.get(gType).keySet())
						if(node.isConnectedForApplication(gType + "_" + i) && !node.isSupervisor(gType + "_" + i))
							node.disconnect(gType + "_" + i, true);
					
				synchronized(this){
					try {
						wait(10000);
					} catch (InterruptedException e) {}
				}
	
				for(String gType : launchedGroups.keySet())
					for(int i : launchedGroups.get(gType).keySet())
						if(node.isConnectedForApplication(gType + "_" + i))
							node.disconnect(gType + "_" + i, true);
				
				launchedGroups.clear();
				showOnScreen("--- ESPERIMENTO TERMINATO ---");
				break;
		}	
	}
}
