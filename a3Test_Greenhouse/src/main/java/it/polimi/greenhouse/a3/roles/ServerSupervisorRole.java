package it.polimi.greenhouse.a3.roles;

import it.polimi.deepse.a3droid.A3Message;
import it.polimi.deepse.a3droid.Timer;
import it.polimi.deepse.a3droid.TimerInterface;
import it.polimi.greenhouse.util.AppConstants;
import it.polimi.greenhouse.util.StringTimeUtil;

public class ServerSupervisorRole extends SupervisorRole implements TimerInterface {

	private boolean startExperiment;
	private boolean experimentIsRunning;
	private boolean paramsSet = false;
	private int sentCont;
	private double avgRTT;
	private volatile int dataToWaitFor;
	private String startTimestamp;
	private byte sPayLoad [];
	private final static long TIMEOUT = 60 * 1000;
	private long MAX_INTERNAL = 10 * 1000;
	private int PAYLOAD_SIZE = 32;
	
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
		node.sendToSupervisor(new A3Message(AppConstants.JOINED, getGroupName() + "_" + node.getUUID() + "_" + channel.getChannelId()), "control");
	}	
	
	@Override
	public void logic() {
		showOnScreen("[" + getGroupName() + "_SupRole]");
		active = false;
	}

	@Override
	public void receiveApplicationMessage(A3Message message) {
		
		double rtt;
		switch(message.reason){
		
			case AppConstants.ADD_MEMBER:
				String [] content = ((String)message.object).split("_");
				String type = content[0];
				int experimentId = Integer.valueOf(content[1]);
				String uuid = content[2];
				String name = content[3];
				if(!type.equals("server"))
					removeGroupMember(uuid);
				addGroupMember(type, experimentId, uuid, name);
				break;
				
			case AppConstants.MEMBER_ADDED:
				resetCount();
				break;
				
			case AppConstants.MEMBER_REMOVED:				
				removeGroupMember(retrieveGroupMemberUuid(message.object));
				resetCount();
				break;
				
			case AppConstants.SET_PARAMS:
				if(!paramsSet){
					String params [] = message.object.split("_");
					if(!params[0].equals("A"))
						break;
					paramsSet = true;
					long freq = Long.valueOf(params[1]);
					this.MAX_INTERNAL = 60 * 1000 / freq;
					this.PAYLOAD_SIZE = Integer.valueOf(params[2]);
					showOnScreen("Params set to: " + freq + " Mes/min and " + PAYLOAD_SIZE + " Bytes");
				}
				break;
			
			case AppConstants.SENSOR_PING:
				showOnScreen("Received new data from a sensor");
				message.reason = AppConstants.SENSOR_PONG;
				content = ((String)message.object).split("#");
				String sensorAddress = content[0];
				String experiment = content[1];
				String sendTime = content[2];
				message.object = sensorAddress + "#" + experiment + "#" + sendTime;
				channel.sendUnicast(message, message.senderAddress);
				break;
				
				
			case AppConstants.SERVER_PONG:
				showOnScreen("Actuator response received");
				sentCont ++;
				rtt = StringTimeUtil.roundTripTime(((String)message.object), StringTimeUtil.getTimestamp()) / 1000;
				avgRTT = (avgRTT * (sentCont - 1) + rtt) / sentCont; 

				if(rtt > TIMEOUT && experimentIsRunning){
					experimentIsRunning = false;
					node.sendToSupervisor(new A3Message(AppConstants.LONG_RTT, ""), "control");
				}
				else{
					if(--dataToWaitFor <= 0){
						resetCount();
						new Timer(this, 0, (int) (Math.random() * MAX_INTERNAL)).start();
					}
				}
				
				if(sentCont % 100 == 0)
					showOnScreen(sentCont + " mex spediti.");
				break;

			case AppConstants.START_EXPERIMENT:
				if(startExperiment){
					if(!experimentIsRunning && launchedGroups.containsKey("actuators") && !launchedGroups.get("actuators").isEmpty()){
						showOnScreen("Experiment has started");
						startExperiment = false;
						experimentIsRunning = true;
						sentCont = 0;
						startTimestamp = StringTimeUtil.getTimestamp();
						resetCount();
						sPayLoad = StringTimeUtil.createPayload(groupSize("actuators") * PAYLOAD_SIZE);
						channel.sendBroadcast(message);
						sendMessage();
					}
				}
				else
					startExperiment = true;
				break;
				
				
			case AppConstants.STOP_EXPERIMENT:
				break;
				
			case AppConstants.LONG_RTT:
				if(experimentIsRunning){
					showOnScreen("Experiment has stopped");
					experimentIsRunning = false;
					paramsSet = false;
					double runningTime = StringTimeUtil.roundTripTime(startTimestamp, StringTimeUtil.getTimestamp()) / 1000;
					float frequency = sentCont / ((float)(runningTime));
					
					node.sendToSupervisor(new A3Message(AppConstants.DATA, "StoA: " + sentCont + "\t" +
							runningTime + "\t" + frequency + "\t" + avgRTT), "control");
				}
				break;
				
			default:
				break;
		}
	}
	
	
	private void resetCount() {
		dataToWaitFor = 0;
		for(String gType : launchedGroups.keySet())
			if(gType.equals("actuators"))
				for(int i : launchedGroups.get(gType).keySet())
					dataToWaitFor += launchedGroups.get(gType).get(i).size();
	}

	private void sendMessage() {
		showOnScreen("Sendind command to actuators");
		if(experimentIsRunning)
			if(launchedGroups.containsKey("actuators"))
				for(int groupId : launchedGroups.get("actuators").keySet())
					channel.sendBroadcast(new A3Message(AppConstants.SERVER_PING, groupId + "#" + StringTimeUtil.getTimestamp(), sPayLoad));
	}	
	
	@Override
	public void memberAdded(String name) {
		showOnScreen("Entered: " + name);
		A3Message msg = new A3Message(AppConstants.MEMBER_ADDED, name);
		channel.sendBroadcast(msg);
		node.sendToSupervisor(msg, "control");
	}

	@Override
	public void memberRemoved(String name) {	
		showOnScreen("Exited: " + name);
		A3Message msg = new A3Message(AppConstants.MEMBER_REMOVED, name);
		channel.sendBroadcast(msg);
		node.sendToSupervisor(msg, "control");
	}

	@Override
	public void timerFired(int reason) {
		if(experimentIsRunning)
			sendMessage();
	}
}
