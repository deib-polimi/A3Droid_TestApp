package it.polimi.mediasharing.a3.groups;

import it.polimi.mediasharing.a3.roles.ServerFollowerRole;
import it.polimi.mediasharing.a3.roles.ServerSupervisorRole;
import a3.a3droid.GroupDescriptor;

public class ServerDescriptor extends GroupDescriptor {

	public ServerDescriptor() {
		super("A3Test3", ServerSupervisorRole.class.getName(), ServerFollowerRole.class.getName());
		// TODO Auto-generated constructor stub
	}

	@Override
	public int getSupervisorFitnessFunction() {
		// TODO Auto-generated method stub
		return 0;
	}

	/*@Override
	public int getSupervisorFitnessFunction() {
		// TODO Auto-generated method stub
		return 0;
	}*/

}
