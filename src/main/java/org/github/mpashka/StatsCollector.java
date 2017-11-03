package org.github.mpashka;

/**
 * Created by pmoukhataev on 17.10.17.
 */
public class StatsCollector {

    private static final StatsCollectorImpl STATS_COLLECTOR = new StatsCollectorImpl();
    private static final StatsSession.TimeProvider TIME_PROVIDER = System::currentTimeMillis;
    private static final long DEFAULT_STATS_PERIOD_MS = 3 * 60_000;

    public static EventId createEventId(String name) {
        return STATS_COLLECTOR.createEventId(name, EventType.event);
    }

    public static EventId createEventId(String name, EventType type) {
        return STATS_COLLECTOR.createEventId(name, type);
    }

    public static StatsSession createSession() {
        return createSession(DEFAULT_STATS_PERIOD_MS);
    }

    public static StatsSession createSession(long statsPeriodMs) {
        return new StatsSession(TIME_PROVIDER, statsPeriodMs);
    }

    static StatsCollectorImpl getStatsCollector() {
        return STATS_COLLECTOR;
    }
}
