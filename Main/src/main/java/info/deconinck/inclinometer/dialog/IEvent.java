package info.deconinck.inclinometer.dialog;

public interface IEvent {

    /**
     * event type
     */
    String getEventType();

    void setEventType(String eventType);

    /**
     * event data
     */
    Object getData();

    void setData(Object data);
}
