package fr.jmmc.oimaging.uws;

import fr.jmmc.jmcs.util.FileUtils;
import fr.jmmc.jmcs.util.StringUtils;
import fr.jmmc.jmcs.util.runner.EmptyJobListener;
import fr.jmmc.jmcs.util.runner.JobListener;
import fr.jmmc.jmcs.util.runner.LocalLauncher;
import fr.jmmc.jmcs.util.runner.RootContext;
import fr.jmmc.jmcs.util.runner.RunState;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uws.UWSException;
import uws.job.ErrorType;
import uws.job.JobThread;
import uws.job.Result;
import uws.job.UWSJob;
import uws.service.request.UploadFile;

public class OImagingWork extends JobThread {

    /** Logger */
    private static final Logger _logger = LoggerFactory.getLogger(OImagingWork.class.getName());
    /** application identifier for LocalService */
    public final static String APP_NAME = "OImaging-uws-worker";
    /** user for LocalService */
    public final static String USER_NAME = "JMMC";
    /** task identifier for LocalService */
    public final static String TASK_NAME = "LocalRunner";
    public static final String INPUTFILE = "inputfile";

    public OImagingWork(UWSJob j) throws UWSException {
        super(j);
    }

    @Override
    protected void jobWork() throws UWSException, InterruptedException {

        // If the task has been canceled/interrupted, throw the corresponding exception:
        if (isInterrupted()) {
            throw new InterruptedException();
        }

        // Check input param
        if (getJob().getAdditionalParameterValue(INPUTFILE) == null) {
            _logger.error("inputfile is null");
            throw new UWSException(UWSException.BAD_REQUEST, "Wrong \"" + INPUTFILE + "\" param. An OIFits file is expected!", ErrorType.FATAL);
        }

        File outputFile = null;
        File logFile = null;
        try {
            _logger.error("jobWork launched");
            _logger.error("params: " + getJob().getAdditionalParameters());
            // prepare the result:
            Result outputResult = createResult("outputfile");
            Result logResult = createResult("logfile");

            // Get user's input
            UploadFile inputFile = (UploadFile) getJob().getAdditionalParameterValue(INPUTFILE);
            // and fakes other files using inputfile name
            // Warning : we use the path from getLocation()

            String inputFilename = inputFile.getLocation().replaceFirst("file:", "");
            outputFile = new File(inputFilename + ".out");
            logFile = new File(inputFilename + ".log");

            exec("bsmem-ci", inputFilename, outputFile.getAbsolutePath(), logFile.getAbsolutePath(), EmptyJobListener.INSTANCE);

            if (outputFile.exists()) {
                FileUtils.saveFile(outputFile, getResultOutput(outputResult));
                publishResult(outputResult);
            }

            if (logFile.exists()) {
                FileUtils.saveFile(logFile, getResultOutput(logResult));
                publishResult(logResult);
            }

        } catch (IOException e) {
            // If there is an error, encapsulate it in an UWSException so that an error summary can be published:
            throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, e, "Impossible to write the result file of the Job " + job.getJobId() + " !", ErrorType.TRANSIENT);
        } finally {
            if (outputFile != null) {
                outputFile.delete();
            }
            if (logFile != null) {
                logFile.delete();
            }
        }

    }

    /**
     * Launch the given application in background.
     *
     * @param appName
     * @param inputFilename
     * @param outputFilename
     * @param jobListener job event listener (not null)
     * @throws IllegalStateException if the job can not be submitted to the job queue
     */
    public static int exec(final String appName, final String inputFilename, final String outputFilename, final String logFilename, final JobListener jobListener) throws IllegalStateException {

        if (StringUtils.isEmpty(appName)) {
            throw new IllegalArgumentException("empty application name !");
        }
        if (StringUtils.isEmpty(inputFilename)) {
            throw new IllegalArgumentException("empty input filename !");
        }
        if (StringUtils.isEmpty(outputFilename)) {
            throw new IllegalArgumentException("empty output filename !");
        }
        if (StringUtils.isEmpty(logFilename)) {
            throw new IllegalArgumentException("empty log filename !");
        }
        if (jobListener == null) {
            throw new IllegalArgumentException("undefined job listener !");
        }

        // create the execution context with log file:
        // TODO: fix temporary location
        final RootContext jobContext = LocalLauncher.prepareMainJob(APP_NAME, USER_NAME, FileUtils.getTempDirPath(), logFilename);

        final String[] cmd = new String[]{appName, inputFilename, outputFilename};
        LocalLauncher.prepareChildJob(jobContext, TASK_NAME, cmd);

        // Puts the job in the job queue (can throw IllegalStateException if job not queued)
        LocalLauncher.startJob(jobContext, jobListener);

        // Wait for process completion
        try {
            // Wait for task to be done :
            jobContext.getFuture().get();
        } catch (InterruptedException ie) {
            _logger.debug("waitFor: interrupted", ie);
        } catch (ExecutionException ee) {
            _logger.info("waitFor: execution error", ee);
        }

        // TODO: retrieve command execution status code
        return jobContext.getState() == RunState.STATE_FINISHED_OK ? 0 : 1;
    }

}
