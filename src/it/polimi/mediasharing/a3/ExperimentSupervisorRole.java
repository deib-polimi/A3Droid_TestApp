package it.polimi.mediasharing.a3;

import it.polimi.mediasharing.activities.MainActivity;
import it.polimi.mediasharing.sockets.Server;

import java.io.IOException;

import a3.a3droid.A3Message;
import a3.a3droid.A3SupervisorRole;

public class ExperimentSupervisorRole extends A3SupervisorRole {

	private int currentExperiment;
	private boolean startExperiment;
	private Server server;
	
	public ExperimentSupervisorRole() {
		// TODO Auto-generated constructor stub
		super();		
	}

	@Override
	public void onActivation() {
		// TODO Auto-generated method stub
		
		currentExperiment = Integer.valueOf(getGroupName().split("_")[1]);
		startExperiment = true;		
	}	
	
	private void startFileServer(){
		try {
			server = new Server(4444, this);
			server.start();
		} catch (IOException e) {
			showOnScreen("Error creating the file server.");
		}
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
			channel.sendBroadcast(message);
			break;
			
		case MainActivity.START_EXPERIMENT:
			if(startExperiment){
				startFileServer();
				startExperiment = false;
				//channel.sendBroadcast(message);
			}
			else
				startExperiment = true;
			
			break;
			
		case MainActivity.STOP_EXPERIMENT:
			server.interrupt();
			break;
			
		case MainActivity.LONG_RTT:
			
			server.interrupt();
			channel.sendBroadcast(new A3Message(MainActivity.STOP_EXPERIMENT_COMMAND, ""));
			break;
		}
	}
}
