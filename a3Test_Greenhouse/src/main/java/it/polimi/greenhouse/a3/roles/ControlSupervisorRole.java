package it.polimi.greenhouse.a3.roles;

import android.os.Environment;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import it.polimi.deepse.a3droid.a3.A3Message;
import it.polimi.deepse.a3droid.a3.events.A3GroupEvent;
import it.polimi.deepse.a3droid.a3.exceptions.A3ChannelNotFoundException;
import it.polimi.deepse.a3droid.a3.exceptions.A3SupervisorNotElectedException;
import it.polimi.greenhouse.a3.events.TestEvent;
import it.polimi.greenhouse.activities.MainActivity;
import it.polimi.greenhouse.util.AppConstants;

/**
 * This class is the role the supervisor of "control" group plays.
 * @author Francesco
 *
 */
public class ControlSupervisorRole extends SupervisorRole {

	private File sd;
	private File f;
	private FileWriter fw;
	private BufferedWriter bw;
	private volatile int dataToWaitFor;
	private volatile String result;
	private int numberOfTrials;
	private boolean experimentIsRunning;

	private Set<String> vmIds;

	public ControlSupervisorRole(){
		super();
	}

	@Override
	public void onActivation() {
		vmIds = Collections.synchronizedSet(new HashSet<String>());
		dataToWaitFor = 0;
		numberOfTrials = 1;
		postUIEvent(0, "[CtrlSupRole]");
		try {
			node.sendToSupervisor(new A3Message(AppConstants.NEW_PHONE, node.getUID()), "control");
		} catch (A3SupervisorNotElectedException e) {
			e.printStackTrace();
		}
	}

	public void receiveApplicationMessage(A3Message message) {

		try {
			switch(message.reason){

				case AppConstants.JOINED:
					message.reason = AppConstants.ADD_MEMBER;
					message.object += "_" + message.senderAddress;
					node.sendBroadcast(message, "control");
					sendToConnectedSupervisors(message);
					break;

				case AppConstants.ADD_MEMBER:
					addMember(message);
					break;

				case AppConstants.MEMBER_ADDED:
					resetCount();
					break;

				case AppConstants.MEMBER_REMOVED:
					String uuid = retrieveGroupMemberUuid(message.object);
					removeGroupMember(uuid);
					resetCount();
					break;

				case AppConstants.NEW_PHONE:
					vmIds.add(message.object);
					numberOfTrials = 1;
					//showOnScreen("Telefoni connessi: " + vmIds.size());
					break;

				case AppConstants.SET_PARAMS_COMMAND:
					message.reason = AppConstants.SET_PARAMS;
					node.sendBroadcast(message, "control");
					sendToConnectedSupervisors(message);
					break;

				case AppConstants.CREATE_GROUP_USER_COMMAND:
					message.reason = AppConstants.CREATE_GROUP;
					sendBroadcast(message);
					sendToConnectedSupervisors(message);
					break;

				case AppConstants.CREATE_GROUP:
					break;

				case AppConstants.START_EXPERIMENT_USER_COMMAND:

					startExperiment(message);
					break;

				case AppConstants.LONG_RTT:

					stopExperiment(message);
					break;

				case AppConstants.DATA:

					receiveData(message);
					break;

				case AppConstants.STOP_EXPERIMENT:
					stopExperimentOld(message);
					break;

				case AppConstants.NEW_SUPERVISOR_ELECTED:
					String groupName = message.object;
					EventBus.getDefault().post(new TestEvent(AppConstants.NEW_SUPERVISOR_ELECTED, groupName));
					break;

				default:
					break;
			}
		} catch (A3ChannelNotFoundException e) {
			e.printStackTrace();
		}
	}

    private void addMember(A3Message message){
        String [] content = ((String)message.object).split("_");
        String type = content[0];
        int experimentId = Integer.valueOf(content[1]);
        String uuid = content[2];
        String name = content[3];
        if(!type.equals("server_0"))
            removeGroupMember(uuid);
        addGroupMember(type, experimentId, uuid, name);
        if(experimentIsRunning){
            message.reason = AppConstants.START_EXPERIMENT;
            message.object = "";
            sendBroadcast(message);
            sendToConnectedSupervisors(message);
        }
    }

