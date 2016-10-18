package it.polimi.greenhouse.a3.groups;

import it.polimi.greenhouse.a3.roles.ActuatorFollowerRole;
import it.polimi.greenhouse.a3.roles.ActuatorSupervisorRole;
import a3.a3droid.GroupDescriptor;

public class ActuatorsDescriptor extends GroupDescriptor {

	public ActuatorsDescriptor() {
		super("actuators", ActuatorSupervisorRole.class.getName(), ActuatorFollowerRole.class.getName());
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
