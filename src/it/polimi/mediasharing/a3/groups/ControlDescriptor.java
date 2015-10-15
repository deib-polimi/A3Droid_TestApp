package it.polimi.mediasharing.a3.groups;

import it.polimi.mediasharing.a3.roles.ControlFollowerRole;
import it.polimi.mediasharing.a3.roles.ControlSupervisorRole;
import a3.a3droid.GroupDescriptor;

/**
 * This class is the descriptor of "control" group.
 * Its roles are "ControlSupervisorRole" and "ControlFollowerRole".
 * @author Francesco
 *
 */
public class ControlDescriptor extends GroupDescriptor{

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