	private void startExperiment(A3Message message){
		if(experimentIsRunning)
			return;

		postUIEvent(0, "--- Start of Expriment " + numberOfTrials + "---");

		experimentIsRunning = true;
		result = "";
		resetCount();

		message.reason = AppConstants.START_EXPERIMENT;
		sendBroadcast(message);
		sendToConnectedSupervisors(message);
	}

	private void stopExperiment(A3Message message){
		if(message.object.equals(getChannelId()) || !experimentIsRunning)
			return;

		experimentIsRunning = false;
		sd = Environment.getExternalStorageDirectory();
		f = new File(sd, AppConstants.EXPERIMENT_PREFIX + "Greenhouse_" + vmIds.size() + ".csv");

		try {
			fw = new FileWriter(f, true);
			bw = new BufferedWriter(fw);
		} catch (IOException e) {
			postUIEvent(0,"cannot write the log file "+e.getLocalizedMessage());
		}
		message.object = getChannelId();
		sendBroadcast(message);
		sendToConnectedSupervisors(message);
	}

    @Deprecated
    /**
     * This method is never actually called
     * TODO: To check if its behaviour must not go into the stopExperiment method
     */
    private void stopExperimentOld(A3Message message) throws A3ChannelNotFoundException {
        //showOnScreen("--- STOP_EXPERIMENT: ATTENDERE 10s CIRCA ---");

        sendBroadcast(message);
        for(String gType : launchedGroups.keySet())
            for(int i : launchedGroups.get(gType).keySet())
                if(node.isConnected(gType + "_" + i) && !node.isSupervisor(gType + "_" + i))
                    node.disconnect(gType + "_" + i);

        synchronized(this){
            try {
                wait(10000);
            } catch (InterruptedException e) {}
        }

        for(String gType : launchedGroups.keySet())
            for(int i : launchedGroups.get(gType).keySet())
                if(node.isConnected(gType + "_" + i))
                    node.disconnect(gType + "_" + i);

        launchedGroups.clear();
        //showOnScreen("--- ESPERIMENTO TERMINATO ---");
    }

    private void receiveData(A3Message message){
        if(dataToWaitFor == 0)
            Log.i(MainActivity.TAG, "###EXPERIMENT RESULTS###");
        Log.i(MainActivity.TAG, ((String)message.object).replace(".", ",") + "\n");

        result = result + ((String)message.object).replace(".", ",") + "\n";
        dataToWaitFor --;

        if(dataToWaitFor <= 0){
			try {
			    bw.write(result);
				bw.flush();
			} catch (IOException e) {
				postUIEvent(0, "ECCEZIONE IN CtrlSupRole [bw.flush()]: " + e.getLocalizedMessage());
			}

			postUIEvent(0,"--- End of Expriment with  " + numberOfTrials + " Trials  ---");
			numberOfTrials ++;
        }
    }
	
	@Override
	protected void removeGroupMember(String uuid) {
		super.removeGroupMember(uuid);
	}
	
	private void resetCount(){
		dataToWaitFor = 0;
		for(String gType : launchedGroups.keySet())
			if(gType.equals("monitoring"))
				for(int i : launchedGroups.get(gType).keySet())
					dataToWaitFor += launchedGroups.get(gType).get(i).size();
		
		if(launchedGroups.containsKey("actuators") && !launchedGroups.get("actuators").isEmpty())
			dataToWaitFor++;
	}
	
	private void sendToConnectedSupervisors(A3Message message){
		for(String gType : launchedGroups.keySet())
			for(int i : launchedGroups.get(gType).keySet())
				if(node.isConnected(gType + "_" + i) && node.isSupervisor(gType + "_" + i))
					try {
						node.sendToSupervisor(message,
                            gType + "_" + i);
					} catch (A3SupervisorNotElectedException e) {
						e.printStackTrace();
					}
	}
}
