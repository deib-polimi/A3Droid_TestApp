package it.polimi.greenhouse;

import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import org.junit.runner.RunWith;


import android.os.Build;
import android.os.Environment;
import android.support.test.espresso.IdlingPolicies;
import android.support.test.rule.ActivityTestRule;
import android.text.format.DateUtils;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import it.polimi.deepse.a3droid.a3.events.A3GroupEvent;
import it.polimi.deepse.a3droid.a3.exceptions.A3ChannelNotFoundException;
import it.polimi.deepse.a3droid.a3.exceptions.A3NoGroupDescriptionException;
import it.polimi.greenhouse.a3.events.TestEvent;
import it.polimi.greenhouse.activities.MainActivity;
import it.polimi.greenhouse.util.AppConstants;
import it.polimit.greenhouse.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class TestLoadExperiment extends TestBase{

    private final String TAG = "TestLoadExperiment";

    private static final int DEVICES_NUMBER =8;
    private static final int MESSAGES_PER_MINUTE_NUMBER=10;
    private static final int MESSAGE_PAYLOAD_SIZE_BYTE=1480;



    private static String ROLE_OUTPUT;
    private static final int WAITING_TIME = 5;
    private static final int WAITING_COUNT = 60;
    private static final int START_TIME = 30;
    private static final int EXPERIMENT_TIME = 67 * 1;
    private static final int STOP_TIME = 40;
    public final static String SUPERVISOR_SERIAL= "HT4BBJT00970";
    //public final static String SUPERVISOR_SERIAL= "TA1760AS2G";
    //public final static String SUPERVISOR_MODEL = "OnePlus3";
    //public final static String SUPERVISOR_MODEL = "SM-P605";
    // public final static String SUPERVISOR_MODEL = "XT1052";
    private final static String SPV_EXP_STARTED_OUTPUT =  "Start of Experiment";
    private final static String SPV_EXP_STOPPED_OUTPUT = "End of Experiment";
    private final static String FLW_EXP_STARTED_OUTPUT ="Experiment has started";
    private final static String FLW_EXP_STOPPED_OUTPUT = "Experiment has stopped";

    private volatile int  dataToWaitFor;
    private volatile String result;

    private long groupInitializationStart = 0;
    private int jointNodes = 0;
    private boolean experimentRunning = false;
    private boolean allMessagesProcessed = false;

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(
            MainActivity.class);

    @Before
    public void initialize(){
        initIds();
        initValidString();
        resetTimeout();
        EventBus.getDefault().register(this);
    }

    @After
    public void finalize(){
        EventBus.getDefault().unregister(this);
    }

    public void initIds(){
        startServerButton = R.id.button4;
        startSensorButton = R.id.button2;
        startExpertimentButton = R.id.button10;
        stopExpertimentButton = R.id.button9;

        followerMsgFrequencyEditText=R.id.editText2;
        followerMsgPayloadEditText=R.id.editText4;
    }

    public void initValidString() {
        if(Build.SERIAL.equals(SUPERVISOR_SERIAL))
            ROLE_OUTPUT = "CtrlSupRole";
        else
            ROLE_OUTPUT = "CtrlFolRole";
    }

    public void resetTimeout() {
        IdlingPolicies.setMasterPolicyTimeout(120, TimeUnit.SECONDS);
        IdlingPolicies.setIdlingResourceTimeout(52, TimeUnit.SECONDS);
    }

    @Test
    public void testDevice(){
        Log.i(TAG, "Starting the test LoadTesting");
        onView(withId(R.id.editText1)).perform(closeSoftKeyboard());
        MainActivity mainActivity = mActivityRule.getActivity();
        mainActivity.createAppNode();
        
         
        if(Build.SERIAL.equals(SUPERVISOR_SERIAL))
            initSupervisorAndWait(mainActivity,
                    DateUtils.SECOND_IN_MILLIS * WAITING_TIME,
                    DateUtils.SECOND_IN_MILLIS * START_TIME,
                    DateUtils.SECOND_IN_MILLIS * EXPERIMENT_TIME,
                    DateUtils.SECOND_IN_MILLIS * STOP_TIME,
                    MESSAGES_PER_MINUTE_NUMBER,
                    MESSAGE_PAYLOAD_SIZE_BYTE
                    );
        else
            initFollowerAndWait(mainActivity,
                    DateUtils.SECOND_IN_MILLIS * WAITING_TIME,
                    DateUtils.SECOND_IN_MILLIS * START_TIME,
                    DateUtils.SECOND_IN_MILLIS * EXPERIMENT_TIME,
                    DateUtils.SECOND_IN_MILLIS * STOP_TIME,
                    MESSAGES_PER_MINUTE_NUMBER,
                    MESSAGE_PAYLOAD_SIZE_BYTE);
    }



    public void initSupervisorAndWait(MainActivity mainActivity, long waitingTime, long startTime, long experimentTime, long stopTime,
                                      int followerMsgFreq,int followerMsgPayload) {


        //changes the follower FREQUENCY to send messages to its supervisor in a higher or lower rate
        onView(withId(followerMsgFrequencyEditText)).perform(replaceText(String.valueOf(followerMsgFreq)));
        //changes the follower PAYLOAD SIZE in bytes to send messages to its supervisor
        onView(withId(followerMsgPayloadEditText)).perform(replaceText(String.valueOf(followerMsgPayload)));
        //waiting for X number of followers to send their RTT
        dataToWaitFor=DEVICES_NUMBER - 1;
        result="";


        // Make sure Espresso does not time out
        IdlingPolicies.setMasterPolicyTimeout(1000 * 1000, TimeUnit.MILLISECONDS);
        IdlingPolicies.setIdlingResourceTimeout(1000 * 1000, TimeUnit.MILLISECONDS);

        checkModel();

        mainActivity.createTestControlNode(DEVICES_NUMBER, true);
        Log.i(TAG, "Supervisor: waiting for others");

        int counter = WAITING_COUNT;
        do{
            //suspends the main thread of application, but not the Espresso thread
            waitFor(waitingTime);
        }while(--counter > 0 && !mainActivity.isTestGroupReady());

        if(!mainActivity.isTestGroupReady()) {
            Log.w(TAG, "Supervisor: counter reached 0, test cancelled");
            return;
        }

        Log.i(TAG, "Supervisor: starting test with counter=" + counter);
        try {
            mainActivity.getTestAppNode().disconnect("test_control");
        } catch (A3ChannelNotFoundException e) {
            e.printStackTrace();
        }
        waitFor(5000);



        //all the nodes have joint
        //supervisor node starts to create  control and monitoring groups and becomes their supervisor
        //onView(withId(startSensorButton)).perform(click());
        try {
            mainActivity.getAppNode().connectAndWaitForActivation("control");
            mainActivity.getAppNode().connectAndWaitForActivation("monitoring_1");
        } catch (A3NoGroupDescriptionException e) {
            e.printStackTrace();
        } catch (A3ChannelNotFoundException e) {
            e.printStackTrace();
        }






        // Now we wait START_TIME for all the sensors to be connected
        Log.i(TAG, "Supervisor: wait for followers");
        waitFor(startTime * 2);
        //End of Group Initialization
        //Log.i(TAG, "GroupInitialization ended at: " + System.currentTimeMillis());

        //now we are sure that all the followers are in the control group and we can ask them to start sending packets to this supervisor
        // Start the experiment by pressing the start button
        Log.i(TAG, "Server: starting experiment");
        onView(withId(startExpertimentButton)).perform(click());

        // Now we wait EXPERIMENT_TIME for the experiment to run
        Log.i(TAG, "Server: waiting for the experiment to run");
        waitFor(experimentTime);

        // Stop the experiment by pressing the stop button, supervisor broad cast to followers to send back their final results
        Log.i(TAG, "Supervisor: stopping experiment");
        //onView(withId(stopExpertimentButton)).perform(click());

        waitForAllMessages();

        // Now we wait STOP_TIME for the experiment to be terminated
       // waitFor(stopTime);

        // Check for the right role according to the device model
        Log.i(TAG, "Supervisor: check the role");
        onView(withId(R.id.oneInEditText)).check(matches(withPat(ROLE_OUTPUT)));


        // Check for the start experiment output in the log
        Log.i(TAG, "Supervisor: check for experiment start");
        checkSuperExperimentStarted();
        checkSuperExperimentStopped();
    }

    public void initFollowerAndWait(MainActivity mainActivity, long waitingTime, long startTime, long experimentTime, long stopTime,
                                    int followerMsgFreq, int followerMsgPayload) {


        // Make sure Espresso does not time out
        IdlingPolicies.setMasterPolicyTimeout(1000 * 1000, TimeUnit.MILLISECONDS);
        IdlingPolicies.setIdlingResourceTimeout(1000 * 1000, TimeUnit.MILLISECONDS);

        //changes the follower FREQUENCY to send messages to its supervisor in a higher or lower rate
        onView(withId(followerMsgFrequencyEditText)).perform(replaceText(String.valueOf(followerMsgFreq)));
        //changes the follower PAYLOAD SIZE in bytes to send messages to its supervisor
        onView(withId(followerMsgPayloadEditText)).perform(replaceText(String.valueOf(followerMsgPayload)));

        checkModel();

        //Waits for a 0 - 10 seconds to avoid too many simultaneous devices joining the same group
        Log.i(TAG, "Follower: random wait to connect");
        long randomWait = (long) (DateUtils.SECOND_IN_MILLIS * Math.random() * WAITING_TIME * 2);
        waitFor(randomWait);

        Log.i(TAG, "Follower: waiting for others");
        mainActivity.createTestControlNode(DEVICES_NUMBER, false);

        int counter = WAITING_COUNT;
        do{
            waitFor(waitingTime);
        }while(--counter > 0 && !mainActivity.isTestGroupReady());

        if(!mainActivity.isTestGroupReady()) {
            Log.w(TAG, "Follower: counter reached 0, test cancelled");
            return;
        }

        Log.i(TAG, "Follower: starting test with counter=" + counter);
        try {
            mainActivity.getTestAppNode().disconnect("test_control");
        } catch (A3ChannelNotFoundException e) {
            e.printStackTrace();
        }
        waitFor(5000);



        // Now we wait 1x START_TIME for the supervisor to start
        waitFor(startTime);


        //onView(withId(startSensorButton)).perform(click());
        try {
            mainActivity.getAppNode().connectAndWaitForActivation("control");
            mainActivity.getAppNode().connectAndWaitForActivation("monitoring_1");
        } catch (A3NoGroupDescriptionException e) {
            e.printStackTrace();
        } catch (A3ChannelNotFoundException e) {
            e.printStackTrace();
        }

        //wait for sensors to send message to supervisor and experiment to be completed
        waitFor(startTime + experimentTime);

        waitForAllMessages();



        // Checks if this node has joint the group
        checkFollowerAccession();
        checkFollowerSendRTTtoSupervisor();
    }

    private void waitForAllMessages() {
        while (!allMessagesProcessed){
            waitFor(DateUtils.SECOND_IN_MILLIS * 5);
        }
    }

    // This method will be called when a MessageEvent is posted
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void handleGroupEvent(A3GroupEvent event) {
    }

    @Subscribe(threadMode = ThreadMode.POSTING)
    public void handleTestEvent(TestEvent event) {
        if (event.groupName.equals("control")){
            switch (event.eventType){
                case AppConstants.START_EXPERIMENT:
                    Log.i(TAG, event.groupName+"test event: Start of Experiment");
                    experimentRunning=true;
                    break;
                case AppConstants.DATA:
                    //average RTT from follower received log the results to supervisor
                    dataToWaitFor--;
                    parseMessage(event.object);
                    if(dataToWaitFor == 0) {
                        Log.i(TAG,"dataToWaitFor: "+dataToWaitFor);
                        //write to csv file
                        allMessagesProcessed=true;
                        logResult(result);
                    }
                    break;
                case AppConstants.ALL_MESSAGES_RECEIVED:
                case AppConstants.ALL_MESSAGES_REPLIED:
                    Log.i(TAG,"all messages replied");
                    allMessagesProcessed = true;
                    break;
                default:
                    break;
            }
        }
    }

    private void parseMessage(Object object) {
         String objectValue[] =  String.valueOf(object).split("\t");
         result+=objectValue[0]+","+objectValue[1]+","+objectValue[2]+","+objectValue[3]+","+objectValue[4].split(" ")[0]+"\n";
    }


    private void logResult(String result){
        Log.i(TAG, "logResult: " + result);
        File resultFolder = Environment.getExternalStorageDirectory();
        File resultFile = new File(resultFolder, AppConstants.EXPERIMENT_PREFIX + "_GroupLoadExperiment_" + DEVICES_NUMBER+"_"+
                MESSAGES_PER_MINUTE_NUMBER+"_"+MESSAGE_PAYLOAD_SIZE_BYTE+ ".csv");
        try {
            FileWriter fw = new FileWriter(resultFile, true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(result + "\n");
            bw.flush();
        } catch (IOException e) {
            Log.e(TAG, "cannot write the log file "+e.getLocalizedMessage());
        }
    }



    private void checkModel(){
        onView(withId(R.id.oneInEditText))
                .check(matches(withPat(Build.MANUFACTURER)));
    }

    private void checkFollowerAccession(){
        onView(withId(R.id.oneInEditText)).check(matches(withPat("CtrlFolRole")));
    }
    private void checkFollowerSendRTTtoSupervisor(){
        onView(withId(R.id.oneInEditText)).check(matches(withPat("Average RTT of follower sent to Control supervisor")));
    }

    private void checkSuperExperimentStopped() {

       // onView(withId(R.id.oneInEditText)).check(matches(withPat(SPV_EXP_STOPPED_OUTPUT)));
    }

    private void checkSuperExperimentStarted() {
        onView(withId(R.id.oneInEditText))
                .check(matches(withPat(SPV_EXP_STARTED_OUTPUT)));


    }


}
