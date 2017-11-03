package org.github.mpashka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 */
class StatsCollectorImpl {
    private static final Logger log = LoggerFactory.getLogger(StatsCollector.class);

    private static final int[] HISTOGRAM = {
            10, 20, 40, 100, 200, 300, 400
    };

    private static final int MAX_EVENTS = 200;
    private static final int MAX_SESSIONS = 200;
    private static final int MIN_TIME = 3;
    private static final int MAX_LEVELS = 20;

    private static final int DISPLAY_GLOBAL = 5;

    private AtomicLong lastDisplayTime = new AtomicLong(System.currentTimeMillis());
    private int statsNumber = 0;

    private EventId[] eventIds = new EventId[MAX_EVENTS];
    private AtomicInteger eventId = new AtomicInteger();
    private int sessionNumber = 0;
    private SessionStats[] sessionsStats = new SessionStats[MAX_SESSIONS];
    private SessionStats[] totalSessionsStats = new SessionStats[MAX_SESSIONS];
    private AtomicInteger count = new AtomicInteger();
    private AtomicInteger totalCount = new AtomicInteger();



    public EventId createEventId(String name, EventType type) {
        int id = eventId.getAndIncrement();
        EventId eventId = new EventId(id, name, type);
        eventIds[id] = eventId;
        return eventId;
    }

    void endSession(int[] ids, long[] times, int length, long now, long statsPeriodMs) {
        long end = times[length-1];
        if (end - MIN_TIME <= 0) return;
        count.incrementAndGet();
        SessionStats session = findSession(ids, times, length);
        if (session != null) {
            session.append(times, length);
        }
        displayStatsIfNeeded(now, statsPeriodMs);
    }

    private SessionStats findSession(int[] ids, long[] times, int length) {
        int maxSessionNumber = this.sessionNumber;
        SessionStats session = findSessionAsync(ids, maxSessionNumber, length);
        if (session == null) {
            synchronized (this) {
                maxSessionNumber = this.sessionNumber;
                session = findSessionAsync(ids, maxSessionNumber, length);
                if (session == null && maxSessionNumber < MAX_SESSIONS-1) {
                    sessionsStats[maxSessionNumber] = new SessionStats(ids, length, eventIds);
                    sessionsStats[maxSessionNumber].append(times, length);
                    totalSessionsStats[maxSessionNumber] = new SessionStats(ids, length, eventIds);
                    this.sessionNumber++;
                }
            }
        }
        return session;
    }

    private SessionStats findSessionAsync(int[] ids, int maxSessionNumber, int length) {
        for (int i = 0; i < maxSessionNumber; i++) {
            SessionStats session = sessionsStats[i];
            if (session == null) break;
            if (session.equalsSession(ids, length)) return session;
        }
        return null;
    }

    private void displayStatsIfNeeded(long now, long statsPeriodMs) {
        long lastDisplay = lastDisplayTime.get();
        if (now > lastDisplay + statsPeriodMs && lastDisplayTime.compareAndSet(lastDisplay, now)) {
            StatsResult stats = createLocalStats();
            stats.log();
            if (stats.number % DISPLAY_GLOBAL == 0) {
                createGlobalStats().log();
            }
        }
    }

    /** For testing purposes */
    void clear() {
        totalCount.set(0);
        this.sessionNumber = 0;

    }

    StatsResult createGlobalStats() {
        return doCreateStats("global", totalCount.get(), totalSessionsStats, null, statsNumber);
    }

    StatsResult createLocalStats() {
        statsNumber++;
        int count = this.count.getAndSet(0);
        totalCount.addAndGet(count);
        return doCreateStats("local", count, sessionsStats, totalSessionsStats, statsNumber);
    }

    private StatsResult doCreateStats(String type, int count, SessionStats[] sessionsStats, SessionStats[] totalSessionsStats, int number) {
        int sessionNumber = this.sessionNumber;
        StatsResultSession[] sessions = new StatsResultSession[sessionNumber];
        for (int i = 0; i < sessionNumber; i++) {
            sessions[i] = sessionsStats[i].createResult(i, totalSessionsStats != null ? totalSessionsStats[i] : null, count);
        }
        return new StatsResult(type, sessions, number);
    }

    private static class SessionStats {
        // index - event, value - event type
        private EventId[] events;

        // index1 - histogram, index2 - event index
        private AtomicLong[][] times;

        // index1 - event
        private AtomicInteger[] count;

        public SessionStats(int[] ids, int length, EventId[] eventIds) {
            events = new EventId[length];
            for (int i = 0; i < length; i++) {
                events[i] = eventIds[ids[i]];
            }

            count = new AtomicInteger[HISTOGRAM.length+1];
            times = new AtomicLong[HISTOGRAM.length+1][];
            for (int histogram = 0; histogram < count.length; histogram++) {
                count[histogram] = new AtomicInteger();
                times[histogram] = new AtomicLong[length];
                for (int i = 0; i < length; i++) {
                    times[histogram][i] = new AtomicLong();
                }
            }
        }

        public void append(long[] times, int length) {
            if (length != events.length) {
                // Something is wrong
                return;
            }
            int histoNumber = detectHistogram(times[length-1]);
            count[histoNumber].incrementAndGet();
            AtomicLong[] myTimes = this.times[histoNumber];
            for (int i = 0; i < length; i++) {
                myTimes[i].addAndGet(times[i]);
            }
        }

        private int detectHistogram(long time) {
            for (int i = 0; i < HISTOGRAM.length; i++) {
                if (time < HISTOGRAM[i]) {
                    return i;
                }
            }
            return HISTOGRAM.length;
        }

