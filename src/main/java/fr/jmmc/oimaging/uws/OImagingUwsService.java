package fr.jmmc.oimaging.uws;

import fr.jmmc.jmcs.util.runner.LocalLauncher;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
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
    private static final Logger _logger = LoggerFactory.getLogger(OImagingWork.class.getName());

    private UWSService service = null;

    /*
	 * REQUIRED
	 * Create instances of jobs, but only the "work" part. The "work" and the description of the job (and all the provided parameters)
	 * are now separated and only kept in the UWSJob given in parameter. This one is created automatically by the API.
	 * You just have to provide the "work" part.
     */
    private static class MyUWSFactory extends AbstractUWSFactory {

        public MyUWSFactory() {
            super();
            _logger.warn("OImagingUwsService initialisation");

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
                        _logger.error("inputfile is null");
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

    /* OPTIONAL
     * By overriding this class and giving it to your UWSService instance, you can customize the root page of your UWS.
     * If this class is not overridden an XML document which lists all registered jobs lists is returned. */
    private static class MyHomePage extends ShowHomePage {

        private static final long serialVersionUID = 1L;

        public MyHomePage(final UWSService u) {
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

    /* REQUIRED
	 * Initialize your UWS. At least, you should create one jobs list. */
    @Override
    public void init(ServletConfig config) throws ServletException {
        try {
            // TODO : bootstrap slf4j logging system
            // Create the UWS service:
            service = new UWSService(new MyUWSFactory(), new LocalUWSFileManager(new File(config.getServletContext().getRealPath("/"))));
            /* Note:
	     * Service files (like log and results) will be stored in a new directory called "ServiceFiles"
             * inside the deployment directory of the webservice. */
            // Change the default home page:
            service.replaceUWSAction(new MyHomePage(service));

            JobList jl = new JobList("oimaging");
            jl.setExecutionManager(new QueuedExecutionManager(service.getLogger(), 3));	// queue limited at 3 running jobs
            service.addJobList(jl);

        } catch (UWSException ue) {
            throw new ServletException("Can not initialize the UWS service!", ue);
        }

        LocalLauncher.startUp();
    }

    @Override
    public void destroy() {

        LocalLauncher.shutdown();

        if (service != null) {
            service.destroy();
        }
    }

    /* REQUIRED
	 * Forward all requests to the UWSService instance and deal yourself with the coming errors (if any). */
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            service.executeRequest(req, resp);
        } catch (UWSException ue) {
            resp.sendError(ue.getHttpErrorCode(), ue.getMessage());
        }
    }

}
