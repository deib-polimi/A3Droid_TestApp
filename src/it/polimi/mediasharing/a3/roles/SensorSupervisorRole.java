package it.polimi.mediasharing.a3.roles;

import it.polimi.mediasharing.activities.MainActivity;
import a3.a3droid.A3Message;
import a3.a3droid.A3SupervisorRole;

public class SensorSupervisorRole extends A3SupervisorRole {

	//private int currentExperiment;
	private boolean startExperiment;
	private long lcat;
	
	public SensorSupervisorRole() {
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
			channel.sendUnicast(message, message.senderAddress);
			break;
		
		case MainActivity.MEDIA_DATA:
			message.reason = MainActivity.MEDIA_DATA_SHARE;
			message.object = lcat + "#" + (String)message.object;
			channel.sendBroadcast(message);
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
