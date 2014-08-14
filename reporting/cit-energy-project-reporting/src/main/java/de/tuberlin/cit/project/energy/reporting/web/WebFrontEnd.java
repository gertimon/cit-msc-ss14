package de.tuberlin.cit.project.energy.reporting.web;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.handler.ResourceHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.tuberlin.cit.project.energy.reporting.CSVUtils;
import de.tuberlin.cit.project.energy.reporting.ReportGenerator;
import de.tuberlin.cit.project.energy.reporting.model.UsageReport;

/**
 * Serves static webapp files and generated reports as JSON.
 *
 * @author Sascha
 */
public class WebFrontEnd extends AbstractHandler {
    private final static Log LOG = LogFactory.getLog(WebFrontEnd.class);
    
    public final static int DEFAULT_PORT = 50201;
    public final static String API_PREFIX = "/api/v1/";
    public final static String USER_REPORT_PATH = "user-report.json";
    
    public final static String RESOURCE_DIR = "webapp" + File.separator + "dist";

    public final static String CSV_DUMP_DIR = "dump";
    public final static String CSV_DUMP_PREFIX_FORMAT = CSV_DUMP_DIR + File.separator + "usageReport.%d-%d";
    public final static long CSV_OFFLINE_DEFAULT_START = 1407628800;
    public final static long CSV_OFFLINE_DEFAULT_END = 1407887999;
    
    private final Server webServer;
    private final ObjectMapper objectMapper;
    private ReportGenerator reportGenerator;

    public WebFrontEnd() {
        this(DEFAULT_PORT);
    }

    public WebFrontEnd(int port) {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT); // pretty output

        this.webServer = new Server(port);
        this.webServer.setHandler(this);
        
        // ----- the handler serving all the static files -----
        File resourceDir = new File(RESOURCE_DIR);
        if (!resourceDir.exists()) {
            LOG.error("Can't find resource dir " + RESOURCE_DIR);
        } else {
            ResourceHandler resourceHandler = new ResourceHandler();
            resourceHandler.setResourceBase(resourceDir.getAbsolutePath());
            this.webServer.addHandler(resourceHandler);
        }
        
        try {
            this.webServer.start();
        } catch (Exception e) {
            LOG.error("Can't start web front end: " + e.getMessage());
            e.printStackTrace();
        }
        LOG.info("Web front end start at port " + port + ".");
    }

    /** Lazy loaded report generator. */
    private ReportGenerator getReportGenerator() {
        if (this.reportGenerator == null)
            this.reportGenerator = new ReportGenerator();
        return this.reportGenerator;
    }

    @Override
    public void handle(String path, HttpServletRequest request, HttpServletResponse response, int dispatch)
            throws IOException, ServletException {

        if (path.equalsIgnoreCase(API_PREFIX + USER_REPORT_PATH)) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");
            response.addHeader("Access-Control-Allow-Origin", "*"); // allow grunt webapp serving in development

            try {
                String mode = request.getParameter("mode");

                if (mode != null && mode.equalsIgnoreCase("online")) {
                    long today = Calendar.getInstance().getTimeInMillis() / 1000;
                    today = today - (today % (60*60*24)); // align to days (in GMT)
                    double days = 1;
                    double daysOffset = 4;
                    long start = (long)(today - 60 * 60 * 24 * daysOffset);
                    long end = (long)(start + (60 * 60 * 24 * days) - 1);
                    int resolution = 60*60;
                    LOG.info("Got report generation request: from=" + (new Date(start * 1000)) + ", to=" + (new Date(end * 1000)) + ", resolution=" + resolution);
                    LOG.info("This could take some time...");

                    UsageReport report = getReportGenerator().getReport(start, end, resolution);
                    ObjectNode result = report.toJson();
                    result.put("mode", "online");
                    this.objectMapper.writeValue(response.getOutputStream(), result);

                    String outputPrefix = String.format(CSV_DUMP_PREFIX_FORMAT, start, end);
                    LOG.info("Dummping report with prefix " + outputPrefix);
                    CSVUtils.writeCSV(report, outputPrefix);

                } else { // default to offline mode
                    String inputPrefix = String.format(CSV_DUMP_PREFIX_FORMAT,
                            CSV_OFFLINE_DEFAULT_START, CSV_OFFLINE_DEFAULT_END);
                    LOG.info("Reading offline data with prefix: " + inputPrefix);
                    UsageReport report = CSVUtils.readCSV(inputPrefix);
                    ObjectNode result = report.toJson();
                    result.put("mode", "offline");
                    this.objectMapper.writeValue(response.getOutputStream(), result);
                }
            } catch(Exception e) {
                LOG.error("Failure while processing report request: " + e.getMessage(), e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }

            ((Request)request).setHandled(true);
        }
    }

    public void stopServer() throws Exception {
        this.webServer.stop();
        this.webServer.join();
    }

    public static void main(String[] args) {
        new WebFrontEnd();
    }
}
