package it.polimi.greenhouse.a3.roles;

import java.util.Arrays;
import java.util.List;

import it.polimi.deepse.a3droid.a3.A3FollowerRole;
import it.polimi.deepse.a3droid.a3.A3Message;
import it.polimi.greenhouse.a3.nodes.TestControlNode;
import it.polimi.greenhouse.util.AppConstants;


/**
 * This class is the role the followers of "control" group play.
 * @author Francesco
 *
 */
public class TestControlFollowerRole extends A3FollowerRole {

	@Override
	public void onActivation() {
		//showOnScreen("[TestGroupCtrlFolRole]");
		sendToSupervisor(new A3Message(AppConstants.MEMBER_ADDED, ((TestControlNode) node).isServer() + ""));
	}

	@Override
	public void logic() {}

	@Override
	public void receiveApplicationMessage(A3Message a3Message) {

		switch (a3Message.reason){

			case AppConstants.TEST_GROUP_READY:
				List<String> ids = Arrays.asList(a3Message.object.split("_"));
				((TestControlNode) node).setReady(checkId(ids, getChannelId()));
				break;
			case AppConstants.MEMBER_ADDED:
				//((TestControlNode) node).addMember(a3Message.object);
				break;
			case AppConstants.MEMBER_REMOVED:
				//((TestControlNode) node).removeMember(a3Message.object);
				break;
			default:
				break;
		}
	}

	/**
	 * Check if the channel ID, which has an additional suffix, contains one of the ids in the list, which have no suffix
	 * @param ids the list of ids of the test group
	 * @param id the current channel id
	 * @return is the id in the list
	 */
	private boolean checkId(List <String> ids, String id){
		for(String cId : ids)
			if(id.indexOf(cId) != -1)
				return true;
		return false;
	}
}