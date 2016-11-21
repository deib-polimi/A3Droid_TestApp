package it.polimi.greenhouse;

/**
 * Created by saeed on 11/16/2016.
 */

import android.os.Environment;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.junit.After;
import org.junit.runner.RunWith;



import android.os.Build;
import android.support.test.espresso.IdlingPolicies;
import android.support.test.rule.ActivityTestRule;
import android.text.format.DateUtils;
import android.util.Log;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import it.polimi.deepse.a3droid.a3.A3GroupDescriptor;
import it.polimi.deepse.a3droid.a3.events.A3GroupEvent;
import it.polimi.deepse.a3droid.a3.exceptions.A3NoGroupDescriptionException;
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

public class TestGroupInitialization extends TestBase{

    private final String TAG = "TestGroupInitialization";

    private static final int DEVICES_NUMBER = 1;

    private static String ROLE_OUTPUT;
    private static final int WAITING_TIME = 5;
    private static final int WAITING_COUNT = 60;
    private static final int START_TIME = 10;
    private static final int EXPERIMENT_TIME = 60 * 5;
    private static final int STOP_TIME = 40;

    //public final static String SUPERVISOR_MODEL = "SM-P605";
    public final static String SUPERVISOR_MODEL = "XT1052";
    private final static String SPV_EXP_STARTED_OUTPUT =  "Start of Expriment";
    private final static String SPV_EXP_STOPPED_OUTPUT = "End of Expriment";
    private final static String FLW_EXP_STARTED_OUTPUT ="Experiment has started";
    private final static String FLW_EXP_STOPPED_OUTPUT = "Experiment has stopped";

    private long groupInitializationStart = 0;

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
        Log.i(TAG, "Starting the test GroupInitialization");
        onView(withId(R.id.editText1)).perform(closeSoftKeyboard());
        MainActivity mainActivity = mActivityRule.getActivity();
        mainActivity.createAppNode();
        if(Build.MODEL.equals(SUPERVISOR_MODEL))
            initSupervisorAndWait(mainActivity,
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
            //suspends the main thread of application, but not the Espresso thread
            waitFor(waitingTime);
        }while(--counter > 0 && !mainActivity.isTestGroupReady());

        if(!mainActivity.isTestGroupReady()) {
            Log.w(TAG, "Supervisor: counter reached 0, test cancelled");
            return;
        }

        Log.i(TAG, "Supervisor: starting test with counter=" + counter);

        //onView(withId(startSensorButton)).perform(click());
        try {
            groupInitializationStart = System.currentTimeMillis();
            mainActivity.getAppNode().connect("control");
        } catch (A3NoGroupDescriptionException e) {
            Log.e(TAG, e.getMessage());
            return;
        }

        Log.i(TAG, "GroupInitialization started at: " + groupInitializationStart);

        // Now we wait START_TIME for all the sensors to be connected
        Log.i(TAG, "Supervisor: wait for sensors");
        waitFor(startTime);

        // Checks if this node has joint the group
        checkGroupAccession();

        //End of Group Initialization
        Log.i(TAG, "GroupInitialization ended at: " + System.currentTimeMillis() );
    }


    // This method will be called whenntrol a MessageEvent is posted
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void handleGroupEvent(A3GroupEvent event) {
        if(event.groupName.equals("control"))
            if(event.eventType.equals(A3GroupEvent.A3GroupEventType.GROUP_STATE_CHANGED))
                if(event.object == A3GroupDescriptor.A3GroupState.ACTIVE)
                    logResult(System.currentTimeMillis() - groupInitializationStart);

    }

    private void logResult(long result){
        File resultFolder = Environment.getExternalStorageDirectory();
        File resultFile = new File(resultFolder, AppConstants.EXPERIMENT_PREFIX + "_GroupInitializationTime_" + DEVICES_NUMBER + ".csv");
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
        onView(withId(R.id.oneInEditText))
                .check(matches(withPat("CtrlSupRole")));
    }


}
