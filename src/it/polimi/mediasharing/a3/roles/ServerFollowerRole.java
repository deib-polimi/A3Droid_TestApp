package it.polimi.mediasharing.a3.roles;

import it.polimi.mediasharing.activities.MainActivity;

import java.util.Date;

import a3.a3droid.A3FollowerRole;
import a3.a3droid.A3Message;
import a3.a3droid.Timer;
import a3.a3droid.TimerInterface;

public class ServerFollowerRole extends A3FollowerRole implements TimerInterface{

	private int currentExperiment;
	private long rttThreshold;
	
	private String s;
	private boolean experimentIsRunning;
	private int sentCont;
	private String startTimestamp;
	
	public ServerFollowerRole() {
		// TODO Auto-generated constructor stub
		super();		
	}

	@Override
	public void onActivation() {
		// TODO Auto-generated method stub
		
		currentExperiment = Integer.valueOf(getGroupName().split("_")[1]);
		
		experimentIsRunning = false;
		sentCont = 0;
		initializeExperiment();
	}

	private void initializeExperiment() {
		// TODO Auto-generated method stub
		
		char[] c;
		
		switch(currentExperiment){
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
		// TODO Auto-generated method stub
		showOnScreen("[" + getGroupName() + "_FolRole]");
		active = false;
	}

	@Override
	public void receiveApplicationMessage(A3Message message) {

		long rtt;
		switch(message.reason){
		case MainActivity.PONG:
			// TODO Auto-generated method stub
			sentCont ++;
			
			rtt = roundTripTime(((String)message.object).split(" ")[0], getTimestamp());

			if(rtt > rttThreshold && experimentIsRunning){
				experimentIsRunning = false;
				node.sendToSupervisor(new A3Message(MainActivity.LONG_RTT, ""), "control");
			}
			else{
				new Timer(this, 0, (int) (Math.random() * 100)).start();
			}
			
			if(sentCont % 100 == 0)
				showOnScreen(sentCont + " mex spediti.");
			
			break;

		case MainActivity.START_EXPERIMENT:

			startTimestamp = getTimestamp();
			sentCont = 0;

			experimentIsRunning = true;
			//sendMessage();
			break;

		case MainActivity.STOP_EXPERIMENT_COMMAND:
			
			experimentIsRunning = false;
			long runningTime = roundTripTime(startTimestamp, getTimestamp());
			float frequency = sentCont / ((float)(runningTime / 1000));
			
			node.sendToSupervisor(new A3Message(MainActivity.DATA, sentCont + " " +
					(runningTime/ 1000) + " " + frequency), "control");
			break;
		}
	}

	private void sendMessage() {
		// TODO Auto-generated method stub
		if(experimentIsRunning)
			channel.sendToSupervisor(new A3Message(MainActivity.PING, getTimestamp() + " " + s));
	}

	private String getTimestamp() {
		try{
		// TODO Auto-generated method stub
			return new Date().getTime() + "";
		}catch(Exception e){showOnScreen("getTimestamp(): " + e.getLocalizedMessage());}
		return "0";
	}
	
	private long roundTripTime(String departureTimestamp, String arrivalTimestamp) {
		// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub
		sendMessage();
	}
}
