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
import it.polimi.deepse.a3droid.a3.A3Message;
import it.polimi.deepse.a3droid.a3.events.A3GroupEvent;
import it.polimi.deepse.a3droid.a3.events.A3UIEvent;
import it.polimi.deepse.a3droid.a3.exceptions.A3ChannelNotFoundException;
import it.polimi.deepse.a3droid.a3.exceptions.A3NoGroupDescriptionException;
import it.polimi.deepse.a3droid.a3.exceptions.A3SupervisorNotElectedException;
import it.polimi.greenhouse.a3.events.TestEvent;
import it.polimi.greenhouse.a3.groups.TestControlDescriptor;
import it.polimi.greenhouse.activities.MainActivity;
import it.polimi.greenhouse.util.AppConstants;
import it.polimit.greenhouse.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

//this test measures the time for each follower to join the group's supervisor
//by increasing the number simultaneous joining followers from one to ten to study its impact

@RunWith(AndroidJUnit4.class)
@LargeTest

public class TestGroupAccession extends TestBase{

    private final String TAG = "TestGroupAccession";
    private static final int DEVICES_NUMBER =10;

    private static String ROLE_OUTPUT;
    private static final int WAITING_TIME = 5;
    private static final int WAITING_COUNT = 60;
    private static final int START_TIME = 10;
    private static final int EXPERIMENT_TIME =60*2 ;
    private static final int STOP_TIME = 40;

    //// TODO: 11/29/2016 in a device farm, we have to decide about model and serial of supervisor device
    // public final static String SUPERVISOR_MODEL = "XT1052";
   // public final static String SUPERVISOR_MODEL = "Nexus 9";
    public final static String SUPERVISOR_SERIAL= "HT4BBJT00970";//a71

    private final static String FLW_GA_STARTED_OUTPUT =  "Start of Group Accession";
    private final static String FLW_GA_STOPPED_OUTPUT = "End of Group Accession";
    //private final static String FLW_EXP_STARTED_OUTPUT ="Experiment has started";
    //private final static String FLW_EXP_STOPPED_OUTPUT = "Experiment has stopped";

    private long groupInitializationStart = 0;
    private volatile int expectedNumberofFolArrivals;
    private volatile String result;
    MainActivity mainActivity;


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
        Log.i(TAG, "Starting Group Accession Test");
        onView(withId(R.id.editText1)).perform(closeSoftKeyboard());
        mainActivity = mActivityRule.getActivity();
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
        expectedNumberofFolArrivals=DEVICES_NUMBER-1;
        result="";

        checkModel();

        mainActivity.createTestControlNode(DEVICES_NUMBER, true);
        //connect to testcontrol group as its supervisor

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
        waitFor(10*1000);



        //Connect this node to Control group, this node would be the supervisor of Control group
        waitFor(startTime);
        try {
            mainActivity.getAppNode().connectAndWaitForActivation("control");
        } catch (A3NoGroupDescriptionException e) {
            Log.e(TAG, e.getMessage());
            return;
        } catch (A3ChannelNotFoundException e) {
            e.printStackTrace();
        }


        // Now we wait START_TIME for all the sensors to be connected
        Log.i(TAG, "Supervisor: wait for followers");
        waitFor(experimentTime);

