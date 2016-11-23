package it.polimi.greenhouse;

/**
 * Created by saeed on 11/21/2016.
 */

import android.os.Build;
import android.os.Environment;
import android.support.test.espresso.IdlingPolicies;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.text.format.DateUtils;
import android.util.Log;

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
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;


@RunWith(AndroidJUnit4.class)
@LargeTest

public class TestNewSupervisorElection extends TestBase{

    private final String TAG = "NewSupervisorElection";

    private static final int DEVICES_NUMBER = 3;

    private static String ROLE_OUTPUT;
    private static final int WAITING_TIME = 5;
    private static final int WAITING_COUNT = 60;
    private static final int START_TIME = 10;
    private static final int EXPERIMENT_TIME = 60 * 2;
    private static final int STOP_TIME = 40;

    public final static String SUPERVISOR_MODEL = "SM-P605";

    // public final static String SUPERVISOR_MODEL = "XT1052";
    private final static String SPV_EXP_STARTED_OUTPUT =  "Start of Expriment";
    private final static String SPV_EXP_STOPPED_OUTPUT = "End of Expriment";
    private final static String FLW_EXP_STARTED_OUTPUT ="Experiment has started";
    private final static String FLW_EXP_STOPPED_OUTPUT = "Experiment has stopped";

    private long groupInitializationStart = 0;
    private int jointNodes = 0;

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
        if(Build.MODEL.equals(SUPERVISOR_MODEL))
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
        if(Build.MODEL.equals(SUPERVISOR_MODEL))
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
        // Type text and then press the button.
        //onView(withId(R.id.editText1))
        //        .perform(typeText(roleStringOutput), closeSoftKeyboard());


        // Make sure Espresso does not time out
        IdlingPolicies.setMasterPolicyTimeout(1000 * 1000, TimeUnit.MILLISECONDS);
        IdlingPolicies.setIdlingResourceTimeout(1000 * 1000, TimeUnit.MILLISECONDS);

        checkModel();

        mainActivity.createTestControlNode(DEVICES_NUMBER, true);
        Log.i(TAG, "Supervisor: waiting for others");

        int counter = WAITING_COUNT;
        do{
            //checkModel();
            //suspends the main thread of application, but not the Espresso thread
            waitFor(waitingTime);
        }while(--counter > 0 && !mainActivity.isTestGroupReady());

        if(!mainActivity.isTestGroupReady()) {
            Log.w(TAG, "Supervisor: counter reached 0, test cancelled");
            return;
        }

        Log.i(TAG, "Supervisor: starting test with counter=" + counter);

        //onView(withId(startServerButton)).perform(click());
        try {
            mainActivity.getAppNode().connect("control");
        } catch (A3NoGroupDescriptionException e) {
            Log.e(TAG, e.getMessage());
            return;
        }

        try {
            mainActivity.getAppNode().connect("monitoring_1");
        } catch (A3NoGroupDescriptionException e) {
            Log.e(TAG, e.getMessage());
            return;
        }

        groupInitializationStart = System.currentTimeMillis();
        Log.i(TAG, "GroupInitialization started at: " + groupInitializationStart);

        // Now we wait START_TIME for all the sensors to be connected
        Log.i(TAG, "Supervisor: wait for followers");
        waitFor(startTime*5);

        // Checks if this node has joint the group
        //checkGroupAccession();

        //End of Group Initialization
        Log.i(TAG, "First GroupInitialization ended at: " + System.currentTimeMillis() );


        Log.i(TAG, "Disconnect current Supervisor from group");
        try {
            mainActivity.getAppNode().disconnect("monitoring_1");
        } catch (A3ChannelNotFoundException e) {
            Log.i(TAG,e.getMessage());
            return;
        }

        //wait for another supervisor to be elected between other devices
        waitFor(startTime*10);



