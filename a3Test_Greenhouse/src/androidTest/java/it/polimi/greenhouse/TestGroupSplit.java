package it.polimi.greenhouse;



import android.os.Build;
import android.os.Environment;
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
import it.polimi.deepse.a3droid.a3.events.A3GroupEvent;
import it.polimi.deepse.a3droid.a3.events.A3UIEvent;
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
public class TestGroupSplit extends TestBase{

    private final String TAG = "TestGroupSplit";

    private static final int DEVICES_NUMBER = 7;
    private static final int NUMBER_DEVICES_TO_SPLIT=6;
    //private static final int DEVICES_TO


    private static String ROLE_OUTPUT;
    private static final int WAITING_TIME = 5;
    private static final int WAITING_COUNT = 60;
    private static final int START_TIME = 10;
    private static final int EXPERIMENT_TIME = 90 * 1;
    private static final int STOP_TIME = 40;

    //// TODO: 11/29/2016 in a device farm, we have to decide about model and serial of supervisor device
   // public final static String SUPERVISOR_MODEL = "Nexus 9";
  //  public final static String SUPERVISOR_SERIAL= "17e29a96";
  //  public final static String SUPERVISOR_MODEL = "SM-P605";

    public final static String SUPERVISOR_MODEL = "Nexus 9";
    public final static String SUPERVISOR_SERIAL= "HT4BBJT00970";
    // public final static String SUPERVISOR_MODEL = "XT1052";
    private final static String SPV_EXP_STARTED_OUTPUT =  "Start of Expriment";
    private final static String SPV_EXP_STOPPED_OUTPUT = "End of Expriment";
    private final static String FLW_EXP_STARTED_OUTPUT ="Experiment has started";
    private final static String FLW_EXP_STOPPED_OUTPUT = "Experiment has stopped";

    private long groupInitializationStart = 0;
    private long groupSplitStart=0;
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
        startSplitButton=R.id.button5;

