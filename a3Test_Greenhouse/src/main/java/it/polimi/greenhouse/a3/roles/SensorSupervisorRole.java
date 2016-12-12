package it.polimi.greenhouse.a3.roles;

import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import it.polimi.deepse.a3droid.a3.A3Message;
import it.polimi.deepse.a3droid.a3.A3SupervisorRole;
import it.polimi.deepse.a3droid.a3.exceptions.A3ChannelNotFoundException;
import it.polimi.deepse.a3droid.a3.exceptions.A3SupervisorNotElectedException;
import it.polimi.greenhouse.a3.events.TestEvent;
import it.polimi.greenhouse.activities.MainActivity;
import it.polimi.greenhouse.util.AppConstants;

public class SensorSupervisorRole extends A3SupervisorRole{

    private boolean startExperiment;
    private boolean experimentIsRunning;
    private boolean paramsSet = false;
    private int currentExperiment;
    private int receivedCont, repliedCount;
    private double avgRTT;
    private byte sPayLoad[];
    private String startTimestamp;
    private List<Double> listRTT;
    private  String allFollowerRTTs;

    private final static long TIMEOUT = 60 * 1000;
    private long MAX_INTERNAL = 10 * 1000;
    private int PAYLOAD_SIZE = 32;

    public SensorSupervisorRole() {
        super();
    }

    @Override
    public void onActivation() {
        currentExperiment = Integer.valueOf(getGroupName().split("_")[1]);
        startExperiment = true;
        listRTT=new ArrayList<>();
        experimentIsRunning = false;
        receivedCont = repliedCount = 0;
        avgRTT = 0;
        try {
            if(node.isConnected("control") && node.waitForActivation("control"))
                node.sendToSupervisor(
                new A3Message(AppConstants.JOINED,
                        getGroupName() +
                    "_" + currentExperiment +
                    "_" + node.getUID() +
                    "_" + getChannelId()+
                    "_" + System.currentTimeMillis()+
                    "_" + "SupRole"
                ), "control"
            );
        } catch (A3SupervisorNotElectedException e) {
            e.printStackTrace();
        } catch (A3ChannelNotFoundException e) {
            e.printStackTrace();
        }
        postUIEvent(0, "[" + getGroupName() + "_SupRole]");
    }

    @Override
    public void onDeactivation() {
        postUIEvent(0, "[" + getGroupName() + "_SupRole] deactivated");
    }

    @Override
    public void receiveApplicationMessage(A3Message message) {

        switch (message.reason) {
            case AppConstants.SET_PARAMS:
                if (message.senderAddress.equals(getChannelId()) && !paramsSet) {
                    String params[] = message.object.split("\\.");
                    if (!params[0].equals("S"))
                        break;
                    paramsSet = true;
                    sendBroadcast(message);
                    long freq = Long.valueOf(params[1]);
                    this.MAX_INTERNAL = 60 * 1000 / freq;
                    this.PAYLOAD_SIZE = Integer.valueOf(params[2]);
                    postUIEvent(0, "Params set to: " + freq + " Mes/min and " + PAYLOAD_SIZE + " Bytes");
                }
                break;

            case AppConstants.ADD_MEMBER:
                if (experimentIsRunning) {
                    message.reason = AppConstants.SET_PARAMS;
                    message.object = (60 * 1000 / this.MAX_INTERNAL) + "_" + PAYLOAD_SIZE;
                    sendBroadcast(message);
                }
                break;

            case AppConstants.SENSOR_PING:
                receivedCont++;
                sensorPingBack(message);
                break;

            case AppConstants.START_EXPERIMENT:
                if (startExperiment) {
                    if (!experimentIsRunning) {
                        startExperiment = false;
                        experimentIsRunning = true;
                        postUIEvent(0, "Experiment has started");
                        sendBroadcast(message);
                    }
                } else
                    startExperiment = true;

                break;

            case AppConstants.LONG_RTT:
                Log.i(MainActivity.TAG, "Stopping the experiment");
                sendBroadcast(new A3Message(AppConstants.STOP_EXPERIMENT_COMMAND, ""));
                if (experimentIsRunning) {
                    paramsSet = false;
                    experimentIsRunning = false;
                    postUIEvent(0, "Experiment has stopped");
                    checkOutBound();
                }
                break;

            default:
                break;
        }
    }

    private void sensorPingBack(A3Message message) {
        String experiment = ((String)message.object).split("#")[0];
        String sendTime = ((String) message.object).split("#")[1];
        message.reason = AppConstants.SENSOR_PONG;
        message.object = message.senderAddress + "#" + sendTime;
        sendUnicast(message, message.senderAddress);
        postUIEvent(0, "Sensor data received");
    }

    private void checkOutBound(){
        while(!getChannel().isOutboundEmpty()) {
            try {
                synchronized (this) {
                    this.wait(WAIT_TIME);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        EventBus.getDefault().post(new TestEvent(AppConstants.ALL_MESSAGES_RECEIVED, "control", null));
    }

    private final int WAIT_TIME = 1000;

    public void memberAdded(String name) {
        postUIEvent(0, "Entered: " + name);
        A3Message msg = new A3Message(AppConstants.MEMBER_ADDED, name);
        try {
            node.sendToSupervisor(msg, "control");
        } catch (A3SupervisorNotElectedException e) {
            e.printStackTrace();
        }
    }

    public void memberRemoved(String name) {
        postUIEvent(0, "Exited: " + name);
        A3Message msg = new A3Message(AppConstants.MEMBER_REMOVED, name);
        try {
            node.sendToSupervisor(msg, "control");
        } catch (A3SupervisorNotElectedException e) {
            e.printStackTrace();
        }
    }
}
