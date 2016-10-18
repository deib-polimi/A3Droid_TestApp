package it.polimi.greenhouse.activities;

import it.polimi.greenhouse.a3.groups.ActuatorsDescriptor;
import it.polimi.greenhouse.a3.groups.ControlDescriptor;
import it.polimi.greenhouse.a3.groups.MonitoringDescriptor;
import it.polimi.greenhouse.a3.groups.ServerDescriptor;
import it.polimi.greenhouse.a3.roles.ActuatorFollowerRole;
import it.polimi.greenhouse.a3.roles.ActuatorSupervisorRole;
import it.polimi.greenhouse.a3.roles.ControlFollowerRole;
import it.polimi.greenhouse.a3.roles.ControlSupervisorRole;
import it.polimi.greenhouse.a3.roles.SensorFollowerRole;
import it.polimi.greenhouse.a3.roles.SensorSupervisorRole;
import it.polimi.greenhouse.a3.roles.ServerFollowerRole;
import it.polimi.greenhouse.a3.roles.ServerSupervisorRole;
import it.polimi.greenhouse.util.AppConstants;
import it.polimit.greenhouse.R;

import java.util.ArrayList;

import a3.a3droid.A3DroidActivity;
import a3.a3droid.A3Message;
import a3.a3droid.A3Node;
import a3.a3droid.GroupDescriptor;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends A3DroidActivity{
	
	private A3Node node;
	private EditText inText, sensorsFrequency, actuatorsFrequency, sensorsPayload, actuatorsPayload;
	private Handler toGuiThread;
	private Handler fromGuiThread;
	private EditText experiment;
	public static int runningExperiment;
	private boolean experimentRunning = false;
	
	public static void setRunningExperiment(int runningExperiment) {
		MainActivity.runningExperiment = runningExperiment;
	}

	@SuppressLint("HandlerLeak")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);
		
		toGuiThread = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				
				inText.append(msg.obj + "\n");
			}
		};
		
		HandlerThread thread = new HandlerThread("Handler");
		thread.start();
		fromGuiThread = new Handler(thread.getLooper()){
			@Override
			public void handleMessage(Message msg){
				ArrayList<String> roles = new ArrayList<String>();
				roles.add(ControlSupervisorRole.class.getName());
				roles.add(ControlFollowerRole.class.getName());
				ArrayList<GroupDescriptor> groupDescriptors = new ArrayList<GroupDescriptor>();
				groupDescriptors.add(new ControlDescriptor());
				switch (msg.what) {
					case AppConstants.STOP_EXPERIMENT_COMMAND:
						if(experimentRunning){
							node.sendToSupervisor(new A3Message(AppConstants.LONG_RTT, ""), "control");
							experimentRunning = false;
						}
						break;
					case AppConstants.START_EXPERIMENT_USER_COMMAND:
						if(!experimentRunning){
							experimentRunning = true;					
							if(node.isConnectedForApplication("server_0"))
								node.sendToSupervisor(new A3Message(AppConstants.SET_PARAMS_COMMAND, "A_" + actuatorsFrequency.getText().toString() + "_" + actuatorsPayload.getText().toString()), 
										"control");
							if(node.isConnectedForApplication("monitoring_" + experiment.getText().toString()))
								node.sendToSupervisor(new A3Message(AppConstants.SET_PARAMS_COMMAND, "S_" + sensorsFrequency.getText().toString() + "_" + sensorsPayload.getText().toString()), 
										"control");
							node.sendToSupervisor(new A3Message(AppConstants.START_EXPERIMENT_USER_COMMAND, ""), "control");
						}
						break;
					case AppConstants.START_SENSOR:
						if(experimentRunning)
							break;
						roles.add(SensorSupervisorRole.class.getName());
						roles.add(SensorFollowerRole.class.getName());		
						roles.add(ServerFollowerRole.class.getName());
						groupDescriptors.add(new MonitoringDescriptor() {							
							
							@Override
							public int getSupervisorFitnessFunction() {
																
								return 0;
							}
							
						});
						groupDescriptors.add(new ServerDescriptor());
						node = new A3Node(getUUID(), MainActivity.this, roles, groupDescriptors);
						node.connect("control", true, true);
						node.connect("monitoring_" + experiment.getText().toString(), true, true);						
						break;	
					case AppConstants.START_ACTUATOR:
						if(experimentRunning)
							break;
						roles.add(ActuatorSupervisorRole.class.getName());
						roles.add(ActuatorFollowerRole.class.getName());
						roles.add(ServerFollowerRole.class.getName());
						groupDescriptors.add(new ActuatorsDescriptor());
						groupDescriptors.add(new ServerDescriptor());
						node = new A3Node(getUUID(), MainActivity.this, roles, groupDescriptors);
						node.connect("control", true, true);
						node.connect("actuators_" + experiment.getText().toString(), true, true);						
						break;
					case AppConstants.START_SERVER:
						if(experimentRunning)
							break;
						roles.add(ServerSupervisorRole.class.getName());
						roles.add(ServerFollowerRole.class.getName());	
						groupDescriptors.add(new ServerDescriptor());
						node = new A3Node(getUUID(), MainActivity.this, roles, groupDescriptors);
						node.connect("control", true, true);
						node.connect("server_0", true, true);						
						break;
					default:
						break;
				}		
					
			}
		};

		inText=(EditText)findViewById(R.id.oneInEditText);
		experiment = (EditText)findViewById(R.id.editText1);
		sensorsFrequency = (EditText)findViewById(R.id.editText2);
		sensorsPayload = (EditText)findViewById(R.id.editText4);
		actuatorsFrequency = (EditText)findViewById(R.id.editText3);
		actuatorsPayload = (EditText)findViewById(R.id.editText5);
	}

	public void createGroup(View v){
		fromGuiThread.sendEmptyMessage(AppConstants.CREATE_GROUP_USER_COMMAND);
	}
	
	public void stopExperiment(View v){
		fromGuiThread.sendEmptyMessage(AppConstants.STOP_EXPERIMENT_COMMAND);
	}
	
	public void startExperiment(View v){
		fromGuiThread.sendEmptyMessage(AppConstants.START_EXPERIMENT_USER_COMMAND);
	}
	
	public void startSensor(View v){
		if(!experimentRunning)
			fromGuiThread.sendEmptyMessage(AppConstants.START_SENSOR);
	}
	
	public void startActuator(View v){
		if(!experimentRunning)
			fromGuiThread.sendEmptyMessage(AppConstants.START_ACTUATOR);
	}
	
	public void startServer(View v){
		if(!experimentRunning)
			fromGuiThread.sendEmptyMessage(AppConstants.START_SERVER);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	protected void onDestroy(){
		if(experimentRunning)
			node.disconnect("control", true);
		System.exit(0);
	}
	
	@Override
	public void showOnScreen(String message) {
		toGuiThread.sendMessage(toGuiThread.obtainMessage(0, message));
	}
}
