package it.polimi.greenhouse;



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
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class TestGroupMerge extends TestBase{

    private final String TAG = "TestGroupMerge";
    private static final int SECONDARY_GROUP_ID=2;
    private static final int DEVICES_NUMBER = 3;




    private static String ROLE_OUTPUT;
    private static final int WAITING_TIME = 5;
    private static final int WAITING_COUNT = 60;
    private static final int START_TIME = 10;
    private static final int EXPERIMENT_TIME = 60 * 2;
    private static final int STOP_TIME = 40;

    //// TODO: 11/29/2016 in a device farm, we have to decide about model and serial of supervisor device
    public final static String SUPERVISOR_MODEL = "Nexus 9";
    public final static String SUPERVISOR_SERIAL= "HT4BBJT00970";

    //public final static String SUPERVISOR_MODEL = "SM-P605";
    // public final static String SUPERVISOR_MODEL = "XT1052";
    private final static String SPV_EXP_STARTED_OUTPUT =  "Start of Expriment";
    private final static String SPV_EXP_STOPPED_OUTPUT = "End of Expriment";
    private final static String FLW_EXP_STARTED_OUTPUT ="Experiment has started";
    private final static String FLW_EXP_STOPPED_OUTPUT = "Experiment has stopped";

    private long groupInitializationStart = 0;
    private long groupMergeStart=0;
    private long lastFollowerJoin=0;
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
        startMergeButton=R.id.button6;
        //group ID edit text
        groupIdEditText=R.id.editText1;
    }

    public void initValidString() {
        // Specify a valid string for the test based on the model
        if(Build.MODEL.equals(SUPERVISOR_MODEL) && Build.SERIAL.equals(SUPERVISOR_SERIAL))
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
        Log.i(TAG, "Starting the test Group Merge");
        onView(withId(R.id.editText1)).perform(closeSoftKeyboard());
        MainActivity mainActivity = mActivityRule.getActivity();
        mainActivity.createAppNode();
        if(Build.MODEL.equals(SUPERVISOR_MODEL)&& Build.SERIAL.equals(SUPERVISOR_SERIAL))
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
                    DateUtils.SECOND_IN_MILLIS * STOP_TIME,SECONDARY_GROUP_ID);
    }



    public void initSupervisorAndWait(MainActivity mainActivity, long waitingTime, long startTime, long experimentTime,
                                      long stopStime) {

        // Make sure Espresso does not time out
        IdlingPolicies.setMasterPolicyTimeout(1000 * 1000, TimeUnit.MILLISECONDS);
        IdlingPolicies.setIdlingResourceTimeout(1000 * 1000, TimeUnit.MILLISECONDS);

        checkModel();

        mainActivity.createTestControlNode(DEVICES_NUMBER, true);
        Log.i(TAG, "Test_Supervisor: waiting for others");

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


        //all the nodes have joint
        //supervisor node starts to create  control and monitoring groups and becomes their supervisor
        onView(withId(startSensorButton)).perform(click());
        // Now we wait START_TIME for all the sensors to be connected
        Log.i(TAG, "Supervisor: wait for followers");
        waitFor(startTime*4);
        //End of Group Initialization
        Log.i(TAG, "GroupInitialization ended at: " + System.currentTimeMillis() );

        //now we are sure that all the followers are in the control group and we can ask them to start sending packets to this supervisor
        // Start the experiment by pressing the start button
        onView(withId(startMergeButton)).perform(click());
        //groupSplitStart=System.currentTimeMillis();
        EventBus.getDefault().post(new TestEvent(AppConstants.START_MERGE,"control",System.currentTimeMillis()));
        Log.i(TAG, "Merging groups started");

        // Now we wait EXPERIMENT_TIME for the experiment to run
        Log.i(TAG, "Server: waiting for the experiment to run");
        waitFor(experimentTime);


        Log.i(TAG,"Merge Group Ended");

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

    public void initFollowerAndWait(MainActivity mainActivity, long waitingTime, long startTime,
                                    long experimentTime, long stopTime, int secondaryGroupId) {

        //changes the group id that this node belongs to
        onView(withId(groupIdEditText)).perform(replaceText(String.valueOf(secondaryGroupId)));

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
        waitFor(startTime*2);

        // We start the sensor by pressing the button
        Log.i(TAG, "Follower: starting");
        onView(withId(startSensorButton)).perform(click());

        // Now we wait 1x START_TIME for this node to start as a follower
        waitFor(startTime);

        //wait for monitoring_1 supervisor disconnect to happen and a follower be replaced by old supervisor
        waitFor(experimentTime+stopTime);

        // Checks if this node has joint the group
        checkGroupAccession();
    }


    // This method will be called when a MessageEvent is posted
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void handleGroupEvent(A3GroupEvent event) {

    }

    //TestEvent
    @Subscribe (threadMode = ThreadMode.POSTING)
    public void handleTestEvent(TestEvent event){
        if (event.groupName.equals("control")) {
            switch (event.eventType){
                case AppConstants.START_MERGE:
                    groupMergeStart=(long) event.object;
                    Log.i(TAG, event.groupName+" merge Group start at: "+ event.object);
                    break;
                case AppConstants.JOINED:
                    //lastFollowerJoin=(long) event.object;
                    String[] message=event.object.toString().split("_");
                    lastFollowerJoin=Long.valueOf(message[5]);
                    Log.i(TAG,message[0]+"_"+message[1]+" group merge ended after milliseconds: "+(lastFollowerJoin-groupMergeStart)+" node: "+message[3]);
                    break;
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
