package it.polimi.greenhouse.a3.roles;

import it.polimi.greenhouse.util.AppConstants;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import a3.a3droid.A3Message;
import android.os.Environment;

/**
 * This class is the role the supervisor of "control" group plays.
 * @author Francesco
 *
 */
public class ControlSupervisorRole extends SupervisorRole {	

	private File sd;
	private File f;
	private FileWriter fw;
	private BufferedWriter bw;
	private volatile int dataToWaitFor;
	private volatile String result;
	private int numberOfTrials;
	private boolean experimentIsRunning;
	
	private Set<String> vmIds;	
	
	public ControlSupervisorRole(){
		super();
	}
	
	@Override
	public void onActivation() {
		vmIds = Collections.synchronizedSet(new HashSet<String>());
		dataToWaitFor = 0;
		numberOfTrials = 1;
	}	

	@Override
	public void logic() {
		showOnScreen("[CtrlSupRole]");
		node.sendToSupervisor(new A3Message(AppConstants.NEW_PHONE, node.getUUID()), "control");		
		active = false;
	}

	@Override
	public void receiveApplicationMessage(A3Message message) {
		
		String content []; 
		switch(message.reason){
			
			case AppConstants.JOINED:		
				message.reason = AppConstants.ADD_MEMBER;
				message.object += "_" + message.senderAddress;
				channel.sendBroadcast(message);
				sendToConnectedSupervisors(message);				
				break;
				
			case AppConstants.ADD_MEMBER:
				content = ((String)message.object).split("_");
				String type = content[0];
				int experimentId = Integer.valueOf(content[1]);
				String uuid = content[2];
				String name = content[3];
				if(!type.equals("server_0"))
					removeGroupMember(uuid);				
				addGroupMember(type, experimentId, uuid, name);
				if(experimentIsRunning){
					message.reason = AppConstants.START_EXPERIMENT;
					message.object = "";					
					channel.sendBroadcast(message);
					sendToConnectedSupervisors(message);
				}
				break;
				
			case AppConstants.MEMBER_ADDED:								
				resetCount();
				break;
				
			case AppConstants.MEMBER_REMOVED:				
				removeGroupMember(retrieveGroupMemberUuid(message.object));
				resetCount();
				break;
				
			case AppConstants.NEW_PHONE:
				
				vmIds.add(message.object);
				numberOfTrials = 1;
				showOnScreen("Telefoni connessi: " + vmIds.size());
				break;	
				
			case AppConstants.SET_PARAMS_COMMAND:
				message.reason = AppConstants.SET_PARAMS;
				channel.sendBroadcast(message);
				sendToConnectedSupervisors(message);
				break;
				
			case AppConstants.CREATE_GROUP_USER_COMMAND:
				message.reason = AppConstants.CREATE_GROUP;
				channel.sendBroadcast(message);
				sendToConnectedSupervisors(message);				
				break;
				
			case AppConstants.CREATE_GROUP:
				break;
				
			case AppConstants.START_EXPERIMENT_USER_COMMAND:
				
				if(experimentIsRunning)
					break;
				
				showOnScreen("--- TENTATIVO " + numberOfTrials + "---");
				
				experimentIsRunning = true;
				result = "";
				resetCount();
				
				message.reason = AppConstants.START_EXPERIMENT;
				channel.sendBroadcast(message);
				sendToConnectedSupervisors(message);
				break;
				
			case AppConstants.LONG_RTT:
				
				if(message.object.equals(channel.getChannelId()) || !experimentIsRunning)
					break;
				
				experimentIsRunning = false;
				sd = Environment.getExternalStorageDirectory();
				f = new File(sd, AppConstants.EXPERIMENT_PREFIX + "Greenhouse_" + vmIds.size() + ".txt");
	
				try {
					fw = new FileWriter(f, true);
					bw = new BufferedWriter(fw);
				} catch (IOException e) {
					showOnScreen(e.getLocalizedMessage());
				}
				message.object = channel.getChannelId();
				channel.sendBroadcast(message);
				sendToConnectedSupervisors(message);				
				break;
				
			case AppConstants.DATA:
							
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
				
			case AppConstants.STOP_EXPERIMENT:
				
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
				
			default:
				break;
		}	
	}	
	
	@Override
	protected void removeGroupMember(String uuid) {
		super.removeGroupMember(uuid);
		vmIds.remove(uuid);
	}
	
	private void resetCount(){
		dataToWaitFor = 0;
		for(String gType : launchedGroups.keySet())
			if(gType.equals("monitoring"))
				for(int i : launchedGroups.get(gType).keySet())
					dataToWaitFor += launchedGroups.get(gType).get(i).size();
		
		if(launchedGroups.containsKey("actuators") && !launchedGroups.get("actuators").isEmpty())
			dataToWaitFor++;
	}
	
	private void sendToConnectedSupervisors(A3Message message){
		for(String gType : launchedGroups.keySet())
			for(int i : launchedGroups.get(gType).keySet())
				if(node.isConnectedForApplication(gType + "_" + i) && node.isSupervisor(gType + "_" + i))
					node.sendToSupervisor(message,
						gType + "_" + i);
	}	

	@Override
	public void memberAdded(String name) {
		showOnScreen("Entered: " + name);
		A3Message msg = new A3Message(AppConstants.MEMBER_ADDED, name);
		channel.sendBroadcast(msg);
		sendToConnectedSupervisors(msg);
	}

	@Override
	public void memberRemoved(String name) {	
		showOnScreen("Exited: " + name);
		A3Message msg = new A3Message(AppConstants.MEMBER_REMOVED, name);
		channel.sendBroadcast(msg);
		sendToConnectedSupervisors(msg);
	}
}
