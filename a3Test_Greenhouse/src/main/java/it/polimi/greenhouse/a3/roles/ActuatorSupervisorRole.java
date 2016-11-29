package it.polimi.greenhouse.a3.roles;

import it.polimi.deepse.a3droid.a3.A3Message;
import it.polimi.deepse.a3droid.a3.A3SupervisorRole;
import it.polimi.deepse.a3droid.a3.exceptions.A3NoGroupDescriptionException;
import it.polimi.deepse.a3droid.a3.exceptions.A3SupervisorNotElectedException;
import it.polimi.greenhouse.util.AppConstants;

public class ActuatorSupervisorRole extends A3SupervisorRole {

	private boolean startExperiment;
	private boolean serverPinged;
	
	public ActuatorSupervisorRole() {
		super();		
	}

	@Override
	public void onActivation() {
		startExperiment = true;
		serverPinged = false;
		try {
			node.connect("server_0");
			try {
				node.sendToSupervisor(new A3Message(AppConstants.JOINED, getGroupName() + "_" + node.getUID() + "_" + getChannelId()), "control");
			} catch (A3SupervisorNotElectedException e) {
				e.printStackTrace();
			}
			postUIEvent(0, "[" + getGroupName() + "_SupRole]");
		} catch (A3NoGroupDescriptionException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void receiveApplicationMessage(A3Message message) {
		switch(message.reason){
		case AppConstants.SERVER_PING:
			if(serverPinged){
				serverPinged = false;
				break;
			}else
				serverPinged = true;
			//showOnScreen("Received new data from a server");
			String [] content = ((String)message.object).split("#");
			String serverAddress = content[0];
			String experiment = content[1];
			String sendTime = content[2];
			//byte serverData [] = message.bytes;
			message.object = serverAddress + "#" + experiment + "#" + sendTime;
			sendBroadcast(message);
			//showOnScreen("Broadcasted data to follower actuators");
			message.reason = AppConstants.SERVER_PONG;
			message.object = sendTime;
			try {
				node.sendToSupervisor(message, "server_0");
			} catch (A3SupervisorNotElectedException e) {
				e.printStackTrace();
			}
			break;
			
		case AppConstants.SERVER_PONG:
			//showOnScreen("Received follower actuator response");
			content = ((String)message.object).split("#");
			experiment = content[0];
			sendTime = content[1];
			message.object = sendTime;
			try {
				node.sendToSupervisor(message, "server_0");
			} catch (A3SupervisorNotElectedException e) {
				e.printStackTrace();
			}
			break;
		
		case AppConstants.START_EXPERIMENT:
			if(startExperiment){
				startExperiment = false;
				sendBroadcast(message);
			}
			else
				startExperiment = true;
			
			break;
			
		case AppConstants.STOP_EXPERIMENT:
			break;
			
		case AppConstants.LONG_RTT:
			
			sendBroadcast(new A3Message(AppConstants.STOP_EXPERIMENT_COMMAND, ""));
			break;
			
		default:
			break;
		}
	}
	
	public void memberAdded(String name) {
		//showOnScreen("Entered: " + name);
		A3Message msg = new A3Message(AppConstants.MEMBER_ADDED, name);
		try {
			node.sendToSupervisor(msg, "control");
		} catch (A3SupervisorNotElectedException e) {
			e.printStackTrace();
		}
	}

	public void memberRemoved(String name) {
		//showOnScreen("Exited: " + name);
		A3Message msg = new A3Message(AppConstants.MEMBER_REMOVED, name);
		try {
			node.sendToSupervisor(msg, "control");
		} catch (A3SupervisorNotElectedException e) {
			e.printStackTrace();
		}
	}
}