        public StatsResultSession createResult(int sessionNumber, SessionStats total, int count) {
            List<StatsResultMethod> methods = new ArrayList<>();

            int level = 0;
            int[] prevIndices = new int[MAX_LEVELS];
            long[][] prevTimes = new long[MAX_LEVELS][];

            long[] localTimes_1 = null;
            long[] localTimes = null;
            for (int i = 0; i < events.length; i++) {
                if (localTimes == null) {
                    localTimes = new long[HISTOGRAM.length+1];
                }

                for (int histo = 0; histo <= HISTOGRAM.length; histo++) {
                    if (total != null) {
                        localTimes[histo] = times[histo][i].getAndSet(0);
                        total.times[histo][i].addAndGet(localTimes[histo]);
                    } else {
                        localTimes[histo] = times[histo][i].get();
                    }
                }

                EventId event = events[i];

                boolean nextLevel = false;
                if (event.getType() == EventType.method && level < MAX_LEVELS-1) {
                    prevTimes[level] = localTimes_1;
                    prevIndices[level] = i;
                    level++;
                    nextLevel = true;
                }

                methods.add(logMethod(level, localTimes, localTimes_1, event));

                if (nextLevel) {
                    localTimes_1 = null;
                } else if (event.getType() == EventType.methodEnd && level > 0) {
                    level--;
                    int prevIndex = prevIndices[level];
                    localTimes_1 = prevTimes[level];
                    EventId methodEvent = events[prevIndex];
                    methods.add(logMethod(level, localTimes, localTimes_1, methodEvent));
                }
                long[] tmp = localTimes_1;
                localTimes_1 = localTimes;
                localTimes = tmp;
            }

            int[] counts = new int[HISTOGRAM.length+1];
            int thisCount = 0;
            for (int i = 0; i <= HISTOGRAM.length; i++) {
                if (total != null) {
                    counts[i] = this.count[i].getAndSet(0);
                    total.count[i].addAndGet(counts[i]);
                } else {
                    counts[i] = this.count[i].get();
                }
                thisCount += counts[i];
            }
            return new StatsResultSession(sessionNumber, thisCount * 1000 / count
                    , methods.toArray(new StatsResultMethod[methods.size()]), counts);
        }

        private StatsResultMethod logMethod(int level, long[] times, long[] prevTimes, EventId event) {
//            StringBuilder debug = new StringBuilder();
            int[] percents = new int[HISTOGRAM.length + 1];
            for (int i = 0; i <= HISTOGRAM.length; i++) {
                long nextTime = times[i];
                long prevTime = prevTimes != null ? prevTimes[i] : 0;
                long time = nextTime - prevTime;
                long totalTime = event.getType() != EventType.end ? this.times[i][events.length-1].get() : times[i];
                int percent = totalTime > 0 ? (int) (time * 1000 / totalTime) : -1;
                percents[i] = percent;
//                debug.append(", " + prevTime +"-" + nextTime);
            }
            return new StatsResultMethod(event, level, percents/*, debug.toString()*/);
        }


        /** Check if this is the same session */
        boolean equalsSession(int[] ids, int length) {
            for (int i = 0; i < length; i++) {
                if (ids[i] != events[i].getId()) return false;
            }
            return true;
        }
    }

    static final class StatsResult {
        private String type;
        StatsResultSession[] sessions;
        int number;

        public StatsResult(String type, StatsResultSession[] sessions, int number) {
            this.type = type;
            this.sessions = sessions;
            this.number = number;
        }

        public void log() {
            int skipped = 0;
            for (StatsResultSession session : sessions) {
                if (session.percent <= 100) {
                    skipped++;
                }
            }
            log.error("Stats {} of {}, {} skipped (# {})", type, sessions.length, skipped, number);
            for (StatsResultSession session : sessions) {
                if (session.percent > 100) {
                    session.log();
                }
            }
        }
    }

    static final class StatsResultSession {
        int number;
        int percent;
        StatsResultMethod[] methods;
        int[] countHistogram;

        public StatsResultSession(int number, int percent, StatsResultMethod[] methods, int[] countHistogram) {
            this.number = number;
            this.percent = percent;
            this.methods = methods;
            this.countHistogram = countHistogram;
        }

        void log() {
            log.error("    Session stats #{} / {}%", number, percent);
            for (int i = 0; i < methods.length; i++) {
                methods[i].log();
            }

            StringBuilder countsOut = new StringBuilder();
            countsOut.append("      Totals [");
            for (int i = 0; i <= HISTOGRAM.length; i++) {
                if (i > 0) {
                    countsOut.append(", ");
                }
                countsOut.append(countHistogram[i]);
            }
            countsOut.append("]");
            log.error(countsOut.toString());
        }
    }

    static final class StatsResultMethod {
        EventId event;
        int level;
        int[] percents;
//        String debug;

        public StatsResultMethod(EventId event, int level, int[] percents/*, String debug*/) {
            this.event = event;
            this.level = level;
            this.percents = percents;
//            this.debug = debug;
        }

        void log() {
            StringBuilder methodLine = new StringBuilder("        ");
            for (int i = 0; i < level; i++) {
                methodLine.append("  ");
            }
            methodLine.append(event.getName());
            methodLine.append(" [");
            for (int histogram = 0; histogram <= HISTOGRAM.length; histogram++) {
                if (histogram > 0) {
                    methodLine.append(", ");
                }
                if (this.percents[histogram] >= 0) {
                    methodLine.append(this.percents[histogram]);
                } else {
                    methodLine.append('-');
                }
            }
            methodLine.append("]%");
//            methodLine.append("  " + debug);
            log.error(methodLine.toString());
        }
    }
 }