        //check if this node has became a supervisor
        checkSuprvisorGroupAccession();
    }

    public void initFollowerAndWait(MainActivity mainActivity, long waitingTime, long startTime, long experimentTime, long stopTime) {

        // Make sure Espresso does not time out
        IdlingPolicies.setMasterPolicyTimeout(1000 * 1000, TimeUnit.MILLISECONDS);
        IdlingPolicies.setIdlingResourceTimeout(1000 * 1000, TimeUnit.MILLISECONDS);

        checkModel();

        //Waits for a 0 - 10 seconds to avoid too many simultaneous devices joining the same group
        Log.i(TAG, "Follower:  wait to connect to test_control");
        long randomWait = (long) (DateUtils.SECOND_IN_MILLIS * Math.random() * WAITING_TIME * 2);
        waitFor(randomWait);


        Log.i(TAG, "Follower: waiting for others");
        mainActivity.createTestControlNode(DEVICES_NUMBER, false);
        //connect to testcontrol group as its follower


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
        waitFor(10*1000);


        // Now we wait 1x START_TIME for the supervisor to start
        waitFor(startTime*3);

        long randomWait2 = (long) (DateUtils.SECOND_IN_MILLIS * Math.random() * WAITING_TIME * 2);
        waitFor(randomWait2);

        // We start the sensor by pressing the button
        Log.i(TAG, "Follower: starting");
        //onView(withId(startSensorButton)).perform(click());


            EventBus.getDefault().post(new A3UIEvent(0, FLW_GA_STARTED_OUTPUT));
            //we record the time when this follower starts to join the control group

            groupInitializationStart = System.currentTimeMillis();

        try {
            mainActivity.getAppNode().connectAndWaitForActivation("control");
        } catch (A3ChannelNotFoundException e) {
            Log.i(TAG," saeed connection to control failed");
            e.printStackTrace();
        } catch (A3NoGroupDescriptionException e) {
            e.printStackTrace();
        }


        // Now we wait 1x START_TIME for this node to start as a follower
        waitFor(experimentTime);

        // Checks if this node has became a follower
        checkFollowerGroupAccession();
    }


    // This method will be called when a MessageEvent is posted
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void handleGroupEvent(A3GroupEvent event) {

        if(event.groupName.equals("control")) {
            switch (event.eventType) {
                case GROUP_STATE_CHANGED:
                    if(event.object == A3GroupDescriptor.A3GroupState.ACTIVE)
                        if(!Build.SERIAL.equals(SUPERVISOR_SERIAL)) {
                            //follower received the confirmation of Control supervisor about its membership to group
                            //follower sends data to supervisor to be written in a CSV file at Supervisor
                            EventBus.getDefault().post(new A3UIEvent(0, FLW_GA_STOPPED_OUTPUT));
                            long result = System.currentTimeMillis() - groupInitializationStart;
                            try {
                                mainActivity.getAppNode().sendToSupervisor(
                                        new A3Message(AppConstants.FOLLOWER_ACCESSION,String.valueOf(result))
                                        ,"control");
                            } catch (A3SupervisorNotElectedException e) {
                                e.printStackTrace();
                            }
                        }
                    break;

                case GROUP_JOINED:
                    break;

                default:
                    break;
            }
        }
    }


    @Subscribe(threadMode = ThreadMode.POSTING)
    public void handleTestEvent(TestEvent event){
        if(event.groupName.equals("control")){
            switch (event.eventType){
                case AppConstants.FOLLOWER_ACCESSION:
                    expectedNumberofFolArrivals--;
                    result+=event.object.toString()+"\n";
                    if(expectedNumberofFolArrivals == 0 && !result.equals("")) {
                        //log the time that follower joined the control group
                        EventBus.getDefault().post(new A3UIEvent(0,"All Expected Fols arrived"));
                        logResult();
                    }
                    break;
            }

        }
    }

//logs the test results on supervisor device
    private void logResult(){
        Log.i(TAG, "logResult: " + result);
        File resultFolder = Environment.getExternalStorageDirectory();
        File resultFile = new File(resultFolder, AppConstants.EXPERIMENT_PREFIX + "_GroupAccessionTime_" + DEVICES_NUMBER + ".csv");
        try {
            FileWriter fw = new FileWriter(resultFile, true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(result);
            bw.flush();
        } catch (IOException e) {
            Log.e(TAG, "cannot write the log file "+e.getLocalizedMessage());
        }
    }






    private void checkModel(){
        onView(withId(R.id.oneInEditText))
                .check(matches(withPat(Build.MANUFACTURER)));
    }

    private void checkSuprvisorGroupAccession(){
        onView(withId(R.id.oneInEditText)).check(matches(withPat("CtrlSupRole")));
        onView(withId(R.id.oneInEditText)).check(matches(withPat("All Expected Fols arrived")));
    }

    private void checkFollowerGroupAccession(){
        onView(withId(R.id.oneInEditText)).check(matches(withPat("CtrlFolRole")));
    }


}
