package fr.jmmc.oimaging.uws;

import fr.jmmc.jmcs.data.preference.Preferences;
import fr.jmmc.jmcs.logging.LoggingService;
import fr.jmmc.jmcs.util.FileUtils;
import fr.jmmc.jmcs.util.concurrent.ThreadExecutors;
import fr.jmmc.jmcs.util.runner.LocalLauncher;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
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
import uws.service.request.UploadFile;

public class OImagingUwsService extends HttpServlet {
    
    private static final long serialVersionUID = 1L;
    
    final static long INTERVAL_MONITORING = 60000L;
    
    private static final boolean LOG_SETTINGS = false;
    
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
        // Start the application log singleton with log mappers
        LoggingService.getInstance(false);
        
        final ch.qos.logback.classic.Logger jmmcLogger = LoggingService.getJmmcLogger();
        jmmcLogger.info("jMCS log created at {}. Current level is {}.", new Date(), jmmcLogger.getEffectiveLevel());

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
        
        try {
            // Get UWS config from servlet config:
            final int maxRunningJobs = Integer.valueOf(config.getInitParameter("maxRunningJobs"));
            logger.info("init: Initializing UWS Service[maxRunningJobs = {}]", maxRunningJobs);
            
            final String tempPath = FileUtils.getTempDirPath();
            final File uwsDir = new File(tempPath, "uws");
            uwsDir.mkdirs();
            
            logger.info("init: UWS root path = '{}'", uwsDir);
            
            final LocalUWSFileManager fileManager = new LocalUWSFileManager(uwsDir);

            // Create the UWS service:
            service = new UWSService(new MyUWSFactory(), fileManager);

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
            
        } catch (NumberFormatException nfe) {
            throw new ServletException("Can not initialize the UWS service!", nfe);
        } catch (UWSException ue) {
            throw new ServletException("Can not initialize the UWS service!", ue);
        }
        
        LocalLauncher.startUp();
        
        monitorFuture = ThreadExecutors.getSingleExecutor("ServiceMonitor").submit(new ServiceMonitor());
        
        logger.info("init: done");
    }
    
    @Override
    public void destroy() {
        logger.info("destroy: stopping UWS Service...");

        // Signal to monitor to stop:
        monitorFuture.cancel(true);
        
        LocalLauncher.shutdown();
        
        if (service != null) {
            service.destroy();
        }
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
    private static final class MyUWSFactory extends AbstractUWSFactory {
        
        MyUWSFactory() {
            super();
            
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

            // TODO : remove it because of confidentiality requirements.
            out.println("<html><head><title>UWS4 OIMaging (using UWSService)</title></head><body>");
            out.println("<h1>UWS4 OIMaging (using UWSService)</h1");
            out.println("<p>Below is the list of all available jobs lists:</p>");
            
            out.println("<ul>");
            for (JobList jl : getUWS()) {
                out.println("<li>" + jl.getName() + " - " + jl.getNbJobs() + " jobs - <a href=\"" + jl.getUrl() + "\">" + jl.getUrl() + "</a></li>");
            }
            out.println("</ul>");
            return true;
        }
    }
    
    private static final class ServiceMonitor implements Runnable {
        
        @Override
        public void run() {
            while (true) {
                doMonitoring();
                
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

            // TODO: other tasks (File system cleanup ?)
            // cleanup input / output files in UWS for leaking job (never retrieved by client / remove after 1 day ...)
        }
    }
}
