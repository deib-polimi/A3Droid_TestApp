package it.polimi.greenhouse.a3.roles;

import it.polimi.greenhouse.activities.MainActivity;
import it.polimi.greenhouse.util.StringTimeUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import a3.a3droid.A3Message;
import a3.a3droid.A3SupervisorRole;
import a3.a3droid.Timer;
import a3.a3droid.TimerInterface;
import android.annotation.SuppressLint;

public class ServerSupervisorRole extends A3SupervisorRole implements TimerInterface{

	private boolean startExperiment;
	private boolean experimentIsRunning;
	private int sentCont;
	private double avgRTT;
	private int dataToWaitFor;
	private String startTimestamp;
	private String sPayLoad;
	private final static long MAX_INTERNAL = 30 * 1000;
	private final static long TIMEOUT = 60 * 1000;
	private final static int PAYLOAD_SIZE = 64;
	private Map<String, Map<Integer, Integer>> launchedGroups;
	
	public ServerSupervisorRole() {
		super();		
	}

	@Override
	public void onActivation() {
		startExperiment = true;
		experimentIsRunning = false;
		sentCont = 0;
		avgRTT = 0;
		dataToWaitFor = 0;
		launchedGroups = new ConcurrentHashMap<String, Map<Integer, Integer>>();		
		node.sendToSupervisor(new A3Message(MainActivity.JOINED, getGroupName()), "control");
	}	
	
	@Override
	public void logic() {
		showOnScreen("[" + getGroupName() + "_SupRole]");
		active = false;
	}

	@SuppressLint("UseSparseArrays")
	@Override
	public void receiveApplicationMessage(A3Message message) {
		
		long rtt;
		switch(message.reason){
			case MainActivity.SENSOR_PING:
				showOnScreen("Received new data from a sensor");
				message.reason = MainActivity.SENSOR_PONG;
				String [] content = ((String)message.object).split("#");
				String sensorAddress = content[0];
				String experiment = content[1];
				String sendTime = content[2];
				//String sensorData = content[3];
				message.object = sensorAddress + "#" + experiment + "#" + sendTime;
				channel.sendUnicast(message, message.senderAddress);
				showOnScreen("Sent response to sensor");
				break;
				
				
			case MainActivity.SERVER_PONG:
				showOnScreen("Actuator response received");
				sentCont ++;
				rtt = StringTimeUtil.roundTripTime(((String)message.object), StringTimeUtil.getTimestamp());
				avgRTT = (avgRTT * (sentCont - 1) + rtt) / sentCont; 

				if(rtt > TIMEOUT && experimentIsRunning){
					experimentIsRunning = false;
					node.sendToSupervisor(new A3Message(MainActivity.LONG_RTT, ""), "control");
				}
				else{
					if(--dataToWaitFor <= 0){
						resetDataToWait();
						new Timer(this, 0, (int) (Math.random() * MAX_INTERNAL)).start();
					}
				}
				
				if(sentCont % 100 == 0)
					showOnScreen(sentCont + " mex spediti.");
				break;

			case MainActivity.START_EXPERIMENT:
				if(startExperiment){
					showOnScreen("Experiment has started");
					if(!experimentIsRunning && launchedGroups.containsKey("actuators")){
						startExperiment = false;
						experimentIsRunning = true;
						sentCont = 0;
						startTimestamp = StringTimeUtil.getTimestamp();
						resetDataToWait();
						sPayLoad = StringTimeUtil.createString(groupSize("actuators") * PAYLOAD_SIZE);
						channel.sendBroadcast(message);
						sendMessage();
					}
				}
				else
					startExperiment = true;
				break;
				
			case MainActivity.ADD_MEMBER:
				content = ((String)message.object).split("_");
				String type = content[0];
				int experimentId = Integer.valueOf(content[1]);
				if(launchedGroups.containsKey(type))
					if(launchedGroups.get(type).containsKey(experimentId))
						launchedGroups.get(type).put(experimentId, launchedGroups.get(type).get(experimentId) + 1);
					else
						launchedGroups.get(type).put(experimentId, 1);
				else{
					Map<Integer, Integer> newGroup = new HashMap<Integer, Integer>();
					newGroup.put(experimentId, 1);
					launchedGroups.put(type, newGroup);
				}
				break;
				
			case MainActivity.STOP_EXPERIMENT:
				break;
				
			case MainActivity.LONG_RTT:
				if(experimentIsRunning){
					showOnScreen("Experiment has stopped");
					experimentIsRunning = false;
					long runningTime = StringTimeUtil.roundTripTime(startTimestamp, StringTimeUtil.getTimestamp()) / 1000;
					float frequency = sentCont / ((float)(runningTime));
					
					node.sendToSupervisor(new A3Message(MainActivity.DATA, sentCont + "\t" +
							runningTime + "\t" + frequency + "\t" + avgRTT), "control");
				}
				break;
		}
	}
	
	private void resetDataToWait() {
		dataToWaitFor = 0;
		for(String gType : launchedGroups.keySet())
			if(gType.equals("actuators"))
				for(int i : launchedGroups.get(gType).keySet())
					dataToWaitFor += launchedGroups.get(gType).get(i);
	}

	private void sendMessage() {
		showOnScreen("Sendind command to actuators");
		if(experimentIsRunning)
			if(launchedGroups.containsKey("actuators"))
				for(int groupId : launchedGroups.get("actuators").keySet())
					channel.sendBroadcast(new A3Message(MainActivity.SERVER_PING, groupId + "#" + StringTimeUtil.getTimestamp() + "#" + sPayLoad));
	}	
	
	private int groupSize(String type){
		int size = 0;
		for(int i : launchedGroups.get(type).keySet())
			size += launchedGroups.get(type).get(i);
		return size;
	}

	@Override
	public void timerFired(int reason) {
		if(experimentIsRunning)
			sendMessage();
	}
}
