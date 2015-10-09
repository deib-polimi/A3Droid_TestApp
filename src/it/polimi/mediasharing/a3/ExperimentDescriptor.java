package it.polimi.mediasharing.a3;

import it.polimi.mediasharing.activities.MainActivity;
import a3.a3droid.GroupDescriptor;

public class ExperimentDescriptor extends GroupDescriptor {

	public ExperimentDescriptor() {
		super("A3Test3", MainActivity.PACKAGE_NAME + ".ExperimentSupervisorRole", MainActivity.PACKAGE_NAME + ".ExperimentFollowerRole");
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
