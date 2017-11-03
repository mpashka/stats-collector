package org.github.mpashka;

/**
 * Created by pmoukhataev on 17.10.17.
 */
public class EventId {
    private int id;
    private String name;
    private EventType type;

    public EventId(int id, String name, EventType type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public EventType getType() {
        return type;
    }
}
