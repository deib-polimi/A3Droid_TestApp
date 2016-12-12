package it.polimi.greenhouse.a3.roles;

import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import it.polimi.deepse.a3droid.a3.A3FollowerRole;
import it.polimi.deepse.a3droid.a3.A3Message;
import it.polimi.deepse.a3droid.a3.exceptions.A3ChannelNotFoundException;
import it.polimi.deepse.a3droid.a3.exceptions.A3SupervisorNotElectedException;
import it.polimi.deepse.a3droid.pattern.Timer;
import it.polimi.deepse.a3droid.pattern.TimerInterface;
import it.polimi.greenhouse.a3.events.TestEvent;
import it.polimi.greenhouse.activities.MainActivity;
import it.polimi.greenhouse.util.AppConstants;
import it.polimi.greenhouse.util.StringTimeUtil;

public class SensorFollowerRole extends A3FollowerRole implements TimerInterface {

	private int currentExperiment;
	
	private byte sPayLoad [];
	private boolean experimentIsRunning;
	private int sentCont, receivedCount;
	private double avgRTT;
	private List<Double> listRTT;
    private  String allFollowerRTTs;
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
		sentCont = receivedCount = 0;
		avgRTT = 0;
		listRTT=new ArrayList<>();
		try {
			if(node.isConnected("control") && node.waitForActivation("control")) {
                node.sendToSupervisor(
                        new A3Message(AppConstants.JOINED,
								        getGroupName() +
                                        "_" + currentExperiment +
                                        "_" + node.getUID() +
                                        "_" + getChannelId() +
                                        "_" + System.currentTimeMillis() + // shouldn't be here, clocks are different
                                        "_" + "FolRole"
                        ), "control"
                );
            }
		} catch (A3SupervisorNotElectedException e) {
			e.printStackTrace();
		} catch (A3ChannelNotFoundException e) {
			e.printStackTrace();
		}
		postUIEvent(0, "[" + getGroupName() + "_FolRole]");
	}

	@Override
	public void onDeactivation() {

		postUIEvent(0, "[" + getGroupName() + "_FolRole] deactivated");
	}

	@Override
	public void receiveApplicationMessage(A3Message message) {

		double rtt;
		switch(message.reason){
		
		case AppConstants.SET_PARAMS:
			String params [] = message.object.split("\\.");
			if(!params[0].equals("S"))
				break;
			long freq = Long.valueOf(params[1]);
			this.MAX_INTERNAL = 60 * 1000 / freq;
			this.PAYLOAD_SIZE = Integer.valueOf(params[2]);
			postUIEvent(0, "Params set to: " + freq + " Mes/min and " + PAYLOAD_SIZE + " Bytes");
			break;
			
		case AppConstants.SENSOR_PONG:

			String [] content = message.object.split("#");
			String sensorAddress = content[0];
			String date = content[1];

			if(!sensorAddress.equals(getChannelId()))
				return;

			postUIEvent(0, "Server response received");
			receivedCount ++;
			rtt = StringTimeUtil.roundTripTime(date, StringTimeUtil.getTimestamp()) / 1000;
			avgRTT = (avgRTT * (receivedCount - 1) + rtt) / receivedCount;

			checkAllMessages();
			new Timer(this, 0, (int) (Math.random() * MAX_INTERNAL)).start();
			if (receivedCount % 100 == 0)
				postUIEvent(0, receivedCount + " mex spediti.");
			break;

		case AppConstants.START_EXPERIMENT:

			if(!experimentIsRunning){
				postUIEvent(0, "Experiment has started");
				experimentIsRunning = true;
				startTimestamp = StringTimeUtil.getTimestamp();
				sentCont = receivedCount = 0;
                allFollowerRTTs="";
				sPayLoad = StringTimeUtil.createPayload(PAYLOAD_SIZE);
				sendMessage();
			}
			break;

		case AppConstants.STOP_EXPERIMENT_COMMAND:

			Log.i(MainActivity.TAG, "Stopping the experiment");
			if(experimentIsRunning){
				experimentIsRunning = false;
				checkAllMessages();
				postUIEvent(0, "Experiment has stopped");
			}
			break;

			
		default:
			break;
		}
		
	}

	private void checkAllMessages(){
		if(!experimentIsRunning && receivedCount == sentCont) {
			sendResults();
			EventBus.getDefault().post(new TestEvent(AppConstants.ALL_MESSAGES_REPLIED, "control", null));
		}
	}

	private void sendResults(){
		double runningTime = StringTimeUtil.roundTripTime(startTimestamp, StringTimeUtil.getTimestamp()) / 1000;
		float frequency = receivedCount / ((float)runningTime);
		try {
			node.sendToSupervisor(new A3Message(AppConstants.DATA, "StoS_SensorFollower:"+ "\t" + receivedCount + "\t" +
					(runningTime) + "\t" + frequency + "\t" + avgRTT), "control");
			postUIEvent(0, "Average RTT of follower sent to Control supervisor");
		} catch (A3SupervisorNotElectedException e) {
			e.printStackTrace();
		}
	}

	private void sendMessage() {
		if(experimentIsRunning) {
			sentCont++;
			sendToSupervisor(new A3Message(AppConstants.SENSOR_PING, currentExperiment + "#" + StringTimeUtil.getTimestamp(), sPayLoad));
			//node.sendToSupervisor(new A3Message(AppConstants.SENSOR_PING, channel.getChannelId() + "#" + currentExperiment + "#" + StringTimeUtil.getTimestamp(), sPayLoad), "_" + currentExperiment);
		}
	}

	@Override
	public void handleTimeEvent(int reason, Object object) {
		sendMessage();
	}	
}
