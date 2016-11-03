package it.polimi.greenhouse;

import android.support.test.espresso.IdlingResource;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.test.ApplicationTestCase;
import android.view.View;
import android.widget.TextView;

import org.hamcrest.Description;
import org.hamcrest.Matcher;

import java.util.regex.Pattern;

import it.polimi.greenhouse.activities.MainActivity;

import static org.hamcrest.Matchers.is;

public abstract class TestBase {

    protected int startServerButton;
    protected int startExpertimentButton;
    protected int stopExpertimentButton;
    protected int startSensorButton;

    public abstract void initialize();

    public abstract void initValidString();

    public abstract void resetTimeout();

    public abstract void testDevice();


    /**
     * Returns a matcher that matches {@link TextView} based on its text property value. Note: View's
     * Sugar for withText(is("string")).
     *
     * @param text {@link String} with the text to match
     */
    public static Matcher<View> withPat(String text) {
        return withPat(is(text), text);
    }

    /**
     * Returns a matcher that matches {@link TextView}s based on text property value. Note: View's
     * text property is never null. If you setText(null) it will still be "". Do not use null matcher.
     *
     * @param stringMatcher
     *     <a href="http://hamcrest.org/JavaHamcrest/javadoc/1.3/org/hamcrest/Matcher.html">
     *     <code>Matcher</code></a> of {@link String} with text to match
     */
    public static Matcher<View> withPat(final Matcher<String> stringMatcher, final String textPattern) {
        return new BoundedMatcher<View, TextView>(TextView.class) {
            @Override
            public void describeTo(Description description) {
                description.appendText("with pattern: ");
                stringMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(TextView textView) {
                String text = textView.getText().toString();
                Pattern pattern = Pattern.compile(textPattern);
                java.util.regex.Matcher matcher = pattern.matcher(text);
                if(matcher.find())
                    return stringMatcher.matches(matcher.group(0));
                else
                    return false;
            }
        };

    }

    protected void waitFor(long waitingTime){
        try {
            Thread.sleep(waitingTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }





    class ElapsedTimeIdlingResource implements IdlingResource {
        private final long startTime;
        private final long waitingTime;
        private ResourceCallback resourceCallback;

        public ElapsedTimeIdlingResource(long waitingTime) {
            this.startTime = System.currentTimeMillis();
            this.waitingTime = waitingTime;
        }

        @Override
        public String getName() {
            return ElapsedTimeIdlingResource.class.getName() + ":" + waitingTime;
        }

        @Override
        public boolean isIdleNow() {
            long elapsed = System.currentTimeMillis() - startTime;
            boolean idle = (elapsed >= waitingTime);
            if (idle) {
                resourceCallback.onTransitionToIdle();
            }
            return idle;
        }

        @Override
        public void registerIdleTransitionCallback(ResourceCallback resourceCallback) {
            this.resourceCallback = resourceCallback;
        }
    }
}
