package it.polimi.greenhouse.a3.roles;

import org.greenrobot.eventbus.EventBus;

import it.polimi.deepse.a3droid.a3.A3FollowerRole;
import it.polimi.deepse.a3droid.a3.A3Message;
import it.polimi.deepse.a3droid.a3.exceptions.A3SupervisorNotElectedException;
import it.polimi.greenhouse.a3.events.TestEvent;
import it.polimi.greenhouse.util.AppConstants;

public class ServerFollowerRole extends A3FollowerRole {

	public ServerFollowerRole() {
		super();		
	}

	@Override
	public void onActivation() {
		try {
			node.sendToSupervisor(new A3Message(AppConstants.JOINED, getGroupName() + "_" + node.getUID() + "_" + getChannelId()), "control");
		} catch (A3SupervisorNotElectedException e) {
			e.printStackTrace();
		}

		postUIEvent(0, "[" + getGroupName() + "_FolRole]");
	}
	@Override
	public void receiveApplicationMessage(A3Message message) {
		
		String [] content;
		String experiment;
		switch(message.reason){
		case AppConstants.SENSOR_PONG:
			postUIEvent(0, "Forwarding server data to sensor");
			content = ((String)message.object).split("#");
			experiment = content[1];
			try {
				node.sendToSupervisor(message, "monitoring_" + experiment);
			} catch (A3SupervisorNotElectedException e) {
				e.printStackTrace();
			}
			break;
			
		case AppConstants.SERVER_PING:
			content = ((String)message.object).split("#");
			experiment = content[0];
			message.object = message.senderAddress + "#" + (String)message.object;
			if(node.isConnected("actuators_" + experiment)){
				postUIEvent(0, "Forwarding server data to actuators");
				try {
					node.sendToSupervisor(message, "actuators_" + experiment);
				} catch (A3SupervisorNotElectedException e) {
					e.printStackTrace();
				}
			}
			break;

		case AppConstants.START_EXPERIMENT:
			break;

		case AppConstants.STOP_EXPERIMENT_COMMAND:
			break;
			
		default:
			break;
		}
	}
}
