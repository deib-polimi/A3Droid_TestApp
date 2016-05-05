package it.polimi.greenhouse.a3.nodes;

import android.os.Handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import it.polimi.deepse.a3droid.A3Message;
import it.polimi.deepse.a3droid.A3Node;
import it.polimi.deepse.a3droid.GroupDescriptor;
import it.polimi.deepse.a3droid.UserInterface;
import it.polimi.greenhouse.util.AppConstants;

/**
 * created by seadev on 4/26/16.
 */
public class TestControlNode extends A3Node{

    int testSize;
    Handler activityHandler;

    private Set<String> membersSet;
    private String serverId = null;
    private boolean server;

    public TestControlNode(boolean server, int testSize, Handler activityHandler, String uuId, UserInterface ui, ArrayList<String> roles, ArrayList<GroupDescriptor> groupDescriptors) {
        super(uuId, ui, roles, groupDescriptors);
        this.testSize = testSize;
        this.activityHandler = activityHandler;
        this.membersSet = Collections.synchronizedSet(new TreeSet<String>());
        this.server = server;
    }

    //Used by supervisor
    public void addMember(String s, boolean server){
        s = s.replaceFirst("\\.[A-Za-z0-9]+", "");
        if(server)
            this.serverId = s;
        else
            membersSet.add(s);
        showOnScreen("Test member " + s + " entered");
        if(isGroupReady())
            new TestStarter().start();
    }

    public void removeMember(String s){
        s = s.replaceFirst("\\.[A-Za-z0-9]+", "");
        membersSet.remove(s);
        showOnScreen("Test member " + s + " exited");
    }

    public boolean isGroupReady(){
        return serverId != null && membersSet.size() >= testSize - 1;
    }

    //Used by follower
    public void setGroupReady(boolean ready){
        activityHandler.sendMessage(activityHandler.obtainMessage(1, ready));
    }

    public class TestStarter extends Thread {

        public void run() {
            if(isGroupReady()) {
                sendNews();
            }
        }

        private String groupList(){
            StringBuilder sb = new StringBuilder();
            List<String> ids = new ArrayList<>();
            ids.add(0, serverId);
            ids.addAll(membersSet);

            for(int i = 0; i < testSize && i < ids.size(); i++){
                sb.append(ids.get(i));
                if(i < testSize - 1 && i < ids.size() - 1)
                    sb.append("_");
            }
            return sb.toString();
        }

        private void sendNews(){
            sendBroadcast(new A3Message(AppConstants.TEST_GROUP_READY, groupList()), "test_control");
            try {
                Thread.sleep(5 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            activityHandler.sendMessage(activityHandler.obtainMessage(1, true));
        }
    }

    public boolean isServer(){
        return server;
    }
}
