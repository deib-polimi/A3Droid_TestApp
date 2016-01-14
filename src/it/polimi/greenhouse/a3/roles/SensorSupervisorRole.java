package it.polimi.greenhouse.a3.roles;

import it.polimi.greenhouse.activities.MainActivity;
import it.polimi.greenhouse.util.StringTimeUtil;
import a3.a3droid.A3Message;
import a3.a3droid.A3SupervisorRole;
import a3.a3droid.Timer;
import a3.a3droid.TimerInterface;

public class SensorSupervisorRole extends A3SupervisorRole implements TimerInterface{

	private boolean startExperiment;
	private boolean experimentIsRunning;
	private boolean paramsSet = false;
	private int currentExperiment;
	private int sentCont;
	private double avgRTT;
	private byte sPayLoad [];
	private String startTimestamp;
	
	private final static long TIMEOUT = 60 * 1000;
	private long MAX_INTERNAL = 10 * 1000;
	private int PAYLOAD_SIZE = 32;
	
	public SensorSupervisorRole() {
		super();		
	}

	@Override
	public void onActivation() {
		currentExperiment = Integer.valueOf(getGroupName().split("_")[1]);
		startExperiment = true;		
		experimentIsRunning = false;
		sentCont = 0;
		avgRTT = 0;
		node.connect("server_0", true, true);
		node.sendToSupervisor(new A3Message(MainActivity.JOINED, getGroupName() + "_" + node.getUUID()), "control");
	}

	@Override
	public void logic() {
		showOnScreen("[" + getGroupName() + "_SupRole]");
		active = false;
	}

	@Override
	public void receiveApplicationMessage(A3Message message) {
		switch(message.reason){
		
		case MainActivity.SET_PARAMS:						
			if(message.senderAddress.equals(this.channel.getChannelId()) && !paramsSet){
				String params [] = message.object.split("_");
				if(!params[0].equals("S"))
					break;
				paramsSet = true;
				channel.sendBroadcast(message);						
				long freq = Long.valueOf(params[1]);
				this.MAX_INTERNAL = 60 * 1000 / freq;
				this.PAYLOAD_SIZE = Integer.valueOf(params[2]);
				showOnScreen("Params set to: " + freq + " Mes/min and " + PAYLOAD_SIZE + " Bytes");				
			}
			break;
			
		case MainActivity.ADD_MEMBER:
			if(experimentIsRunning){
				message.reason = MainActivity.SET_PARAMS;
				message.object = (60 * 1000 / this.MAX_INTERNAL) + "_" + PAYLOAD_SIZE;
				channel.sendBroadcast(message);
			}
			break;
		
		case MainActivity.SENSOR_PING:
			//showOnScreen("Forwarding sensor data to server");
			message.object = message.senderAddress + "#" + (String)message.object;
			node.sendToSupervisor(message, "server_0");
			break;
			
		case MainActivity.SENSOR_PONG:
			String sensorAddress = ((String)message.object).split("#")[0];
			//String experiment = ((String)message.object).split("#")[1];
			String sendTime = ((String)message.object).split("#")[2];
			message.object = sendTime;

			if(!sensorAddress.equals(channel.getChannelId())){
				//showOnScreen("Forwarding server response to follower sensor");
				channel.sendUnicast(message, sensorAddress);
			}else{
				showOnScreen("Server response received");
				sentCont ++;
				double rtt = StringTimeUtil.roundTripTime(((String)message.object), StringTimeUtil.getTimestamp()) / 1000;
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
			}
			break;

		case MainActivity.START_EXPERIMENT:
			if(startExperiment){
				if(!experimentIsRunning){
					startExperiment = false;
					experimentIsRunning = true;
					channel.sendBroadcast(message);
					startTimestamp = StringTimeUtil.getTimestamp();
					sentCont = 0;
					sPayLoad = StringTimeUtil.createPayload(PAYLOAD_SIZE);
					sendMessage();
				}
			}
			else
				startExperiment = true;
			
			break;
			
		case MainActivity.LONG_RTT:
			
			channel.sendBroadcast(new A3Message(MainActivity.STOP_EXPERIMENT_COMMAND, ""));

			if(experimentIsRunning){
				paramsSet = false;
				experimentIsRunning = false;
				double runningTime = StringTimeUtil.roundTripTime(startTimestamp, StringTimeUtil.getTimestamp()) / 1000;
				float frequency = sentCont / (float)(runningTime);
				node.sendToSupervisor(new A3Message(MainActivity.DATA, "StoS: " + sentCont + "\t" +
						runningTime + "\t" + frequency + "\t" + avgRTT), "control");
			}
			break;

		}
	}
	
	private void sendMessage() {
		if(experimentIsRunning)
			channel.sendToSupervisor(new A3Message(MainActivity.SENSOR_PING, currentExperiment + "#" + StringTimeUtil.getTimestamp(), sPayLoad));
	}

	@Override
	public void timerFired(int reason) {
		sendMessage();
	}
}
