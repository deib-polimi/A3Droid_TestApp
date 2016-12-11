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


import it.polimi.deepse.a3droid.a3.events.A3GroupEvent;
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


import it.polimi.deepse.a3droid.a3.events.A3UIEvent;


//this test measures the time for merging all the members of a source group to a destination group.
// the result is time form the sending a request of the supervisor to the time that all members of the source group
//join the destination group (this would be the worst case scenario)

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
    private static final int EXPERIMENT_TIME = 60 * 1;
    private static final int STOP_TIME = 40;

    //// TODO: 11/29/2016 in a device farm, we have to decide about model and serial of supervisor device
    public final static String SUPERVISOR_MODEL = "Nexus 9";
    public final static String SUPERVISOR_SERIAL= "HT4BBJT00970";

    //public final static String SUPERVISOR_MODEL = "SM-P605";
    // public final static String SUPERVISOR_MODEL = "XT1052";
    private final static String SPV_MRG_STARTED_OUTPUT =  "Start of Group Merge";
    private final static String SPV_MRG_STOPPED_OUTPUT =  "End of Group Merge";
    //private final static String FLW_MRG_STARTED_OUTPUT ="Merge has started";
    //private final static String FLW_MRG_STOPPED_OUTPUT = "Merge has stopped";


    private long groupMergeStart=0;
    private long lastFollowerJoin=0;
    private int expectedNumberOfMergedFollowers=0;



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
        startSensorButton = R.id.button2;
        startMergeButton=R.id.button6;   //merge button
        groupIdEditText=R.id.editText1;//group ID edit text
        inEditText=R.id.oneInEditText;
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
        Log.i(TAG, "Starting GroupMerge Test");
        onView(withId(R.id.editText1)).perform(closeSoftKeyboard());
        MainActivity mainActivity = mActivityRule.getActivity();
        //mainActivity.createAppNode();
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
                    DateUtils.SECOND_IN_MILLIS * STOP_TIME,
                    SECONDARY_GROUP_ID);
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
            //suspends the main thread of application, but not the Espresso thread
            waitFor(waitingTime);
        }while(--counter > 0 && !mainActivity.isTestGroupReady());

        if(!mainActivity.isTestGroupReady()) {
            Log.w(TAG, "Supervisor: counter reached 0, test cancelled");
            return;
        }
        Log.i(TAG, "Supervisor: starting test with counter=" + counter);


        //all the nodes have joint
        //supervisor node starts to create control and monitoring groups and becomes their supervisor
        onView(withId(startSensorButton)).perform(click());
        // Now we wait START_TIME for all the sensors to be connected
        Log.i(TAG, "Supervisor: wait for followers");
        waitFor(startTime*4);


        //now we are sure that all the followers are in the control group and monitoring_1 and monitoring_2 groups
        //we can start to merge all the members of monitoring_2 to be the followers of monitoring_2 group
        onView(withId(startMergeButton)).perform(click());
        //an Event but to save the time that merge requested by control supervisor
        EventBus.getDefault().post(new TestEvent(AppConstants.START_MERGE,"control",System.currentTimeMillis()));
        Log.i(TAG, "Merging groups started");

        // Now we wait EXPERIMENT_TIME for the experiment to run
        Log.i(TAG, "Server: waiting for the experiment to run");
        waitFor(experimentTime);

        //Now, the merge process should be ended, checking test results

        // Check for the right role according to the device model
        onView(withId(R.id.oneInEditText))
                .check(matches(withPat(ROLE_OUTPUT)));





        //Check for the start merge output in the log
        checkGroupMergeAccession();

        checkGroupMergeEnd();


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

        //wait for monitoring_1 supervisor merge to happen and this node merges to monitoring_1
        waitFor(experimentTime+stopTime);

        // Checks if this node has moved from monitoring_1 to monitoring_2
         checkFollowerGroupMerge();
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
                    expectedNumberOfMergedFollowers=DEVICES_NUMBER-1;
                    break;
                case AppConstants.JOINED:
                    String[] message=event.object.toString().split("_");
                    String GroupID=message[1];
                    if(GroupID.equals("1")) {
                        lastFollowerJoin = Long.valueOf(message[5]);
                        Log.i(TAG, message[0] + "_" + message[1] + " group merge ended after milliseconds: " + (lastFollowerJoin - groupMergeStart) + " node: " + message[3]);
                        expectedNumberOfMergedFollowers--;
                        if(expectedNumberOfMergedFollowers ==0){
                            //this means all the expected followers have merged monitoring_1, write to CSV file
                            logResult(lastFollowerJoin - groupMergeStart);
                            EventBus.getDefault().post(new A3UIEvent(0, SPV_MRG_STOPPED_OUTPUT));
                        }
                    }
                    break;
            }
        }
    }



    private void logResult(long result){
        Log.i(TAG, "logResult: " + result);
        File resultFolder = Environment.getExternalStorageDirectory();
        File resultFile = new File(resultFolder, AppConstants.EXPERIMENT_PREFIX + "_GroupMergeTime_" + DEVICES_NUMBER + ".csv");
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

    private void checkGroupMergeAccession(){
        //onView(withId(R.id.oneInEditText)).check(matches(withPat(SPV_MRG_STARTED_OUTPUT)));
    }

    private void checkFollowerGroupMerge() {
        onView(withId(R.id.oneInEditText)).check(matches(withPat("monitoring_1_FolRole")));
    }

    private void checkGroupMergeEnd() {
        onView(withId(R.id.oneInEditText)).check(matches(withPat(SPV_MRG_STOPPED_OUTPUT)));
    }


}
