package it.polimi.greenhouse.a3.roles;

import it.polimi.greenhouse.activities.MainActivity;
import it.polimi.greenhouse.util.StringTimeUtil;
import a3.a3droid.A3FollowerRole;
import a3.a3droid.A3Message;
import a3.a3droid.Timer;
import a3.a3droid.TimerInterface;

public class SensorFollowerRole extends A3FollowerRole implements TimerInterface{

	private int currentExperiment;
	
	private String sPayLoad;
	private boolean experimentIsRunning;
	private int sentCont;
	private double avgRTT;
	private String startTimestamp;
	
	private final static long MAX_INTERNAL = 10 * 1000;
	private final static long TIMEOUT = 60 * 1000;
	
	public SensorFollowerRole() {
		super();		
	}

	@Override
	public void onActivation() {
		
		currentExperiment = Integer.valueOf(getGroupName().split("_")[1]);
		
		experimentIsRunning = false;
		sentCont = 0;
		avgRTT = 0;
		sPayLoad = StringTimeUtil.createString(4);
		node.sendToSupervisor(new A3Message(MainActivity.JOINED, getGroupName()), "control");
	}

	@Override
	public void logic() {
		showOnScreen("[" + getGroupName() + "_FolRole]");
		active = false;
	}

	@Override
	public void receiveApplicationMessage(A3Message message) {

		long rtt;
		switch(message.reason){
		case MainActivity.SENSOR_PONG:
			
			showOnScreen("Server response received");
			sentCont ++;			
			rtt = StringTimeUtil.roundTripTime(((String)message.object), StringTimeUtil.getTimestamp());
			avgRTT = (avgRTT * (sentCont - 1) + rtt) / sentCont;
			
			if(rtt > TIMEOUT && experimentIsRunning){
				experimentIsRunning = false;
				node.sendToSupervisor(new A3Message(MainActivity.LONG_RTT, ""), "control");
			}
			else{
				new Timer(this, 0, (int) (Math.random() * MAX_INTERNAL)).start();
			}
			
			if(sentCont % 100 == 0)
				showOnScreen(sentCont + " mex spediti.");
			
			break;

		case MainActivity.START_EXPERIMENT:

			if(!experimentIsRunning){
				showOnScreen("Experiment has started");
				experimentIsRunning = true;
				startTimestamp = StringTimeUtil.getTimestamp();
				sentCont = 0;
				sendMessage();
			}
			break;

		case MainActivity.STOP_EXPERIMENT_COMMAND:
			
			if(experimentIsRunning){
				showOnScreen("Experiment has stopped");
				long runningTime = StringTimeUtil.roundTripTime(startTimestamp, StringTimeUtil.getTimestamp()) / 1000;
				float frequency = sentCont / ((float)runningTime);
				
				node.sendToSupervisor(new A3Message(MainActivity.DATA, sentCont + "\t" +
						(runningTime) + "\t" + frequency + "\t" + avgRTT), "control");
				experimentIsRunning = false;
			}
			break;
		}
	}

	private void sendMessage() {
		if(experimentIsRunning)
			channel.sendToSupervisor(new A3Message(MainActivity.SENSOR_PING, currentExperiment + "#" + StringTimeUtil.getTimestamp() + "#" + sPayLoad));
	}

	@Override
	public void timerFired(int reason) {
		sendMessage();
	}
}
