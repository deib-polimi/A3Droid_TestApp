package it.polimi.greenhouse.a3.events;

/**
 * TODO
 */
public class TestEvent {

    public final String groupName;
    public final int eventType;
    public final Object object;

    public TestEvent(int eventType, String groupName){
        this.groupName = groupName;
        this.eventType = eventType;
        this.object = null;
    }

    public TestEvent(int eventType, String groupName, Object object){
        this.groupName = groupName;
        this.eventType = eventType;
        this.object = object;
    }

    public String toString(){
        return this.groupName + "_" + this.eventType;
    }
}
