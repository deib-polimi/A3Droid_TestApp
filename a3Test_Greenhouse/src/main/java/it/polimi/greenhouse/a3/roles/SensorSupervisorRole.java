package it.polimi.greenhouse.a3.roles;

import android.util.Log;

import it.polimi.deepse.a3droid.a3.A3Message;
import it.polimi.deepse.a3droid.pattern.TimerInterface;
import it.polimi.deepse.a3droid.a3.A3SupervisorRole;
import it.polimi.greenhouse.activities.MainActivity;
import it.polimi.greenhouse.util.AppConstants;
import it.polimi.greenhouse.util.StringTimeUtil;

public class SensorSupervisorRole extends A3SupervisorRole implements TimerInterface {

    private boolean startExperiment;
    private boolean experimentIsRunning;
    private boolean paramsSet = false;
    private int currentExperiment;
    private int sentCont;
    private double avgRTT;
    private byte sPayLoad[];
    private String startTimestamp;

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
        experimentIsRunning = false;
        sentCont = 0;
        avgRTT = 0;
        //node.connect("server_0", true, true);
        //node.sendToSupervisor(new A3Message(AppConstants.JOINED, getGroupName() + "_" + node.getUID() + "_" + getChannelId()), "control");
    }

    @Override
    public void logic() {
        showOnScreen("[" + getGroupName() + "_SupRole]");
        active = false;
    }

    public void receiveApplicationMessage(A3Message message) {
        switch (message.reason) {
            case AppConstants.SET_PARAMS:
                if (message.senderAddress.equals(getChannelId()) && !paramsSet) {
                    String params[] = message.object.split("_");
                    if (!params[0].equals("S"))
                        break;
                    paramsSet = true;
                    sendBroadcast(message);
                    long freq = Long.valueOf(params[1]);
                    this.MAX_INTERNAL = 60 * 1000 / freq;
                    this.PAYLOAD_SIZE = Integer.valueOf(params[2]);
                    showOnScreen("Params set to: " + freq + " Mes/min and " + PAYLOAD_SIZE + " Bytes");
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
                //sensorPing(message);
                sensorPingBack(message);
                break;

            case AppConstants.SENSOR_PONG:
                String sensorAddress = ((String) message.object).split("#")[0];
                //String experiment = ((String)message.object).split("#")[1];
                String sendTime = ((String) message.object).split("#")[2];
                message.object = sendTime;

                if (!sensorAddress.equals(getChannelId())) {
                    sendUnicast(message, sensorAddress);
                } else {
                    showOnScreen("Server response received");
                    sentCont++;
                    double rtt = StringTimeUtil.roundTripTime(((String) message.object), StringTimeUtil.getTimestamp()) / 1000;
                    avgRTT = (avgRTT * (sentCont - 1) + rtt) / sentCont;

                    if (rtt > TIMEOUT && experimentIsRunning) {
                        experimentIsRunning = false;
                        node.sendToSupervisor(new A3Message(AppConstants.LONG_RTT, ""), "control");
                    } else {
                        //new Timer(this, 0, (int) (Math.random() * MAX_INTERNAL)).start();
                    }

                    if (sentCont % 100 == 0)
                        showOnScreen(sentCont + " mex spediti.");
                }
                break;

            case AppConstants.START_EXPERIMENT:
                if (startExperiment) {
                    if (!experimentIsRunning) {
                        startExperiment = false;
                        experimentIsRunning = true;
                        showOnScreen("Experiment has started");
                        sendBroadcast(message);
                        startTimestamp = StringTimeUtil.getTimestamp();
                        sentCont = 0;
                        sPayLoad = StringTimeUtil.createPayload(PAYLOAD_SIZE);
                        //sendMessage();
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
                    showOnScreen("Experiment has stopped");
                    double runningTime = StringTimeUtil.roundTripTime(startTimestamp, StringTimeUtil.getTimestamp()) / 1000;
                    float frequency = sentCont / (float) (runningTime);
                    node.sendToSupervisor(new A3Message(AppConstants.DATA, "StoS: " + sentCont + "\t" +
                            runningTime + "\t" + frequency + "\t" + avgRTT), "control");
                }
                break;

            default:
                break;
        }
    }

    private void sendMessage() {
        if (experimentIsRunning)
            sendToSupervisor(new A3Message(AppConstants.SENSOR_PING, currentExperiment + "#" + StringTimeUtil.getTimestamp(), sPayLoad));
    }

    private void sensorPing(A3Message message) {
        message.object = message.senderAddress + "#" + (String) message.object;
        node.sendToSupervisor(message, "server_0");
    }

    private void sensorPingBack(A3Message message) {
        //String experiment = ((String)message.object).split("#")[0];
        String sendTime = ((String) message.object).split("#")[1];
        message.reason = AppConstants.SENSOR_PONG;
        message.object = message.senderAddress + "#" + sendTime;
        //channel.sendUnicast(message, message.senderAddress);
        sendBroadcast(message);
        showOnScreen("Sensor data received");
    }

    @Override
    public void handleTimeEvent(int reason, Object object) {
        sendMessage();
    }

    public void memberAdded(String name) {
        showOnScreen("Entered: " + name);
        A3Message msg = new A3Message(AppConstants.MEMBER_ADDED, name);
        node.sendToSupervisor(msg, "control");
    }

    public void memberRemoved(String name) {
        showOnScreen("Exited: " + name);
        A3Message msg = new A3Message(AppConstants.MEMBER_REMOVED, name);
        node.sendToSupervisor(msg, "control");
    }
}
