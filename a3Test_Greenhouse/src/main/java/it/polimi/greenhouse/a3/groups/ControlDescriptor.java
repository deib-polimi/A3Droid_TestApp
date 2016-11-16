package it.polimi.greenhouse.a3.groups;

import it.polimi.deepse.a3droid.a3.A3GroupDescriptor;
import it.polimi.greenhouse.a3.roles.ControlFollowerRole;
import it.polimi.greenhouse.a3.roles.ControlSupervisorRole;

/**
 * This class is the descriptor of "control" group.
 * Its roles are "ControlSupervisorRole" and "ControlFollowerRole".
 * @author Francesco
 *
 */
public class ControlDescriptor extends A3GroupDescriptor {

	public ControlDescriptor(){
		super("control", ControlSupervisorRole.class.getName(), ControlFollowerRole.class.getName());
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
