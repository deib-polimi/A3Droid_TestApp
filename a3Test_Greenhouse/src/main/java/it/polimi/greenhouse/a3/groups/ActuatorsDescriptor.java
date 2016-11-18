package it.polimi.greenhouse.a3.groups;


import it.polimi.deepse.a3droid.a3.A3GroupDescriptor;
import it.polimi.greenhouse.a3.roles.ActuatorFollowerRole;
import it.polimi.greenhouse.a3.roles.ActuatorSupervisorRole;

public class ActuatorsDescriptor extends A3GroupDescriptor {

	public ActuatorsDescriptor() {
		super("actuators", ActuatorSupervisorRole.class.getName(), ActuatorFollowerRole.class.getName());
		// TODO Auto-generated constructor stub
	}

	@Override
	public int getSupervisorFitnessFunction() {
		// TODO Auto-generated method stub
		return 0;
	}
}
