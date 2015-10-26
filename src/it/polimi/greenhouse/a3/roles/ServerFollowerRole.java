package it.polimi.greenhouse.a3.roles;

import it.polimi.greenhouse.activities.MainActivity;
import a3.a3droid.A3FollowerRole;
import a3.a3droid.A3Message;

public class ServerFollowerRole extends A3FollowerRole{

	public ServerFollowerRole() {
		super();		
	}

	@Override
	public void onActivation() {
		node.sendToSupervisor(new A3Message(MainActivity.JOINED, getGroupName()), "control");
	}

	@Override
	public void logic() {
		showOnScreen("[" + getGroupName() + "_FolRole]");
		active = false;
	}

	@Override
	public void receiveApplicationMessage(A3Message message) {
		
		String [] content;
		String experiment;
		switch(message.reason){
		case MainActivity.SENSOR_PONG:
			content = ((String)message.object).split("#");
			experiment = content[1];
			showOnScreen("Forwarding server data to sensor");
			node.sendToSupervisor(message, "monitoring_" + experiment);
			break;
			
		case MainActivity.SERVER_PING:
			showOnScreen("Forwarding server data to actuators");
			content = ((String)message.object).split("#");
			experiment = content[0];
			message.object = message.senderAddress + "#" + (String)message.object;
			node.sendToSupervisor(message, "actuators_" + experiment);
			break;

		case MainActivity.START_EXPERIMENT:
			break;

		case MainActivity.STOP_EXPERIMENT_COMMAND:
			break;
		}
	}
}
