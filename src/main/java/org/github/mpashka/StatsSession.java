package org.github.mpashka;

/**
 */
public class StatsSession {

    private static final EventId END = StatsCollector.createEventId("end", EventType.end);

    private static final int MAX_EVENTS = 100;

    private long start;
    private long statsPeriodMs;
    private int[] ids = new int[MAX_EVENTS];
    private long[] times = new long[MAX_EVENTS];
    private int number = 0;
    private TimeProvider timeProvider;

    StatsSession(TimeProvider timeProvider, long statsPeriodMs) {
        this.timeProvider = timeProvider;
        start = timeProvider.getTime();
        this.statsPeriodMs = statsPeriodMs;
    }

    public void event(EventId eventId) {
        if (number < MAX_EVENTS-1) {
            ids[number] = eventId.getId();
            times[number] = timeProvider.getTime() - start;
            number++;
        }
    }

    public void end() {
        event(END);
        StatsCollector.getStatsCollector().endSession(ids, times, number, times[number-1] + start, statsPeriodMs);
    }

    public interface TimeProvider {
        long getTime();
    }
}
