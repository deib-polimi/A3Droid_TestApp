package it.polimi.greenhouse.a3.roles;

import it.polimi.greenhouse.activities.MainActivity;

import java.util.Date;

import a3.a3droid.A3Message;
import a3.a3droid.A3SupervisorRole;
import a3.a3droid.Timer;
import a3.a3droid.TimerInterface;

public class ServerSupervisorRole extends A3SupervisorRole implements TimerInterface{

	//private int currentExperiment;
	private int currentExperiment;
	private boolean startExperiment;
	private boolean experimentIsRunning;
	private int sentCont;
	private String startTimestamp;
	private String s;
	private long rttThreshold;
	private final static long MAX_INTERNAL = 30 * 1000;
	
	public ServerSupervisorRole() {
		super();		
	}

	@Override
	public void onActivation() {
		startExperiment = true;
		experimentIsRunning = false;
		sentCont = 0;
		currentExperiment = Integer.valueOf(getGroupName().split("_")[1]);
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
					new Timer(this, 0, (int) (Math.random() * MAX_INTERNAL)).start();
				}
				
				if(sentCont % 100 == 0)
					showOnScreen(sentCont + " mex spediti.");
				
				break;

			case MainActivity.START_EXPERIMENT:
				if(startExperiment){
					channel.sendBroadcast(message);
					startExperiment = false;
					experimentIsRunning = true;
					sentCont = 0;
					startTimestamp = getTimestamp();
					sendMessage();
				}
				else
					startExperiment = true;
				
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
	
	private void sendMessage() {
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
		sendMessage();
	}
}
