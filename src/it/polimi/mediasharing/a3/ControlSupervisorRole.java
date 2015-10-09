package it.polimi.mediasharing.a3;

import it.polimi.mediasharing.activities.MainActivity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import a3.a3droid.A3Message;
import a3.a3droid.A3SupervisorRole;
import android.os.Environment;

/**
 * This class is the role the supervisor of "control" group plays.
 * @author Francesco
 *
 */
public class ControlSupervisorRole extends A3SupervisorRole {

	private int runningExperiment;
	private ArrayList<String> vmIds;
	private int launchedGroups;
	private int totalGroups;

	private File sd;
	private File f;
	private FileWriter fw;
	private BufferedWriter bw;
	private int dataToWaitFor;
	private String result;
	private int numberOfTrials;
	
	public ControlSupervisorRole(){
		super();
	}
	
	@Override
	public void onActivation() {
		// TODO Auto-generated method stub
		
		runningExperiment = 0;
		vmIds = new ArrayList<String>();
		
		//I'm not connected to "experiment" group already.
		launchedGroups = 1;
		
		numberOfTrials = 1;
	}	

	@Override
	public void logic() {
		// TODO Auto-generated method stub
		showOnScreen("[CtrlSupRole]");
		node.sendToSupervisor(new A3Message(MainActivity.NEW_PHONE, ""), "control");		
		active = false;
	}

	@Override
	public void receiveApplicationMessage(A3Message message) {
		// TODO Auto-generated method stub
		
		switch(message.reason){
			
		case MainActivity.NEW_PHONE:
			
			vmIds.add(message.senderAddress);
			numberOfTrials = 1;
			showOnScreen("Telefoni connessi: " + vmIds.size());
			break;
			
		case MainActivity.LONG_RTT:
						
			sd = Environment.getExternalStorageDirectory();
			f = new File(sd, MainActivity.EXPERIMENT_PREFIX + runningExperiment + "_" + vmIds.size() +
					"_" + totalGroups + ".txt");

			try {
				fw = new FileWriter(f, true);
				bw = new BufferedWriter(fw);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				showOnScreen(e.getLocalizedMessage());
			}
			
			for(int i = 1; i < launchedGroups; i ++)
				node.sendToSupervisor(message,
						MainActivity.EXPERIMENT_PREFIX + runningExperiment + "_" + i);
			break;
			
		case MainActivity.DATA:
						
			result = result + ((String)message.object).replace(".", ",") + "\n";
			dataToWaitFor --;
			
			if(dataToWaitFor == 0){
				
				try {
					bw.write(result);
					bw.flush();
				} catch (IOException e) {showOnScreen("ECCEZIONE IN CtrlSupRole [bw.flush()]: " + e.getLocalizedMessage());}
				
				showOnScreen("--- TENTATIVO " + numberOfTrials + " TERMINATO ---");
				numberOfTrials ++;
			}
			
			break;
			
		case MainActivity.CREATE_GROUP_USER_COMMAND:
			//String[] splittedObject = message.object.split("_");
			
			int temp = Integer.valueOf((String)message.object /*splittedObject[0]*/);
			//totalGroups = Integer.valueOf(splittedObject[1]);
			
			if(runningExperiment != temp){
				runningExperiment = temp;
				launchedGroups = 1;
				//numberOfTrials = Test3Activity.NUMBER_OF_EXPERIMENTS;
			}
			
			channel.sendBroadcast(new A3Message(MainActivity.CREATE_GROUP, runningExperiment + "_" + launchedGroups));
			break;
			
		case MainActivity.CREATE_GROUP:
			node.connect(MainActivity.EXPERIMENT_PREFIX + message.object, true, true);
			launchedGroups ++;
			break;
			
		case MainActivity.START_EXPERIMENT_USER_COMMAND:
			
			showOnScreen("--- TENTATIVO " + numberOfTrials + "---");
			
			result = "";
			
			totalGroups = launchedGroups - 1;
			dataToWaitFor = totalGroups * (vmIds.size() - 1);
			for(int i = 1; i < launchedGroups; i ++)
				node.sendToSupervisor(new A3Message(MainActivity.START_EXPERIMENT, ""),
						MainActivity.EXPERIMENT_PREFIX + runningExperiment + "_" + i);
			break;
			
		case MainActivity.STOP_EXPERIMENT:
			
			showOnScreen("--- STOP_EXPERIMENT: ATTENDERE 10s CIRCA ---");
			
			for(int i = 1; i < launchedGroups; i ++)
				node.sendToSupervisor(new A3Message(MainActivity.STOP_EXPERIMENT, ""),
						MainActivity.EXPERIMENT_PREFIX + runningExperiment + "_" + i);
			
			for(int i = 1; i <= launchedGroups; i ++){
				if(!node.isSupervisor(MainActivity.EXPERIMENT_PREFIX + runningExperiment + "_" + i))
					node.disconnect(MainActivity.EXPERIMENT_PREFIX + runningExperiment + "_" + i, true);
			}
				
			synchronized(this){
				try {
					wait(10000);
				} catch (InterruptedException e) {}
			}

			for(int i = 1; i <= launchedGroups; i ++)
				node.disconnect(MainActivity.EXPERIMENT_PREFIX + runningExperiment + "_" + i, true);
			
			showOnScreen("--- ESPERIMENTO TERMINATO ---");
			launchedGroups = 1;
			break;
		}	
	}
}
