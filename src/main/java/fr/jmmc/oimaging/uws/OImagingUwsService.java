package fr.jmmc.oimaging.uws;

import fr.jmmc.jmcs.data.preference.Preferences;
import fr.jmmc.jmcs.logging.LoggingService;
import fr.jmmc.jmcs.util.FileUtils;
import fr.jmmc.jmcs.util.concurrent.ThreadExecutors;
import fr.jmmc.jmcs.util.runner.LocalLauncher;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Future;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uws.UWSException;
import uws.job.ErrorType;
import uws.job.JobList;
import uws.job.JobThread;
import uws.job.UWSJob;
import uws.job.manager.QueuedExecutionManager;
import uws.job.parameters.InputParamController;
import uws.job.user.JobOwner;
import uws.service.AbstractUWSFactory;
import uws.service.UWSService;
import uws.service.UWSUrl;
import uws.service.actions.ShowHomePage;
import uws.service.file.LocalUWSFileManager;
import uws.service.log.DefaultUWSLog;
import uws.service.log.UWSLog.LogLevel;
import uws.service.request.UploadFile;

public class OImagingUwsService extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /* max job duration (seconds) = 2 hours */
    private final static long MAX_DURATION = 2 * 3600L;

    private static final boolean LOG_SETTINGS = false;
    private static final boolean HOMEPAGE_SHOW_JOBS = false;

    /** OIMaging LogBack configuration file as one resource file (in class path) */
    private final static String OIMAGING_LOGBACK_CONFIG_RESOURCE = "LogbackConfiguration.xml";

    private static Logger logger = null;

    /* members */
    private UWSService service = null;

    /** future on the monitoring task (cancel) */
    private Future<?> monitorFuture = null;

    public OImagingUwsService() {
        super();
    }

    /* 
     * REQUIRED
	 * Initialize your UWS. At least, you should create one jobs list.
     */
    @Override
    public void init(final ServletConfig config) throws ServletException {
        try {
            // Start the application log singleton in web service mode:
            LoggingService.getInstanceForWebService(OIMAGING_LOGBACK_CONFIG_RESOURCE);

            final Logger jmmcLogger = LoggingService.getJmmcLogger();
            jmmcLogger.info("jMCS log created at {}. Current level is {}.", new Date(), LoggingService.getLoggerEffectiveLevel(jmmcLogger));

            // Initialize internal logger:
            logger = LoggerFactory.getLogger(OImagingWork.class.getName());

            if (LOG_SETTINGS) {
                final StringBuilder sb = new StringBuilder(16384);
                sb.append("System properties:\n");
                // Get all informations about the system running the application
                Preferences.dumpProperties(System.getProperties(), sb);
                sb.append("\n\nEnvironment settings:\n");
                Preferences.dumpProperties(System.getenv(), sb);

                logger.info("JVM Settings:\n{}", sb.toString());
            }

            final String tempPath = FileUtils.getTempDirPath();
            final File uwsDir = new File(tempPath, "uws");
            uwsDir.mkdirs();

            final File logDir = FileUtils.getDirectory("./logs/");
            if (logDir == null) {
                throw new ServletException("Can not initialize the UWS service !");
            }

            // Get UWS config from servlet config:
            final int maxRunningJobs;
            try {
                maxRunningJobs = Integer.valueOf(config.getInitParameter("maxRunningJobs"));
            } catch (NumberFormatException nfe) {
                logger.error("Unable to parse parameter 'maxRunningJobs' from servlet config !", nfe);
                throw new ServletException("Can not initialize the UWS service !", nfe);
            }
            logger.info("init: Initializing UWS Service[maxRunningJobs = {}, maxDuration = {} s]", maxRunningJobs, MAX_DURATION);
            logger.info("init: UWS root path = '{}'", uwsDir.getAbsolutePath());
            logger.info("init: UWS  log path = '{}'", logDir.getAbsolutePath());

            try {
                final LocalUWSFileManager uwsFileManager = new LocalUWSFileManager(uwsDir) {
                    @Override
                    protected File getLogFile(final LogLevel level, final String context) {
                        return new File(logDir, getLogFileName(level, context));
                    }
                };
                // Create the UWS service:
                service = new UWSService(new OImagingUWSFactory(MAX_DURATION), uwsFileManager, new DefaultUWSLog(uwsFileManager));

                /* 
            * Note:
	         * Service files (like log and results) will be stored in a new directory called "ServiceFiles"
             * inside the deployment directory of the webservice.
                 */
                // Change the default home page:
                service.replaceUWSAction(new MyHomePage(service));

                JobList jl = new JobList("oimaging");
                jl.setExecutionManager(new QueuedExecutionManager(service.getLogger(), maxRunningJobs));
                service.addJobList(jl);

            } catch (UWSException ue) {
                logger.error("Unable to initialize UWS service :", ue);
                throw new ServletException("Can not initialize the UWS service !");
            }

            LocalLauncher.startUp();

            monitorFuture = ThreadExecutors.getSingleExecutor("ServiceMonitor").submit(new ServiceMonitor(uwsDir));

            logger.info("init: done");

        } catch (RuntimeException re) {
            logger.error("Unable to initialize UWS service :", re);
            throw new ServletException("Can not initialize the UWS service !");
        }
    }

    @Override
    public void destroy() {
        logger.info("destroy: stopping UWS Service...");

        if (service != null) {
            service.destroy();
        }

        if (monitorFuture != null) {
            // Signal to monitor to interrupt the thread:
            monitorFuture.cancel(true);
        }
        LocalLauncher.shutdown();

        logger.info("destroy: done");
    }

    /* 
     * REQUIRED
	 * Forward all requests to the UWSService instance and deal yourself with the coming errors (if any).
     */
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            service.executeRequest(req, resp);
        } catch (UWSException ue) {
            resp.sendError(ue.getHttpErrorCode(), ue.getMessage());
        }
    }

    /*
	 * REQUIRED
	 * Create instances of jobs, but only the "work" part. The "work" and the description of the job (and all the provided parameters)
	 * are now separated and only kept in the UWSJob given in parameter. This one is created automatically by the API.
	 * You just have to provide the "work" part.
     */
    private static final class OImagingUWSFactory extends AbstractUWSFactory {

        OImagingUWSFactory(final long maxDuration) {
            super();

            // set max job duration (timeout):
            configureExecution(maxDuration, maxDuration, false);

            addExpectedAdditionalParameter(OImagingWork.INPUTFILE);
            setInputParamController(OImagingWork.INPUTFILE, new InputParamController() {

                @Override
                public Object getDefault() {
                    // We could put Mickey in a future version
                    return null;
                }

                @Override
                public Object check(Object val) throws UWSException {
                    UploadFile file;
                    if (val instanceof UploadFile) {
                        file = (UploadFile) val;
                        // TODO check content ?
                    } else {
                        // WARNING : this is a dead code area, we never enter this branch :(
                        // val == null or others
                        logger.error("inputfile is null");
                        throw new UWSException(UWSException.BAD_REQUEST, "Wrong \"" + OImagingWork.INPUTFILE + "\" param. An OIFits file is expected!", ErrorType.FATAL);

                    }
                    return file;
                }

                @Override
                public boolean allowModification() {
                    return false;
                }
            });
        }

        @Override
        public JobThread createJobThread(UWSJob job) throws UWSException {
            // You should return the appropriate JobThread ; usually (like here) it just depends on the jobList name:
            if (job.getJobList().getName().equals("oimaging")) {
                return new OImagingWork(job);
            } else {
                throw new UWSException("Impossible to create a job inside the jobs list \"" + job.getJobList().getName() + "\"!");
            }
        }
    }

    /* 
     * OPTIONAL
     * By overriding this class and giving it to your UWSService instance, you can customize the root page of your UWS.
     * If this class is not overridden an XML document which lists all registered jobs lists is returned.
     */
    private static final class MyHomePage extends ShowHomePage {

        private static final long serialVersionUID = 1L;

        MyHomePage(final UWSService u) {
            super(u);
        }

        @Override
        public boolean apply(UWSUrl url, JobOwner user, HttpServletRequest req, HttpServletResponse resp) throws UWSException, IOException {
            PrintWriter out = resp.getWriter();

            out.println("<html><head><title>UWS4 OImaging</title></head><body>");
            out.println("<h1>UWS4 OImaging</h1>");

            // disabled for confidentiality requirements:
            if (HOMEPAGE_SHOW_JOBS) {
                out.println("<p>Below is the list of all available jobs lists:</p>");

                out.println("<ul>");
                for (JobList jl : getUWS()) {
                    out.println("<li>" + jl.getName() + " - " + jl.getNbJobs() + " jobs - <a href=\"" + jl.getUrl() + "\">" + jl.getUrl() + "</a></li>");
                }
                out.println("</ul>");
            }

            out.println("<h2>Statistics</h2>");
            out.println("<pre>");
            out.println(OImagingUwsStats.INSTANCE.getStats());
            out.println("</pre>");
            
            out.println("<h2>Software versions</h2>");
            out.println(dumpProperties(System.getenv(), new StringBuilder(1024), "CI_VERSION").toString());

            out.println("</body></html>");

            return true;
        }

        public static StringBuilder dumpProperties(final Map<?, ?> properties, final StringBuilder sb, final String pattern) {
            if (properties == null) {
                return sb;
            }

            // Sort properties
            final Object[] keys = new Object[properties.size()];
            properties.keySet().toArray(keys);
            Arrays.sort(keys);

            // For each property, we make a string like "{name} : {value}"
            sb.append("<ul>");
            for (Object key : keys) {
                if (key.toString().contains(pattern)) {
                    sb.append("<li>").append(key).append(" : ").append(properties.get(key)).append("</li>");
                }
            }
            sb.append("</ul>");
            return sb;
        }
    }

}