        splitNumberofNodesEditText=R.id.editTextSplitNumber;

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
        Log.i(TAG, "Starting the test Group Split");
        onView(withId(R.id.editText1)).perform(closeSoftKeyboard());
        MainActivity mainActivity = mActivityRule.getActivity();
        mainActivity.createAppNode();
        if(Build.MODEL.equals(SUPERVISOR_MODEL)&& Build.SERIAL.equals(SUPERVISOR_SERIAL))
            initSupervisorAndWait(mainActivity,
                    DateUtils.SECOND_IN_MILLIS * WAITING_TIME,
                    DateUtils.SECOND_IN_MILLIS * START_TIME,
                    DateUtils.SECOND_IN_MILLIS * EXPERIMENT_TIME,
                    DateUtils.SECOND_IN_MILLIS * STOP_TIME,
                    NUMBER_DEVICES_TO_SPLIT);
        else
            initFollowerAndWait(mainActivity,
                    DateUtils.SECOND_IN_MILLIS * WAITING_TIME,
                    DateUtils.SECOND_IN_MILLIS * START_TIME,
                    DateUtils.SECOND_IN_MILLIS * EXPERIMENT_TIME,
                    DateUtils.SECOND_IN_MILLIS * STOP_TIME);
    }



    public void initSupervisorAndWait(MainActivity mainActivity, long waitingTime,
                                      long startTime, long experimentTime, long stopStime, int numberDevicesToSplit) {


        //changes the number of devices that should be departure from the original group to a secondary one
        onView(withId(splitNumberofNodesEditText)).perform(replaceText(String.valueOf(numberDevicesToSplit)));


        // Make sure Espresso does not time out
        IdlingPolicies.setMasterPolicyTimeout(1000 * 1000, TimeUnit.MILLISECONDS);
        IdlingPolicies.setIdlingResourceTimeout(1000 * 1000, TimeUnit.MILLISECONDS);



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

        Log.i(TAG, "Supervisor: starting test with counter=" + counter);

        try {
            mainActivity.getTestAppNode().disconnect("test_control");
        } catch (A3ChannelNotFoundException e) {
            e.printStackTrace();
        }
        waitFor(startTime*2);




        //supervisor node starts to create  control and monitoring groups and becomes their supervisor
        onView(withId(startSensorButton)).perform(click());
        // Now we wait START_TIME for all the sensors to be connected
        Log.i(TAG, "Supervisor: wait for followers");
        waitFor(startTime*6);


        //now we are sure that all the followers are in the control group and we can split the gorup
        onView(withId(startSplitButton)).perform(click());

        EventBus.getDefault().post(new TestEvent(AppConstants.START_SPLIT,"control",System.currentTimeMillis()));
        Log.i(TAG, "Splitting group into "+numberDevicesToSplit+" groups started");

        // Now we wait EXPERIMENT_TIME for the experiment to run
        Log.i(TAG, "Server: waiting for the experiment to run");
        waitFor(experimentTime);


        Log.i(TAG,"Split Group Ended");

        try {
            checkPrimarySupGroup();
            //test passed -> log result
            Log.i(TAG, "After test, loging results:"+(lastFollowerJoin - groupSplitStart));
            logResult(lastFollowerJoin - groupSplitStart);

        }catch (AssertionFailedError error){
            //test failed
            return;
        }


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

        Log.i(TAG, "Follower: starting test with counter=" + counter);

        try {
            mainActivity.getTestAppNode().disconnect("test_control");
        } catch (A3ChannelNotFoundException e) {
            e.printStackTrace();
        }
        waitFor(startTime*2);

        // Now we wait 1x START_TIME for the supervisor to start
        waitFor(startTime*2);

        // We start the sensor by pressing the button
        Log.i(TAG, "Follower: starting");
        onView(withId(startSensorButton)).perform(click());



        //wait for monitoring_1 supervisor disconnect to happen and a follower be replaced by old supervisor
        waitFor(startTime * 4 + experimentTime);

        // Checks if this node has joint the group
        checkGroupSplit();
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
                case AppConstants.START_SPLIT:
                    groupSplitStart=(long) event.object;
                    Log.i(TAG, event.groupName+" Split Group at: "+ event.object);
                    break;
                case AppConstants.JOINED:
                    //lastFollowerJoin=(long) event.object;
                    String[] message=event.object.toString().split("_");
                    String givenRole = event.object.toString().split("#")[1];
                    if(("monitoring_"+message[1]+"_"+message[2]).equals("monitoring_1_1")) {
                        lastFollowerJoin = System.currentTimeMillis();
                        EventBus.getDefault().post(new A3UIEvent(0,"monitoring_1_1_"+givenRole+"_Arrived"));
                        Log.i(TAG, "group reshaped again after milliseconds: " + (lastFollowerJoin - groupSplitStart) +
                                " node: " + message[4]+" Role: "+givenRole);
                    }
                    break;
            }
        }
    }

    private void logResult(long result){
        Log.i(TAG, "logResult: " + result);
        File resultFolder = Environment.getExternalStorageDirectory();
        File resultFile = new File(resultFolder, AppConstants.EXPERIMENT_PREFIX + "_GroupSplitTime_" + DEVICES_NUMBER + ".csv");
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

    private void checkPrimarySupGroup(){
            onView(withId(R.id.oneInEditText)).check(matches(withPat("CtrlSupRole")));
            onView(withId(R.id.oneInEditText)).check(matches(withPat("monitoring_1_SupRole")));
            onView(withId(R.id.oneInEditText)).check(matches(withPat("monitoring_1_1_SupRole")));
        if(NUMBER_DEVICES_TO_SPLIT>=2)
            onView(withId(R.id.oneInEditText)).check(matches(withPat("monitoring_1_1_FolRole")));
    }

    private void checkGroupSplit(){
        onView(withId(R.id.oneInEditText)).check(matches(withPat("CtrlFolRole")));
        //onView(withId(R.id.oneInEditText)).check(matches(withPat("monitoring_1_FolRole] deactivated")));
        //onView(withId(R.id.oneInEditText)).check(matches(withPat("monitoring_1_1")));
    }
}
