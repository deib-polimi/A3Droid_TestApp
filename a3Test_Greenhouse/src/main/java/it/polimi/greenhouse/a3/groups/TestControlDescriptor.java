package it.polimi.greenhouse.a3.groups;

import it.polimi.deepse.a3droid.a3.A3GroupDescriptor;
import it.polimi.greenhouse.a3.roles.TestControlFollowerRole;
import it.polimi.greenhouse.a3.roles.TestControlSupervisorRole;

/**
 * This class is the descriptor of "control" group.
 * Its roles are "ControlSupervisorRole" and "ControlFollowerRole".
 * @author Francesco
 *
 */
public class TestControlDescriptor extends A3GroupDescriptor {

	public final static String TEST_GROUP_NAME = "test_control";

	public TestControlDescriptor(){
		super(TEST_GROUP_NAME, TestControlSupervisorRole.class.getName(), TestControlFollowerRole.class.getName());
	}

	@Override
	public int getSupervisorFitnessFunction() {
		return 0;
	}

	@Override
	public void groupStateChangeListener(A3GroupState a3GroupState, A3GroupState a3GroupState1) {

	}

	/*@Override
	public int getSupervisorFitnessFunction() {
		// TODO Auto-generated method stub
		return 0;
	}*/
}
