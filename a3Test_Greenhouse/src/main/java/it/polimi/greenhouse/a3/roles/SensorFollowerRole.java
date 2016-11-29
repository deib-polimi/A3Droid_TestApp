package it.polimi.greenhouse.a3.roles;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import it.polimi.deepse.a3droid.a3.A3FollowerRole;
import it.polimi.deepse.a3droid.a3.A3Message;
import it.polimi.deepse.a3droid.a3.exceptions.A3ChannelNotFoundException;
import it.polimi.deepse.a3droid.a3.exceptions.A3SupervisorNotElectedException;
import it.polimi.deepse.a3droid.pattern.Timer;
import it.polimi.deepse.a3droid.pattern.TimerInterface;
import it.polimi.greenhouse.activities.MainActivity;
import it.polimi.greenhouse.util.AppConstants;
import it.polimi.greenhouse.util.StringTimeUtil;

public class SensorFollowerRole extends A3FollowerRole implements TimerInterface {

	private int currentExperiment;
	
	private byte sPayLoad [];
	private boolean experimentIsRunning;
	private int sentCont;
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
		sentCont = 0;
		avgRTT = 0;
		listRTT=new ArrayList<>();
		try {
			if(node.isConnected("control") && node.waitForActivation("control"))
				node.sendToSupervisor(
						new A3Message(AppConstants.JOINED, getGroupName() +
								"_" + currentExperiment +
								"_" + node.getUID() +
								"_" + getChannelId()
						), "control"
				);
		} catch (A3SupervisorNotElectedException e) {
			e.printStackTrace();
		} catch (A3ChannelNotFoundException e) {
			e.printStackTrace();
		}
		postUIEvent(0, "[" + getGroupName() + "_FolRole]");
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
			postUIEvent(0, "Params set to: " + freq + " Mes/min and " + PAYLOAD_SIZE + " Bytes");
			break;
			
		case AppConstants.SENSOR_PONG:

			String [] content = message.object.split("#");
			String sensorAddress = content[0];
			String date = content[1];

			if(!sensorAddress.equals(getChannelId()))
				return;

			postUIEvent(0, "Server response received");
			sentCont ++;			
			rtt = StringTimeUtil.roundTripTime(date, StringTimeUtil.getTimestamp()) / 1000;
            listRTT.add(rtt);
			avgRTT = (avgRTT * (sentCont - 1) + rtt) / sentCont;
			
			if(rtt > TIMEOUT && experimentIsRunning){
				experimentIsRunning = false;
				try {
					node.sendToSupervisor(new A3Message(AppConstants.LONG_RTT, ""), "control");
				} catch (A3SupervisorNotElectedException e) {
					e.printStackTrace();
				}
			}
			else{
				new Timer(this, 0, (int) (Math.random() * MAX_INTERNAL)).start();
			}
			
			if(sentCont % 100 == 0)
				postUIEvent(0, sentCont + " mex spediti.");
			
			break;

		case AppConstants.START_EXPERIMENT:

			if(!experimentIsRunning){
				postUIEvent(0, "Experiment has started");
				experimentIsRunning = true;
				startTimestamp = StringTimeUtil.getTimestamp();
				sentCont = 0;
                allFollowerRTTs="";
				sPayLoad = StringTimeUtil.createPayload(PAYLOAD_SIZE);
				sendMessage();
			}
			break;

		case AppConstants.STOP_EXPERIMENT_COMMAND:

			Log.i(MainActivity.TAG, "Stopping the experiment");

			if(experimentIsRunning){
				postUIEvent(0, "Experiment has stopped");
				double runningTime = StringTimeUtil.roundTripTime(startTimestamp, StringTimeUtil.getTimestamp()) / 1000;
				float frequency = sentCont / ((float)runningTime);


                for (double indiRTT: listRTT
                     ) {
                    allFollowerRTTs+=String.valueOf(indiRTT)+" ";
                }

				try {
					node.sendToSupervisor(new A3Message(AppConstants.DATA, "StoS_SensorFollower: " + sentCont + "\t " +
                            (runningTime) + "\t " + frequency + "\t " + avgRTT+"\t IndividualRTT: "+allFollowerRTTs), "control");
				} catch (A3SupervisorNotElectedException e) {
					e.printStackTrace();
				}
				experimentIsRunning = false;
			}
			break;
			
		default:
			break;
		}
		
	}

	private void sendMessage() {
		if(experimentIsRunning)
			sendToSupervisor(new A3Message(AppConstants.SENSOR_PING, currentExperiment + "#" + StringTimeUtil.getTimestamp(), sPayLoad));
			//node.sendToSupervisor(new A3Message(AppConstants.SENSOR_PING, channel.getChannelId() + "#" + currentExperiment + "#" + StringTimeUtil.getTimestamp(), sPayLoad), "_" + currentExperiment);
	}

	@Override
	public void handleTimeEvent(int reason, Object object) {
		sendMessage();
	}	
}
