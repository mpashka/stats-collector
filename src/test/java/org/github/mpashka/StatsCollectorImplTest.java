package org.github.mpashka;


import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Unit test for simple App.
 */
public class StatsCollectorImplTest {
    private static final Logger log = LoggerFactory.getLogger(StatsCollectorImplTest.class);

    private static final StatsCollectorImpl STATS_COLLECTOR = StatsCollector.getStatsCollector();


    private AtomicLong time = new AtomicLong();
    private StatsSession.TimeProvider incProvider = () -> time.incrementAndGet();
    private StatsSession.TimeProvider inc10Provider = () -> time.addAndGet(10);
    private MyClass myClass = new MyClass();

    @Test
    public void testApp() {

        STATS_COLLECTOR.clear();
        StatsCollectorImpl.StatsResult statsResult1 = myClass.doAndReportLogic(incProvider, true, true, true, true);
        log.info("--------------------------------------------1");
        statsResult1.log();

        STATS_COLLECTOR.clear();
        StatsCollectorImpl.StatsResult statsResult2 = myClass.doAndReportLogic(incProvider, true, true, false, true);
        log.info("--------------------------------------------2");
        statsResult2.log();

        STATS_COLLECTOR.clear();
        StatsCollectorImpl.StatsResult statsResult3 = myClass.doAndReportLogic(incProvider, true, false, true, false);
        log.info("--------------------------------------------3");
        statsResult3.log();
    }

    @Test
    public void test3_1() {
        STATS_COLLECTOR.clear();
        myClass.doLogic(incProvider, true, true, true, true);
        myClass.doLogic(incProvider, true, true, false, true);
        myClass.doLogic(incProvider, true, false, true, false);
        log.info("--------------------------------------------1-2-3");
        STATS_COLLECTOR.createLocalStats().log();
    }

    @Test
    public void test3_2() {
        STATS_COLLECTOR.clear();
        myClass.doLogic(incProvider, true, true, true, true);
        myClass.doLogic(incProvider, true, true, false, true);
        myClass.doLogic(incProvider, true, true, false, true);
        myClass.doLogic(incProvider, true, false, true, false);
        myClass.doLogic(incProvider, true, false, true, false);
        myClass.doLogic(incProvider, true, false, true, false);
        log.info("--------------------------------------------1-2-3");
        STATS_COLLECTOR.createLocalStats().log();
        STATS_COLLECTOR.createGlobalStats().log();
    }

    @Test
    public void testSingle() {
        STATS_COLLECTOR.clear();
        myClass.single(inc10Provider);
        STATS_COLLECTOR.createLocalStats().log();
    }

    @Test
    public void testManyLevels() {
        STATS_COLLECTOR.clear();
        myClass.manyLevels(incProvider);
        STATS_COLLECTOR.createLocalStats().log();
    }

    @Test
    public void testWrongLevel() {
        STATS_COLLECTOR.clear();
        myClass.manyLevelsWrong1(incProvider);
        STATS_COLLECTOR.createLocalStats().log();

        STATS_COLLECTOR.clear();
        myClass.manyLevelsWrong2(incProvider);
        STATS_COLLECTOR.createLocalStats().log();
    }
}
