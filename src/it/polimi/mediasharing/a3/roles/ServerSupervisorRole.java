package it.polimi.mediasharing.a3.roles;

import it.polimi.mediasharing.activities.MainActivity;
import a3.a3droid.A3Message;
import a3.a3droid.A3SupervisorRole;

public class ServerSupervisorRole extends A3SupervisorRole {

	//private int currentExperiment;
	private boolean startExperiment;
	
	public ServerSupervisorRole() {
		// TODO Auto-generated constructor stub
		super();		
	}

	@Override
	public void onActivation() {
		// TODO Auto-generated method stub
		//currentExperiment = Integer.valueOf(getGroupName().split("_")[1]);
		startExperiment = true;		
	}	
	
	@Override
	public void logic() {
		showOnScreen("[" + getGroupName() + "_SupRole]");
		active = false;
	}

	@Override
	public void receiveApplicationMessage(A3Message message) {
		// TODO Auto-generated method stub
		switch(message.reason){
			case MainActivity.PING:
				message.reason = MainActivity.PONG;
				String [] content = ((String)message.object).split("#");
				String sensorAddress = content[0];
				String sendTime = content[1];
				String experiment = content[2];
				String sensorData = content[3];
				message.object = sensorAddress + "#" + sendTime;
				channel.sendUnicast(message, message.senderAddress);
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
