package it.polimi.greenhouse.a3.roles;

import it.polimi.greenhouse.activities.MainActivity;

import java.util.Date;

import a3.a3droid.A3FollowerRole;
import a3.a3droid.A3Message;
import a3.a3droid.Timer;
import a3.a3droid.TimerInterface;

public class ServerFollowerRole extends A3FollowerRole implements TimerInterface{

	private long rttThreshold;
	
	private String s;
	private boolean experimentIsRunning;
	private int sentCont;
	private String startTimestamp;
	
	public ServerFollowerRole() {
		// TODO Auto-generated constructor stub
		super();		
	}

	@Override
	public void onActivation() {
		// TODO Auto-generated method stub
		
		experimentIsRunning = false;
		sentCont = 0;
	}

	@Override
	public void logic() {
		// TODO Auto-generated method stub
		showOnScreen("[" + getGroupName() + "_FolRole]");
		active = false;
	}

	@Override
	public void receiveApplicationMessage(A3Message message) {

		switch(message.reason){
		case MainActivity.PONG:
			// TODO Auto-generated method stub
			sentCont ++;
			String [] content = ((String)message.object).split("#");
			String experiment = content[1];
			showOnScreen("Forwarding server data to sensor");
			node.sendToSupervisor(message, "monitoring_" + experiment);
			break;

		case MainActivity.START_EXPERIMENT:

			startTimestamp = getTimestamp();
			sentCont = 0;

			experimentIsRunning = true;
			//sendMessage();
			break;

		case MainActivity.STOP_EXPERIMENT_COMMAND:
			
			/*experimentIsRunning = false;
			long runningTime = roundTripTime(startTimestamp, getTimestamp());
			float frequency = sentCont / ((float)(runningTime / 1000));
			
			node.sendToSupervisor(new A3Message(MainActivity.DATA, sentCont + " " +
					(runningTime/ 1000) + " " + frequency), "control");*/
			break;
		}
	}

	private void sendMessage() {
		// TODO Auto-generated method stub
		if(experimentIsRunning)
			channel.sendToSupervisor(new A3Message(MainActivity.PING, getTimestamp() + " " + s));
	}

	private String getTimestamp() {
		try{
		// TODO Auto-generated method stub
			return new Date().getTime() + "";
		}catch(Exception e){showOnScreen("getTimestamp(): " + e.getLocalizedMessage());}
		return "0";
	}
	
	private long roundTripTime(String departureTimestamp, String arrivalTimestamp) {
		// TODO Auto-generated method stub
		long i1 = 0, i2 = 0;
		
		try{
			i1 = Long.parseLong(arrivalTimestamp);
		}catch(Exception e){showOnScreen("rtt()[1]: " + e.getLocalizedMessage());}
		
		try{
			i2 = Long.parseLong(departureTimestamp);
		}catch(Exception e){showOnScreen("rtt()[2]: " + e.getLocalizedMessage());}
		return i1 - i2;
	}

	@Override
	public void timerFired(int reason) {
		// TODO Auto-generated method stub
		sendMessage();
	}
}
