/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oimaging.uws;

import fr.jmmc.jmcs.util.concurrent.ThreadExecutors;
import fr.jmmc.jmcs.util.runner.LocalLauncher;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author bourgesl
 */
final class ServiceMonitor implements Runnable {

    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(ServiceMonitor.class.getName());

    /* monitoring delay = 60 s */
    private final static long INTERVAL_MONITORING = 60000L;
    /* temporary file cleanup = 12h = 12 * 3600 s */
    private final static long MAX_ALIVE_DURATION = 12 * 3600 * 1000L;

    /** file name prefix to skip */
    private final static String FILE_PREFIX_PATTERN = null;

    /* members */
    private final File rootDir;

    ServiceMonitor(final File rootDir) {
        this.rootDir = rootDir;
    }

    @Override
    public void run() {
        while (true) {
            if (ThreadExecutors.sleep(INTERVAL_MONITORING)) {
                try {
                    doMonitoring();
                } catch (Throwable th) {
                    logger.error("ServiceMonitor: doMonitoring failure", th);
                }
            } else {
                logger.info("ServiceMonitor: Interrupted.");
                break;
            }
        }
        logger.info("ServiceMonitor: done.");
    }

    public void doMonitoring() {
        LocalLauncher.dumpStats();
        OImagingUwsStats.INSTANCE.dumpStats();

        // TODO: other tasks (File system cleanup ?)
        // cleanup input / output files in UWS for leaking job (never retrieved by client / remove after 1 day ...)
        pruneFileSystem();
    }

    private void pruneFileSystem() {
        logger.debug("pruneFileSystem: start");
        pruneDirectory(rootDir, FILE_PREFIX_PATTERN);
        logger.debug("pruneFileSystem: done");
    }

    private static void pruneDirectory(final File dir, final String filePrefixIgnore) {
        final File[] files = dir.listFiles();

        if (files != null && files.length != 0) {
            final long now = System.currentTimeMillis();

            if (logger.isDebugEnabled()) {
                logger.debug("pruneDirectory: checking {} files...", files.length);
            }

            for (File f : files) {
                if (f.isDirectory()) {
                    // do not filter files in sub directories:
                    pruneDirectory(f, null);

                    // remove directory only if empty:
                    final File[] children = f.listFiles();
                    if (children != null && children.length == 0) {
                        logger.info("deleting directory [{}]", f.getAbsolutePath());
                        f.delete();
                    }

                } else if (f.isFile()) {
                    if (filePrefixIgnore == null || !f.getName().startsWith(filePrefixIgnore)) {
                        final long elapsed = now - f.lastModified();
                        if (elapsed > MAX_ALIVE_DURATION) {
                            logger.info("deleting file [{}]", f.getAbsolutePath());
                            f.delete();
                        } else {
                            if (logger.isDebugEnabled()) {
                                logger.debug("skipping file [{}] (alive: {} s)", f.getAbsolutePath(),
                                        elapsed / 1000l);
                            }
                        }
                    }
                }
            }
            logger.debug("pruneDirectory: done");
        }
    }
}
