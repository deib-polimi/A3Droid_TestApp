package it.polimi.greenhouse;

/**
 * Created by saeed on 11/21/2016.
 */

import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.support.test.espresso.IdlingPolicies;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.text.format.DateUtils;
import android.util.Log;

import junit.framework.AssertionFailedError;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import it.polimi.deepse.a3droid.a3.A3GroupDescriptor;
import it.polimi.deepse.a3droid.a3.A3Message;
import it.polimi.deepse.a3droid.a3.events.A3GroupEvent;

import it.polimi.deepse.a3droid.a3.events.A3UIEvent;
import it.polimi.deepse.a3droid.a3.exceptions.A3ChannelNotFoundException;
import it.polimi.deepse.a3droid.a3.exceptions.A3NoGroupDescriptionException;
import it.polimi.deepse.a3droid.a3.exceptions.A3SupervisorNotElectedException;
import it.polimi.greenhouse.a3.events.TestEvent;
import it.polimi.greenhouse.activities.MainActivity;
import it.polimi.greenhouse.util.AppConstants;
import it.polimit.greenhouse.R;


import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;


@RunWith(AndroidJUnit4.class)
@LargeTest

public class TestNewSupervisorElection extends TestBase{

    private final String TAG = "NewSupervisorElection";

    private static final int DEVICES_NUMBER =10;

    private static String ROLE_OUTPUT;
    private static final int WAITING_TIME = 5;
    private static final int WAITING_COUNT = 60;
    private static final int START_TIME = 10;
    private static final int EXPERIMENT_TIME = 70 * 2;
    private static final int STOP_TIME = 40;

    //public final static String SUPERVISOR_MODEL = "Nexus 9";
    //public final static String SUPERVISOR_MODEL = "SM-P605";
    // public final static String SUPERVISOR_MODEL = "XT1052";
    public final static String SUPERVISOR_SERIAL= "HT4BBJT00970";
    public final static String CONTROL_SUPERVISOR_SERIAL="HT4BVJT00109";

    private final static String SPV_EXP_STARTED_OUTPUT =  "Start of Expriment";
    private final static String SPV_EXP_STOPPED_OUTPUT = "End of Expriment";
    private final static String FLW_EXP_STARTED_OUTPUT ="Experiment has started";
    private final static String FLW_EXP_STOPPED_OUTPUT = "Experiment has stopped";

