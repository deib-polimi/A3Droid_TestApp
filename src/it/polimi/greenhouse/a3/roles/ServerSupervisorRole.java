package it.polimi.greenhouse.a3.roles;

import it.polimi.greenhouse.activities.MainActivity;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import a3.a3droid.A3Message;
import a3.a3droid.A3SupervisorRole;
import a3.a3droid.Timer;
import a3.a3droid.TimerInterface;
import android.annotation.SuppressLint;

public class ServerSupervisorRole extends A3SupervisorRole implements TimerInterface{

	//private int currentExperiment;
	private int currentExperiment;
	private boolean startExperiment;
	private boolean experimentIsRunning;
	private int sentCont = 0;
	private int dataToWaitFor = 0;
	private String startTimestamp;
	private String s;
	private long rttThreshold;
	private final static long MAX_INTERNAL = 10 * 1000;
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
		launchedGroups = new HashMap<String, Map<Integer, Integer>>();
		initializeExperiment();
	}	
	
	private void initializeExperiment() {
		char[] c;
		
		switch(4){
		case 1:	c = new char[62484]; break;
		case 2:	c = new char[32000]; break;
		case 3:	c = new char[5017]; break;
		case 4:	c = new char[1812]; break;
		default: c = null;
		}
		
		for(int i = 0; i < c.length; i++)
			c[i] = '0';
		
		s = new String(c);
		rttThreshold = Long.parseLong("1000000000") * 10;
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
				// TODO Auto-generated method stub
				sentCont ++;
				
				rtt = roundTripTime(((String)message.object), getTimestamp());

				if(rtt > rttThreshold && experimentIsRunning){
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
					startExperiment = false;
					experimentIsRunning = true;
					channel.sendBroadcast(message);
					sentCont = 0;
					startTimestamp = getTimestamp();
					resetDataToWait();
					sendMessage();
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
				experimentIsRunning = false;
				long runningTime = roundTripTime(startTimestamp, getTimestamp());
				float frequency = sentCont / ((float)(runningTime / 1000));
				
				node.sendToSupervisor(new A3Message(MainActivity.DATA, sentCont + " " +
						(runningTime/ 1000) + " " + frequency), "control");
				break;
		}
	}
	
	private void resetDataToWait() {
		dataToWaitFor = 0;
		for(String gType : launchedGroups.keySet())
			for(int i : launchedGroups.get(gType).keySet())
				dataToWaitFor += launchedGroups.get(gType).get(i);
	}

	private void sendMessage() {
		showOnScreen("Sendind command to actuators");
		if(experimentIsRunning)
			channel.sendBroadcast(new A3Message(MainActivity.SERVER_PING, currentExperiment + "#" + getTimestamp() + "#" + s));
	}

	private String getTimestamp() {
		try{
			return new Date().getTime() + "";
		}catch(Exception e){showOnScreen("getTimestamp(): " + e.getLocalizedMessage());}
		return "0";
	}
	
	private long roundTripTime(String departureTimestamp, String arrivalTimestamp) {
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
		if(experimentIsRunning)
			sendMessage();
	}
}
