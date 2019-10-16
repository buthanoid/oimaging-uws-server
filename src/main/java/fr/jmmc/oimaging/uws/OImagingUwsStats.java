/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oimaging.uws;

import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic statistics
 */
public final class OImagingUwsStats {

    private static final Logger logger = LoggerFactory.getLogger(OImagingUwsStats.class.getName());

    /** singleton */
    public final static OImagingUwsStats INSTANCE = new OImagingUwsStats();

    /* members */
    /** stats */
    private final TreeMap<String, AppStats> statsMap = new TreeMap<String, AppStats>();
    /** last update timestamp */
    private long lastUpdate = 0L;

    private OImagingUwsStats() {
        super();
    }

    /**
     * Logs the Launcher statistics
     */
    public synchronized void dumpStats() {
        final long now = System.currentTimeMillis();

        if ((now != lastUpdate)) {
            logger.info("OImagingUwsStats: {}", getStats());
        }
    }

    public synchronized String getStats() {
        final StringBuilder sb = new StringBuilder(1024);
        for (AppStats stats : statsMap.values()) {
            stats.dump(sb);
        }
        return sb.toString();
    }

    public synchronized void start(final String appName) {
        lastUpdate = System.currentTimeMillis();
        getStats(appName).started++;
    }

    public synchronized void success(final String appName) {
        lastUpdate = System.currentTimeMillis();
        getStats(appName).success++;
    }

    public synchronized void error(final String appName) {
        lastUpdate = System.currentTimeMillis();
        getStats(appName).error++;
    }

    public synchronized void cancel(final String appName) {
        lastUpdate = System.currentTimeMillis();
        getStats(appName).cancel++;
    }

    private AppStats getStats(final String appName) {
        AppStats stats = statsMap.get(appName);
        if (stats == null) {
            stats = new AppStats(appName);
            statsMap.put(appName, stats);
        }
        return stats;
    }

    private final static class AppStats {

        final String name;
        int started = 0;
        int success = 0;
        int error = 0;
        int cancel = 0;

        AppStats(final String name) {
            this.name = name;
        }

        void dump(final StringBuilder sb) {
            sb.append("\n").append('[').append(name).append("]:")
                    .append(" started = ").append(started)
                    .append(" success = ").append(success)
                    .append(" error = ").append(error)
                    .append(" cancel = ").append(cancel);
        }
    }
}
