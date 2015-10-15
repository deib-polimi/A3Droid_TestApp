package it.polimi.mediasharing.a3.roles;

import it.polimi.mediasharing.activities.MainActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import a3.a3droid.A3FollowerRole;
import a3.a3droid.A3Message;
import a3.a3droid.Timer;
import a3.a3droid.TimerInterface;
import android.os.Environment;

public class ExperimentFollowerRole extends A3FollowerRole implements TimerInterface{

	private int currentExperiment;
	private long rttThreshold;
	
	private String s;
	private boolean experimentIsRunning;
	private int sentCont;
	private String startTimestamp;
	
	public ExperimentFollowerRole() {
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
			
		case MainActivity.MEDIA_DATA_SHARE:
			
			String response [] = ((String)message.object).split("#");
			
			// TODO Auto-generated method stub
			sentCont ++;
			
			rtt = roundTripTime(response[0], getTimestamp());

			if(rtt > rttThreshold && experimentIsRunning){
				experimentIsRunning = false;
				node.sendToSupervisor(new A3Message(MainActivity.LONG_RTT, ""), "control");
			}
			
			if(sentCont % 100 == 0)
				showOnScreen(sentCont + " mex spediti.");
			
			try {
				OutputStream out;
	        	File file = new File(Environment.getExternalStorageDirectory() + "/a3droid/image.jpg");
	            if (!file.exists()) {
					file.createNewFile();
				}
	            out = new FileOutputStream(file);	            	            
	            String[] byteValues = response[1].substring(1, response[1].length() - 1).split(",");
	            byte[] bytes = new byte[byteValues.length];
	            for (int i=0, len=bytes.length; i<len; i++) {
	            	bytes[i] = Byte.parseByte(byteValues[i].trim());     
	            }
	            out.write(bytes);
	            out.close();
	          
			} catch (NumberFormatException nfe){
				nfe.printStackTrace();
	        } catch (FileNotFoundException ex) {
	            System.out.println("File not found. ");
	        } catch (IOException e) {
				e.printStackTrace();
			}
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
