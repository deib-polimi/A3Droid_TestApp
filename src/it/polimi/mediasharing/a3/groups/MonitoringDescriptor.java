package it.polimi.mediasharing.a3.groups;

import it.polimi.mediasharing.a3.roles.SensorFollowerRole;
import it.polimi.mediasharing.a3.roles.SensorSupervisorRole;
import a3.a3droid.GroupDescriptor;

public class MonitoringDescriptor extends GroupDescriptor {

	public MonitoringDescriptor() {
		super("A3Test3", SensorSupervisorRole.class.getName(), SensorFollowerRole.class.getName());
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
