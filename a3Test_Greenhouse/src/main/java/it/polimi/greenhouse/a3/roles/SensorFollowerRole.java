package it.polimi.greenhouse.a3.roles;

import android.util.Log;

import it.polimi.deepse.a3droid.A3FollowerRole;
import it.polimi.deepse.a3droid.A3Message;
import it.polimi.deepse.a3droid.Timer;
import it.polimi.deepse.a3droid.TimerInterface;
import it.polimi.greenhouse.activities.MainActivity;
import it.polimi.greenhouse.util.AppConstants;
import it.polimi.greenhouse.util.StringTimeUtil;

public class SensorFollowerRole extends A3FollowerRole implements TimerInterface {

	private int currentExperiment;
	
	private byte sPayLoad [];
	private boolean experimentIsRunning;
	private int sentCont;
	private double avgRTT;
	private String startTimestamp;
	
	private final static long TIMEOUT = 60 * 1000;
	private long MAX_INTERNAL = 10 * 1000;
	private int PAYLOAD_SIZE = 32;
	
	public SensorFollowerRole() {
		super();		
	}

	@Override
	public void onActivation() {
		
		currentExperiment = Integer.valueOf(getGroupName().split("_")[1]);
		
		experimentIsRunning = false;
		sentCont = 0;
		avgRTT = 0;
		node.sendToSupervisor(new A3Message(AppConstants.JOINED, getGroupName() + "_" + node.getUUID() + "_" + channel.getChannelId()), "control");
	}

	@Override
	public void logic() {
		showOnScreen("[" + getGroupName() + "_FolRole]");
		active = false;
	}

	@Override
	public void receiveApplicationMessage(A3Message message) {

		double rtt;
		switch(message.reason){
		
		case AppConstants.SET_PARAMS:
			String params [] = message.object.split("_");
			if(!params[0].equals("S"))
				break;
			long freq = Long.valueOf(params[1]);
			this.MAX_INTERNAL = 60 * 1000 / freq;
			this.PAYLOAD_SIZE = Integer.valueOf(params[2]);
			showOnScreen("Params set to: " + freq + " Mes/min and " + PAYLOAD_SIZE + " Bytes");
			break;
			
		case AppConstants.SENSOR_PONG:
			
			showOnScreen("Server response received");
			sentCont ++;			
			rtt = StringTimeUtil.roundTripTime(((String)message.object), StringTimeUtil.getTimestamp()) / 1000;
			avgRTT = (avgRTT * (sentCont - 1) + rtt) / sentCont;
			
			if(rtt > TIMEOUT && experimentIsRunning){
				experimentIsRunning = false;
				node.sendToSupervisor(new A3Message(AppConstants.LONG_RTT, ""), "control");
			}
			else{
				new Timer(this, 0, (int) (Math.random() * MAX_INTERNAL)).start();
			}
			
			if(sentCont % 100 == 0)
				showOnScreen(sentCont + " mex spediti.");
			
			break;

		case AppConstants.START_EXPERIMENT:

			if(!experimentIsRunning){
				showOnScreen("Experiment has started");
				experimentIsRunning = true;
				startTimestamp = StringTimeUtil.getTimestamp();
				sentCont = 0;
				sPayLoad = StringTimeUtil.createPayload(PAYLOAD_SIZE);
				sendMessage();
			}
			break;

		case AppConstants.STOP_EXPERIMENT_COMMAND:

			Log.i(MainActivity.TAG, "Stopping the experiment");

			if(experimentIsRunning){
				showOnScreen("Experiment has stopped");
				double runningTime = StringTimeUtil.roundTripTime(startTimestamp, StringTimeUtil.getTimestamp()) / 1000;
				float frequency = sentCont / ((float)runningTime);
				
				node.sendToSupervisor(new A3Message(AppConstants.DATA, "StoS: " + sentCont + "\t" +
						(runningTime) + "\t" + frequency + "\t" + avgRTT), "control");
				experimentIsRunning = false;
			}
			break;
			
		default:
			break;
		}
		
	}

	private void sendMessage() {
		if(experimentIsRunning)
			channel.sendToSupervisor(new A3Message(AppConstants.SENSOR_PING, currentExperiment + "#" + StringTimeUtil.getTimestamp(), sPayLoad));
	}

	@Override
	public void timerFired(int reason) {
		sendMessage();
	}	
}
