package it.polimi.greenhouse.a3.roles;

import it.polimi.greenhouse.activities.MainActivity;
import a3.a3droid.A3Message;
import a3.a3droid.A3SupervisorRole;

public class ActuatorSupervisorRole extends A3SupervisorRole {

	private boolean startExperiment;
	private boolean serverPinged;
	private int currentExperiment;
	
	public ActuatorSupervisorRole() {
		super();		
	}

	@Override
	public void onActivation() {
		startExperiment = true;
		serverPinged = false;
		currentExperiment = Integer.valueOf(getGroupName().split("_")[1]);
		node.connect("server_" + currentExperiment, false, true);
	}	

	@Override
	public void logic() {
		showOnScreen("[" + getGroupName() + "_SupRole]");
		active = false;
	}

	@Override
	public void receiveApplicationMessage(A3Message message) {
		switch(message.reason){
		case MainActivity.SERVER_PING:
			if(serverPinged){
				serverPinged = false;
				break;
			}else
				serverPinged = true;
			showOnScreen("Received new data from a server");
			String [] content = ((String)message.object).split("#");
			String serverAddress = content[0];
			String experiment = content[1];
			String sendTime = content[2];
			String serverData = content[3];
			message.object = serverAddress + "#" + experiment + "#" + sendTime + "#" + serverData;
			channel.sendBroadcast(message);
			showOnScreen("Broadcasted response to follower actuators");
			break;
			
		case MainActivity.SERVER_PONG:
			showOnScreen("Received follower actuator response");
			content = ((String)message.object).split("#");
			experiment = content[0];
			sendTime = content[1];
			message.object = sendTime;
			node.sendToSupervisor(message, "server_" + experiment);
			break;
		
		case MainActivity.START_EXPERIMENT:
			if(startExperiment){
				startExperiment = false;
				channel.sendBroadcast(message);
			}
			else
				startExperiment = true;
			
			break;
			
		case MainActivity.STOP_EXPERIMENT:
			break;
			
		case MainActivity.LONG_RTT:
			
			channel.sendBroadcast(new A3Message(MainActivity.STOP_EXPERIMENT_COMMAND, ""));
			break;
		}
	}
}