        /* Start the experiment by pressing the start button
        Log.i(TAG, "Server: starting experiment");
        onView(withId(startExpertimentButton)).perform(click());

        // Now we wait EXPERIMENT_TIME for the experiment to run
        Log.i(TAG, "Server: waiting for the experiment to run");
        waitFor(experimentTime);

        // Check for the right role according to the device model
        Log.i(TAG, "Server: check the role");
        onView(withId(R.id.oneInEditText))
                .check(matches(withPat(ROLE_OUTPUT)));

        // Stop the experiment by pressing the stop button
        Log.i(TAG, "Server: stopping experiment");
        onView(withId(stopExpertimentButton)).perform(click());

        // Now we wait STOP_TIME for the experiment to be terminated
        IdlingResource idlingResource4 = new ElapsedTimeIdlingResource(stopStime);
        Espresso.registerIdlingResources(idlingResource4);

        // Check for the start experiment output in the log
        Log.i(TAG, "Server: check for experiment start");
        onView(withId(R.id.oneInEditText))
                .check(matches(withPat(SPV_EXP_STARTED_OUTPUT)));

        // Check for the stop experiment output in the log
        Log.i(TAG, "Server: check for experiment stop");
        onView(withId(R.id.oneInEditText))
                .check(matches(withPat(SPV_EXP_STOPPED_OUTPUT)));

        */

        // Clean up
        // Espresso.unregisterIdlingResources(idlingResource3);
        //Espresso.unregisterIdlingResources(idlingResource4);
    }

    public void initFollowerAndWait(MainActivity mainActivity, long waitingTime, long startTime, long experimentTime, long stopTime) {
        // Type text and then press the button.
        //onView(withId(R.id.editText1))
        // .perform(typeText(roleStringOutput), closeSoftKeyboard());

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

        Log.i(TAG, "Follower: starting test with counter=" + counter);

        // Now we wait 1x START_TIME for the supervisor to start
        waitFor(startTime);

        // We start the sensor by pressing the button
        Log.i(TAG, "Follower: starting");
        //onView(withId(startSensorButton)).perform(click());
        try {
            groupInitializationStart = System.currentTimeMillis();
            mainActivity.getAppNode().connect("control");
            mainActivity.getAppNode().connect("monitoring_1");
        } catch (A3NoGroupDescriptionException e) {
            Log.e(TAG, e.getMessage());
            return;
        }

        // Now we wait 1x START_TIME for this node to start as a follower
        waitFor(startTime);


        //wait for monitoring_1 supervisor disconnect to happen and a follower be replaced by old supervisor
        waitFor(startTime*12);

        // Checks if this node has joint the group
        checkGroupAccession();
    }


    // This method will be called when a MessageEvent is posted
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void handleGroupEvent(A3GroupEvent event) {

        if(event.groupName.equals("control")) {
            switch (event.eventType) {
                case GROUP_STATE_CHANGED:
                    if(event.object == A3GroupDescriptor.A3GroupState.ACTIVE) {
                        Log.i(TAG, "Group " + event.groupName + " is Active!");
                    }
                    break;

                case SUPERVISOR_ELECTED:
                     if(event.object == A3GroupDescriptor.A3GroupState.ACTIVE)
                         if(!Build.MODEL.equals(SUPERVISOR_MODEL))
                             logResult(System.currentTimeMillis() - groupInitializationStart);
                    break;



                default:
                    break;
            }
        }
        if(event.groupName.equals("monitoring_1")) {
            switch (event.eventType) {
                case GROUP_STATE_CHANGED:
                    if(event.object == A3GroupDescriptor.A3GroupState.ACTIVE) {
                        Log.i(TAG, "Group " + event.groupName + " is Active!");
                    }
                    break;

                case SUPERVISOR_ELECTED:
                    if(event.object == A3GroupDescriptor.A3GroupState.ACTIVE)
                        if(!Build.MODEL.equals(SUPERVISOR_MODEL))
                            logResult(System.currentTimeMillis() - groupInitializationStart);
                    break;



                default:
                    break;
            }
        }
    }


    @Subscribe(threadMode = ThreadMode.POSTING)
    public void handlleTestEvent(TestEvent event){
        if (event.groupName.equals("control")){
            switch (event.eventType){
                case AppConstants.NEW_SUPERVISOR_ELECTED:
                    Log.i(TAG, event.groupName+" new Supervisor Elected");

            }
        }else if(event.groupName.equals("monitoring_1")){
            switch (event.eventType){
                case AppConstants.NEW_SUPERVISOR_ELECTED:
                    Log.i(TAG, event.groupName+" new Supervisor Elected");

            }

        }
    }



    private void logResult(long result){
        Log.i(TAG, "logResult: " + result);
        File resultFolder = Environment.getExternalStorageDirectory();
        File resultFile = new File(resultFolder, AppConstants.EXPERIMENT_PREFIX + "_GroupAccessionTime_" + DEVICES_NUMBER + ".csv");
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

    private void checkGroupAccession(){

        onView(withId(R.id.oneInEditText)).check(matches(withPat("Ctrl")));
    }


}
