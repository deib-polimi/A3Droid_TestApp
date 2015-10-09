package it.polimi.mediasharing.a3;

import it.polimi.mediasharing.activities.MainActivity;
import a3.a3droid.GroupDescriptor;

/**
 * This class is the descriptor of "control" group.
 * Its roles are "ControlSupervisorRole" and "ControlFollowerRole".
 * @author Francesco
 *
 */
public class ControlDescriptor extends GroupDescriptor{

	public ControlDescriptor(){
		super("control", MainActivity.PACKAGE_NAME + ".ControlSupervisorRole", MainActivity.PACKAGE_NAME + ".ControlFollowerRole");
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