    private volatile long  supervisorDisconnectionStart;
    private long lastFollowerJoin=0;
    private long supervisorLostTimeout=0;
    private volatile int ignorFirstNodeJoinEvents;
    private volatile int minNumberOfExpectedFollowers;


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
    }

    public void initValidString() {
        // Specify a valid string for the test based on the model
        if( Build.SERIAL.equals(SUPERVISOR_SERIAL))
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
        Log.i(TAG, "Starting the test: New Supervisor Election");
        onView(withId(R.id.editText1)).perform(closeSoftKeyboard());
        MainActivity mainActivity = mActivityRule.getActivity();
        mainActivity.createAppNode();


        if( Build.SERIAL.equals(SUPERVISOR_SERIAL))
            initSupervisorAndWait(mainActivity,
                    DateUtils.SECOND_IN_MILLIS * WAITING_TIME,
                    DateUtils.SECOND_IN_MILLIS * START_TIME,
                    DateUtils.SECOND_IN_MILLIS * EXPERIMENT_TIME,
                    DateUtils.SECOND_IN_MILLIS * STOP_TIME);
        else
            initFollowerAndWait(mainActivity,
                    DateUtils.SECOND_IN_MILLIS * WAITING_TIME,
                    DateUtils.SECOND_IN_MILLIS * START_TIME,
                    DateUtils.SECOND_IN_MILLIS * EXPERIMENT_TIME,
                    DateUtils.SECOND_IN_MILLIS * STOP_TIME);
    }



    public void initSupervisorAndWait(MainActivity mainActivity, long waitingTime, long startTime, long experimentTime, long stopStime) {



        // Make sure Espresso does not time out
        IdlingPolicies.setMasterPolicyTimeout(1000 * 1000, TimeUnit.MILLISECONDS);
        IdlingPolicies.setIdlingResourceTimeout(1000 * 1000, TimeUnit.MILLISECONDS);
        ignorFirstNodeJoinEvents=DEVICES_NUMBER;
        minNumberOfExpectedFollowers=DEVICES_NUMBER-2;

        checkModel();

        mainActivity.createTestControlNode(DEVICES_NUMBER, true);
        Log.i(TAG, "Supervisor: waiting for others");

        int counter = WAITING_COUNT;
        do{

            waitFor(waitingTime);
        }while(--counter > 0 && !mainActivity.isTestGroupReady());

        if(!mainActivity.isTestGroupReady()) {
            Log.w(TAG, "Supervisor: counter reached 0, test cancelled");
            return;
        }

        try {
            mainActivity.getTestAppNode().disconnect("test_control");
        } catch (A3ChannelNotFoundException e) {
            e.printStackTrace();
        }

        Log.i(TAG, "Supervisor: starting test with counter=" + counter);



        //all the nodes have joint test_control
        //supervisor node starts to create  control and monitoring groups and becomes their supervisor
        //onView(withId(startSensorButton)).perform(click());

        waitFor(startTime);

        try {
            //mainActivity.getAppNode().connectAndWaitForActivation("control");
            mainActivity.getAppNode().connectAndWaitForActivation("monitoring_1");

        } catch (A3NoGroupDescriptionException e) {
            e.printStackTrace();
        } catch (A3ChannelNotFoundException e) {
            e.printStackTrace();
        }

        waitFor(startTime);
        try {
            //mainActivity.getAppNode().connectAndWaitForActivation("control");
            mainActivity.getAppNode().connectAndWaitForActivation("control");

        } catch (A3NoGroupDescriptionException e) {
            e.printStackTrace();
        } catch (A3ChannelNotFoundException e) {
            e.printStackTrace();
        }





        // Now we wait START_TIME for all the sensors to be connected
        Log.i(TAG, "Supervisor: wait for followers");
        waitFor(startTime*5);
        try {
            mainActivity.getAppNode().sendToSupervisor(new A3Message(AppConstants.SUPERVISOR_LEFT, ""), "control");
        } catch (A3SupervisorNotElectedException e) {
            e.printStackTrace();
        }


        //wait for another supervisor to be elected between other devices
       /* waitFor(experimentTime);



*/


    }


    public void initFollowerAndWait(MainActivity mainActivity, long waitingTime, long startTime, long experimentTime, long stopTime) {


        // Make sure Espresso does not time out
        IdlingPolicies.setMasterPolicyTimeout(1000 * 1000, TimeUnit.MILLISECONDS);
        IdlingPolicies.setIdlingResourceTimeout(1000 * 1000, TimeUnit.MILLISECONDS);

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

        try {
            mainActivity.getTestAppNode().disconnect("test_control");
        } catch (A3ChannelNotFoundException e) {
            e.printStackTrace();
        }

        Log.i(TAG, "Follower: starting test with counter=" + counter);


        if(Build.SERIAL.equals(CONTROL_SUPERVISOR_SERIAL)) {
            try {
                mainActivity.getAppNode().connectAndWaitForActivation("control");

            } catch (A3NoGroupDescriptionException e) {
                e.printStackTrace();
            } catch (A3ChannelNotFoundException e) {
                e.printStackTrace();
            }
        }

        //waite for the supervisor device to push the sensor button first
        waitFor(startTime);

        if(!Build.SERIAL.equals(CONTROL_SUPERVISOR_SERIAL)) {
            try {
                mainActivity.getAppNode().connectAndWaitForActivation("control");

            } catch (A3NoGroupDescriptionException e) {
                e.printStackTrace();
            } catch (A3ChannelNotFoundException e) {
                e.printStackTrace();
            }
        }

        waitFor(startTime);

        // We start the sensor by pressing the button
        Log.i(TAG, "Follower: starting");
        //onView(withId(startSensorButton)).perform(click());
        try {
            mainActivity.getAppNode().connectAndWaitForActivation("monitoring_1");

        } catch (A3NoGroupDescriptionException e) {
            e.printStackTrace();
        } catch (A3ChannelNotFoundException e) {
            e.printStackTrace();
        }




        //wait for monitoring_1 supervisor disconnect happen and a follower be replaced by old supervisor
        waitFor(experimentTime);
        waitFor(startTime*3);

        // Checks if this node has joint the group
        checkFirstFollowersAccession();


        try {
            //Log.i(TAG,"saeed3 ");
            checkGroupReactivation();
            //test passed -> log result
            //Log.i(TAG, "After test, log results:"+(lastFollowerJoin - supervisorDisconnectionStart));
            if(minNumberOfExpectedFollowers <=0)
                logResult((supervisorLostTimeout-supervisorDisconnectionStart)+","+(lastFollowerJoin - supervisorLostTimeout));

        }catch (AssertionFailedError error){
            //test failed
            //Log.i(TAG,"saeed4 ");

            return;
        }
    }


    //Group Change Events
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void handleGroupEvent(A3GroupEvent event) {
       if(event.groupName.equals("control")) {

        }

    }


   //Test Events
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void handleTestEvent(TestEvent event){
        if (event.groupName.equals("control")){
            switch (event.eventType){
                case AppConstants.NEW_SUPERVISOR_ELECTED:
                    Log.i(TAG, event.groupName+" new Supervisor Elected");
                    break;
                case AppConstants.SUPERVISOR_LEFT:
                   // long followerMessageReceivedTime=Long.valueOf((String) event.object);
                    Log.i(TAG, event.groupName+" Supervisor left: ");
                    supervisorDisconnectionStart=System.currentTimeMillis();
                    break;
                case AppConstants.ROLE_DEACTIVATED:
                    if(event.object.toString().equals("monitoring_1") && supervisorLostTimeout== 0){
                        supervisorLostTimeout=System.currentTimeMillis();
                    }
                    break;
                case AppConstants.JOINED:
                    ignorFirstNodeJoinEvents--;
                    Log.i(TAG,"SAeed:1 "+ignorFirstNodeJoinEvents);
                    if(ignorFirstNodeJoinEvents < 0) {
                        String[] message = event.object.toString().split("_");
                        String givenRole = event.object.toString().split("#")[1];
                        lastFollowerJoin = System.currentTimeMillis();//Long.valueOf(message[5]);
                        EventBus.getDefault().post(new A3UIEvent(0,  givenRole + " arrived"));
                        Log.i(TAG, "group reshaped again : " + (lastFollowerJoin - supervisorLostTimeout) + " node: " +
                                message[3] + " role: " +givenRole + "  group: "+message[0]+"_"+message[1]+" SupTimeout:"+ (supervisorLostTimeout-supervisorDisconnectionStart));
                        if(givenRole.equals("FolRole")){
                            minNumberOfExpectedFollowers--;
                        }
                    }
                    break;
            }
        }
    }



    private void logResult(String result){
        Log.i(TAG, "logResult: " + result);
        File resultFolder = Environment.getExternalStorageDirectory();
        File resultFile = new File(resultFolder, AppConstants.EXPERIMENT_PREFIX + "_GroupNewSupervisorElection_" + DEVICES_NUMBER + ".csv");
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

    private void checkFirstFollowersAccession(){
        if(Build.SERIAL.equals(CONTROL_SUPERVISOR_SERIAL)) {
            onView(withId(R.id.oneInEditText)).check(matches(withPat("CtrlSupRole")));
        }else{
            onView(withId(R.id.oneInEditText)).check(matches(withPat("CtrlFolRole")));
        }
        onView(withId(R.id.oneInEditText)).check(matches(withPat("monitoring_1_Fol")));
    }


    private void checkGroupReactivation() {
        onView(withId(R.id.oneInEditText)).check(matches(withPat("FolRole arrived")));

    }



}
