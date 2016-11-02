package it.polimi.greenhouse.a3.roles;

import it.polimi.deepse.a3droid.a3.A3FollowerRole;
import it.polimi.deepse.a3droid.a3.A3Message;
import it.polimi.greenhouse.util.AppConstants;

public class ServerFollowerRole extends A3FollowerRole {

	public ServerFollowerRole() {
		super();		
	}

	@Override
	public void onActivation() {
		node.sendToSupervisor(new A3Message(AppConstants.JOINED, getGroupName() + "_" + node.getUID() + "_" + getChannelId()), "control");
	}

	@Override
	public void logic() {
		postUIEvent(0, "[" + getGroupName() + "_FolRole]");
	}

	@Override
	public void receiveApplicationMessage(A3Message message) {
		
		String [] content;
		String experiment;
		switch(message.reason){
		case AppConstants.SENSOR_PONG:
			postUIEvent(0, "Forwarding server data to sensor");
			content = ((String)message.object).split("#");
			experiment = content[1];
			node.sendToSupervisor(message, "monitoring_" + experiment);
			break;
			
		case AppConstants.SERVER_PING:
			content = ((String)message.object).split("#");
			experiment = content[0];
			message.object = message.senderAddress + "#" + (String)message.object;
			if(node.isConnectedForApplication("actuators_" + experiment)){
				postUIEvent(0, "Forwarding server data to actuators");
				node.sendToSupervisor(message, "actuators_" + experiment);
			}
			break;

		case AppConstants.START_EXPERIMENT:
			break;

		case AppConstants.STOP_EXPERIMENT_COMMAND:
			break;
			
		default:
			break;
		}
	}
}
