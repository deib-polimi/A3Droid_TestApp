package it.polimi.greenhouse.a3.roles;

import it.polimi.deepse.a3droid.a3.A3Message;
import it.polimi.deepse.a3droid.a3.A3SupervisorRole;
import it.polimi.greenhouse.a3.nodes.TestControlNode;
import it.polimi.greenhouse.util.AppConstants;


public class TestControlSupervisorRole  extends A3SupervisorRole{

    @Override
    public void onActivation() {
        //showOnScreen("[TestGroupCtrlSupRole]");
        ((TestControlNode) node).addMember(getChannelId(), ((TestControlNode) node).isServer());
    }

    @Override
    public void logic() {}

    @Override
    public void receiveApplicationMessage(A3Message a3Message) {
        switch (a3Message.reason){
            case AppConstants.MEMBER_ADDED:
                ((TestControlNode) node).addMember(a3Message.senderAddress, Boolean.parseBoolean(a3Message.object));
                break;

            case AppConstants.MEMBER_REMOVED:
                ((TestControlNode) node).removeMember(a3Message.senderAddress);
                break;

            default:
                break;
        }
    }

    public void memberAdded(String s) {
        //((TestControlNode) node).addMember(s);
    }

    public void memberRemoved(String s) {
        //((TestControlNode) node).removeMember(s);
    }
}