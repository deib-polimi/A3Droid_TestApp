package it.polimi.greenhouse.a3.groups;

import it.polimi.deepse.a3droid.GroupDescriptor;
import it.polimi.greenhouse.a3.roles.ControlFollowerRole;
import it.polimi.greenhouse.a3.roles.ControlSupervisorRole;
import it.polimi.greenhouse.a3.roles.TestControlFollowerRole;
import it.polimi.greenhouse.a3.roles.TestControlSupervisorRole;

/**
 * This class is the descriptor of "control" group.
 * Its roles are "ControlSupervisorRole" and "ControlFollowerRole".
 * @author Francesco
 *
 */
public class TestControlDescriptor extends GroupDescriptor {

	public TestControlDescriptor(){
		super("test_control", TestControlSupervisorRole.class.getName(), TestControlFollowerRole.class.getName());
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
