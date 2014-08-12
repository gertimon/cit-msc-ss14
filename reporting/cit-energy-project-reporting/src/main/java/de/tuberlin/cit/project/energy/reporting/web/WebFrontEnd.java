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
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.tuberlin.cit.project.energy.reporting.ReportGenerator;
import de.tuberlin.cit.project.energy.reporting.model.UsageReport;


public class WebFrontEnd extends AbstractHandler {
    private final static Log LOG = LogFactory.getLog(WebFrontEnd.class);
    
    public final static int DEFAULT_PORT = 50201;
    public final static String API_PREFIX = "/api/v1/";
    public final static String USER_REPORT_PATH = "user-report.json";
    
    public final static String RESOURCE_DIR = "webapp/dist";
    
    private final Server webServer;
    private final ObjectMapper objectMapper;
    private final ReportGenerator reportGenerator;

    public WebFrontEnd() {
        this(DEFAULT_PORT);
    }

    public WebFrontEnd(int port) {
        this.objectMapper = new ObjectMapper();
        this.reportGenerator = new ReportGenerator();
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

    @Override
    public void handle(String path, HttpServletRequest request, HttpServletResponse response, int dispatch)
            throws IOException, ServletException {

        if (path.equalsIgnoreCase(API_PREFIX + USER_REPORT_PATH)) {
            response.setStatus(HttpServletResponse.SC_OK);

            response.setContentType("application/json");

            long now = Calendar.getInstance().getTimeInMillis() / 1000;
            double days = 0.25;
            long start = (long)(now - 60 * 60 * 24 * days);
            long end = now;
            int resolution = 60*60;
            LOG.info("Got report generation request: from=" + (new Date(start * 1000)) + ", to=" + (new Date(end * 1000)) + ", resolution=" + resolution);
            UsageReport report = this.reportGenerator.getReport(start, end, resolution);            
            this.objectMapper.writeValue(response.getOutputStream(), report.toJson());

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
