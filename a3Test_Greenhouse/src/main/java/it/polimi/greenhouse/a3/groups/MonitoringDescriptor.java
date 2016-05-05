package it.polimi.greenhouse.a3.groups;

import it.polimi.deepse.a3droid.GroupDescriptor;
import it.polimi.greenhouse.a3.roles.SensorFollowerRole;
import it.polimi.greenhouse.a3.roles.SensorSupervisorRole;

public abstract class MonitoringDescriptor extends GroupDescriptor {

	public MonitoringDescriptor() {
		super("monitoring", SensorSupervisorRole.class.getName(), SensorFollowerRole.class.getName());
		// TODO Auto-generated constructor stub
	}

	/*@Override
	public int getSupervisorFitnessFunction() {
		
		
		return 0;
	}*/
	
	/*@Override
	public int getSupervisorFitnessFunction() {
		// TODO Auto-generated method stub
		return 0;
	}*/

}
