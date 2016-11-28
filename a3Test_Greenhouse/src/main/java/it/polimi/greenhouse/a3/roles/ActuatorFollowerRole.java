package it.polimi.greenhouse.a3.roles;

import it.polimi.deepse.a3droid.a3.A3FollowerRole;
import it.polimi.deepse.a3droid.a3.A3Message;
import it.polimi.deepse.a3droid.a3.exceptions.A3SupervisorNotElectedException;
import it.polimi.greenhouse.util.AppConstants;

public class ActuatorFollowerRole extends A3FollowerRole {

	public ActuatorFollowerRole() {
		super();		
	}
	@Override
	public void onActivation() {
		try {
			node.sendToSupervisor(new A3Message(AppConstants.JOINED, getGroupName() + "_" + node.getUID() + "_" + getChannelId()), "control");
		} catch (A3SupervisorNotElectedException e) {
			e.printStackTrace();
		}
		postUIEvent(0, "[" + getGroupName() + "_FolRole]");
	}

	@Override
	public void receiveApplicationMessage(A3Message message) {

		switch(message.reason){		
		case AppConstants.SERVER_PING:
			//showOnScreen("Received new data from a server");
			message.reason = AppConstants.SERVER_PONG;
			String [] content = ((String)message.object).split("#");
			//String serverAddress = content[0];
			String experiment = content[1];
			String sendTime = content[2];
			//byte serverData [] = message.bytes;
			message.object = experiment + "#" + sendTime;
			sendToSupervisor(message);
			//showOnScreen("Sent response to server");
			break;
			
		default:
			break;
		}
	}
}
