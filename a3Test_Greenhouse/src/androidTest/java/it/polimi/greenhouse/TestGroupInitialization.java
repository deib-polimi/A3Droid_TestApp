package it.polimi.greenhouse;

/**
 * Created by saeed on 11/16/2016.
 */

import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.junit.runner.RunWith;



import android.os.Build;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingPolicies;
import android.support.test.espresso.IdlingResource;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.text.format.DateUtils;
import android.util.Log;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

import it.polimi.deepse.a3droid.a3.events.A3GroupEvent;
import it.polimi.greenhouse.activities.MainActivity;
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

    private static final int DEVICES_NUMBER = 2;

    private static String ROLE_OUTPUT;
    private static final int WAITING_TIME = 5;
    private static final int WAITING_COUNT = 60;
    private static final int START_TIME = 20;
    private static final int EXPERIMENT_TIME = 60 * 5;
    private static final int STOP_TIME = 40;

    public final static String SUPERVISOR_MODEL = "SM-P605";
    //public final static String SUPERVISOR_MODEL = "XT1052";
    private final static String SPV_EXP_STARTED_OUTPUT =  "Start of Expriment";
    private final static String SPV_EXP_STOPPED_OUTPUT = "End of Expriment";
    private final static String FLW_EXP_STARTED_OUTPUT ="Experiment has started";
    private final static String FLW_EXP_STOPPED_OUTPUT = "Experiment has stopped";

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(
            MainActivity.class);

    @Before
    public void initialize(){
        initIds();
        initValidString();
        resetTimeout();
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

        if(Build.MODEL.equals(SUPERVISOR_MODEL))
            initServerAndWait(mainActivity,
                    DateUtils.SECOND_IN_MILLIS * WAITING_TIME,
                    DateUtils.SECOND_IN_MILLIS * START_TIME,
                    DateUtils.SECOND_IN_MILLIS * EXPERIMENT_TIME,
                    DateUtils.SECOND_IN_MILLIS * STOP_TIME);
        else
            initSensorAndWait(mainActivity,
                    DateUtils.SECOND_IN_MILLIS * WAITING_TIME,
                    DateUtils.SECOND_IN_MILLIS * START_TIME,
                    DateUtils.SECOND_IN_MILLIS * EXPERIMENT_TIME,
                    DateUtils.SECOND_IN_MILLIS * STOP_TIME);
    }



    public void initServerAndWait(MainActivity mainActivity, long waitingTime, long startTime, long experimentTime, long stopStime) {
        // Type text and then press the button.
        //onView(withId(R.id.editText1))
        //        .perform(typeText(roleStringOutput), closeSoftKeyboard());


        // Make sure Espresso does not time out
        IdlingPolicies.setMasterPolicyTimeout(1000 * 1000, TimeUnit.MILLISECONDS);
        IdlingPolicies.setIdlingResourceTimeout(1000 * 1000, TimeUnit.MILLISECONDS);

        mainActivity.createTestControlGroup(DEVICES_NUMBER, true);
        Log.i(TAG, "Server: waiting for others");

        int counter = WAITING_COUNT;
        IdlingResource idlingResource = null;
        do{
            if(idlingResource != null)
                Espresso.unregisterIdlingResources(idlingResource);
            idlingResource = new ElapsedTimeIdlingResource(waitingTime);
            Espresso.registerIdlingResources(idlingResource);
            //checkModel();
            //suspends the main thread of application, but not the Espresso thread
            waitFor(waitingTime);
        }while(--counter > 0 && !mainActivity.isTestGroupReady());

        if(!mainActivity.isTestGroupReady()) {
            Espresso.unregisterIdlingResources(idlingResource);
            Log.w(TAG, "Server: counter reached 0, test cancelled");
            return;
        }

        Log.i(TAG, "Server: starting test with counter=" + counter);

        onView(withId(startServerButton)).perform(click());
        Log.i(TAG,"GroupInitialization started at: " +System.currentTimeMillis() );

        // Now we wait 3x START_TIME for all the sensors to be connected
        // TODO: check if 3x is enough
        Log.i(TAG, "Server: wait for sensors");
        IdlingResource idlingResource2 = new ElapsedTimeIdlingResource(startTime * 3);
        Espresso.registerIdlingResources(idlingResource2);

        //End of Group Initialization
        Log.i(TAG,"GroupInitialization ended at: " +System.currentTimeMillis() );
        checkModel();

        /* Start the experiment by pressing the start button
        Log.i(TAG, "Server: starting experiment");
        onView(withId(startExpertimentButton)).perform(click());

        // Now we wait EXPERIMENT_TIME for the experiment to run
        Log.i(TAG, "Server: waiting for the experiment to run");
        IdlingResource idlingResource3 = new ElapsedTimeIdlingResource(experimentTime);
        Espresso.registerIdlingResources(idlingResource3);

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
        Espresso.unregisterIdlingResources(idlingResource);
        Espresso.unregisterIdlingResources(idlingResource2);
       // Espresso.unregisterIdlingResources(idlingResource3);
        //Espresso.unregisterIdlingResources(idlingResource4);
    }

    public void initSensorAndWait(MainActivity mainActivity, long waitingTime, long startTime, long experimentTime, long stopTime) {
        // Type text and then press the button.
        //onView(withId(R.id.editText1))
        // .perform(typeText(roleStringOutput), closeSoftKeyboard());

        // Make sure Espresso does not time out
        IdlingPolicies.setMasterPolicyTimeout(1000 * 1000, TimeUnit.MILLISECONDS);
        IdlingPolicies.setIdlingResourceTimeout(1000 * 1000, TimeUnit.MILLISECONDS);

        //Waits for a 0 - 10 seconds to avoid too many simultaneous devices joining the same group
        Log.i(TAG, "Sensor: random wait to connect");
        long randomWait = (long) (DateUtils.SECOND_IN_MILLIS * Math.random() * WAITING_TIME * 2);
        IdlingResource idlingResource = new ElapsedTimeIdlingResource(randomWait);
        Espresso.registerIdlingResources(idlingResource);
        checkModel();

        Log.i(TAG, "Sensor: waiting for others");
        mainActivity.createTestControlGroup(DEVICES_NUMBER, false);

        int counter = WAITING_COUNT;
        do{
            if(idlingResource != null)
                Espresso.unregisterIdlingResources(idlingResource);
            idlingResource = new ElapsedTimeIdlingResource(waitingTime);
            Espresso.registerIdlingResources(idlingResource);
            //checkModel();
            waitFor(waitingTime);
        }while(--counter > 0 && !mainActivity.isTestGroupReady());

        if(!mainActivity.isTestGroupReady()) {
            Log.w(TAG, "Sensor: counter reached 0, test cancelled");
            Espresso.unregisterIdlingResources(idlingResource);
            return;
        }

        Log.i(TAG, "Sensor: starting test with counter=" + counter);

        // Now we wait 1x START_TIME for the server to start
        IdlingResource idlingResource2 = new ElapsedTimeIdlingResource(startTime);
        Espresso.registerIdlingResources(idlingResource2);

        checkModel();

        // We start the sensor by pressing the button
        Log.i(TAG, "Sensor: starting");
        onView(withId(startSensorButton)).perform(click());

        // Now we wait 1x START_TIME for the sensor to start
        // TODO: check if 1x is enough
        //IdlingResource idlingResource2 = new ElapsedTimeIdlingResource(START_TIME);
        //Espresso.registerIdlingResources(idlingResource2);

        // Now we wait EXPERIMENT_TIME + STOP_TIME * 2 for the experiment to finish and to supervisor receive all the data
        Log.i(TAG, "Sensor: wait for experiment to run and stop");
        IdlingResource idlingResource3 = new ElapsedTimeIdlingResource(experimentTime + stopTime * 2);
        Espresso.registerIdlingResources(idlingResource3);

        // Check for the right role according to the device model
        Log.i(TAG, "Sensor: check role");
        onView(withId(R.id.oneInEditText))
                .check(matches(withPat(ROLE_OUTPUT)));

        // Check for the start experiment output in the log
        Log.i(TAG, "Sensor: check experiment start");
        onView(withId(R.id.oneInEditText))
                .check(matches(withPat(FLW_EXP_STARTED_OUTPUT)));

        // Check for the stop experiment output in the log
        Log.i(TAG, "Sensor: check experiment stop");
        onView(withId(R.id.oneInEditText))
                .check(matches(withPat(FLW_EXP_STOPPED_OUTPUT)));

        // Clean up
        Espresso.unregisterIdlingResources(idlingResource);
        Espresso.unregisterIdlingResources(idlingResource2);
        Espresso.unregisterIdlingResources(idlingResource3);
    }


    // This method will be called when a MessageEvent is posted
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void handleGroupEvent(A3GroupEvent event) {
        Log.i(TAG, event.toString());
    }

    private void checkModel(){
        // Check for the model
        onView(withId(R.id.oneInEditText))
                .check(matches(withPat(Build.MANUFACTURER)));
    }

}
