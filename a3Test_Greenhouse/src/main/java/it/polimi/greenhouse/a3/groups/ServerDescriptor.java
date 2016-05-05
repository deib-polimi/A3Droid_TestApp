package it.polimi.greenhouse.a3.groups;

import it.polimi.deepse.a3droid.GroupDescriptor;
import it.polimi.greenhouse.a3.roles.ServerFollowerRole;
import it.polimi.greenhouse.a3.roles.ServerSupervisorRole;

public class ServerDescriptor extends GroupDescriptor {

	public ServerDescriptor() {
		super("server", ServerSupervisorRole.class.getName(), ServerFollowerRole.class.getName());
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
