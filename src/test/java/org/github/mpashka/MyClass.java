package org.github.mpashka;

/**
 * Created by pmoukhataev on 30.10.17.
 */
public class MyClass {

    private static final StatsCollectorImpl STATS_COLLECTOR = StatsCollector.getStatsCollector();
    private static EventId method1_1 = StatsCollector.createEventId("1.1");
    private static EventId method1_2 = StatsCollector.createEventId("1.2");
    private static EventId method2 = StatsCollector.createEventId("2", EventType.method);
    private static EventId method2_1 = StatsCollector.createEventId("2.1");
    private static EventId method2_end = StatsCollector.createEventId("2.e", EventType.methodEnd);

    private static EventId method2a = StatsCollector.createEventId("2a", EventType.method);
    private static EventId method2a_1 = StatsCollector.createEventId("2a.1");
    private static EventId method2a_end = StatsCollector.createEventId("2a.e", EventType.methodEnd);

    private static EventId method3 = StatsCollector.createEventId("3", EventType.method);
    private static EventId method3_1 = StatsCollector.createEventId("3.1");
    private static EventId method3_end = StatsCollector.createEventId("3.e", EventType.methodEnd);


    public StatsCollectorImpl.StatsResult doAndReportLogic(StatsSession.TimeProvider timeProvider, boolean m2, boolean m3, boolean m2_2a, boolean m3_2a) {
        doLogic(timeProvider, m2, m3, m2_2a, m3_2a);
        return STATS_COLLECTOR.createLocalStats();
    }

    public void doLogic(StatsSession.TimeProvider timeProvider, boolean m2, boolean m3, boolean m2_2a, boolean m3_2a) {
        StatsSession session = session(timeProvider);
        session.event(method1_1);
        if (m2) {
            session.event(method2);
            session.event(method2_1);
            if (m2_2a) {
                m2a(session);
            }
            session.event(method2_end);
        }
        session.event(method1_2);
        if (m3) {
            m3(session, m3_2a);
        }

        session.end();
    }

    private StatsSession session(StatsSession.TimeProvider timeProvider) {
        return new StatsSession(timeProvider, 60_000);
    }

    public void single(StatsSession.TimeProvider timeProvider) {
        StatsSession session = session(timeProvider);
        session.event(method1_1);
        session.end();
    }

    private void m3(StatsSession session, boolean m3_2a) {
        session.event(method3);
        if (m3_2a) {
            m2a(session);
        }
        session.event(method3_1);
        session.event(method3_end);
    }

    private void m2a(StatsSession session) {
        session.event(method2a);
        session.event(method2a_1);
        session.event(method2a_end);
    }

    public void manyLevels(StatsSession.TimeProvider timeProvider) {
        StatsSession session = session(timeProvider);
        session.event(method2);
        session.event(method2a);
        session.event(method3);
        session.event(method3_end);
        session.event(method2a_end);
        session.event(method2_end);
        session.end();
    }

    public void manyLevelsWrong1(StatsSession.TimeProvider timeProvider) {
        StatsSession session = session(timeProvider);
        session.event(method2);
        session.event(method2a);
        session.event(method3);
        session.event(method3_end);
        session.end();
    }

    public void manyLevelsWrong2(StatsSession.TimeProvider timeProvider) {
        StatsSession session = session(timeProvider);
        session.event(method3);
        session.event(method3_end);
        session.event(method2a_end);
        session.event(method2_end);
        session.end();
    }


}
