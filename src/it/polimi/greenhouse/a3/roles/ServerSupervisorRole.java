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

	private int currentExperiment;
	private boolean startExperiment;
	private boolean experimentIsRunning;
	private int sentCont = 0;
	private int dataToWaitFor = 0;
	private String startTimestamp;
	private String sPayLoad;
	private final static long MAX_INTERNAL = 30 * 1000;
	private final static long TIMEOUT = 60 * 1000;
	private Map<String, Map<Integer, Integer>> launchedGroups;
	
	public ServerSupervisorRole() {
		super();		
	}

	@Override
	public void onActivation() {
		startExperiment = true;
		experimentIsRunning = false;
		sentCont = 0;
		currentExperiment = Integer.valueOf(getGroupName().split("_")[1]);
		launchedGroups = new ConcurrentHashMap<String, Map<Integer, Integer>>();
		sPayLoad = StringTimeUtil.createString(4);
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
				sentCont ++;
				
				rtt = StringTimeUtil.roundTripTime(((String)message.object), StringTimeUtil.getTimestamp());

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
					if(!experimentIsRunning && launchedGroups.containsKey("actuators")){
						startExperiment = false;
						experimentIsRunning = true;
						channel.sendBroadcast(message);
						sentCont = 0;
						startTimestamp = StringTimeUtil.getTimestamp();
						resetDataToWait();
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
					launchedGroups.get(type).put(experimentId, launchedGroups.get(type).get(experimentId) + 1);
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
					experimentIsRunning = false;
					long runningTime = StringTimeUtil.roundTripTime(startTimestamp, StringTimeUtil.getTimestamp());
					float frequency = sentCont / ((float)(runningTime / 1000));
					
					node.sendToSupervisor(new A3Message(MainActivity.DATA, sentCont + " " +
							(runningTime/ 1000) + " " + frequency), "control");
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
			channel.sendBroadcast(new A3Message(MainActivity.SERVER_PING, currentExperiment + "#" + StringTimeUtil.getTimestamp() + "#" + sPayLoad));
	}	

	@Override
	public void timerFired(int reason) {
		if(experimentIsRunning)
			sendMessage();
	}
}
