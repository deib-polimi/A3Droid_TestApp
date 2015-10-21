package it.polimi.greenhouse.a3.roles;

import it.polimi.greenhouse.activities.MainActivity;
import a3.a3droid.A3FollowerRole;
import a3.a3droid.A3Message;

public class ActuatorFollowerRole extends A3FollowerRole{

	public ActuatorFollowerRole() {
		super();		
	}
	@Override
	public void onActivation() {
	}
	
	@Override
	public void logic() {
		showOnScreen("[" + getGroupName() + "_FolRole]");
		active = false;
	}

	@Override
	public void receiveApplicationMessage(A3Message message) {

		switch(message.reason){		
		case MainActivity.SERVER_PING:
			showOnScreen("Received new data from a server");
			message.reason = MainActivity.SERVER_PONG;
			String [] content = ((String)message.object).split("#");
			//String serverAddress = content[0];
			String experiment = content[1];
			String sendTime = content[2];
			//String serverData = content[3];
			message.object = experiment + "#" + sendTime;
			channel.sendToSupervisor(message);
			showOnScreen("Sent response to server");
			break;
		}
	}
}
