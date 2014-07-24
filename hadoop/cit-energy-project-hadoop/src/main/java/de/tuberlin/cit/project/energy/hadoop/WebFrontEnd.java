package de.tuberlin.cit.project.energy.hadoop;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.tuberlin.cit.project.energy.hadoop.EnergyConservingDataNodeFilter.EnergyMode;

/**
 * Web front end around energy base data node filter.
 * 
 * @author Sascha
 */
public class WebFrontEnd extends AbstractHandler {
    private final static Log LOG = LogFactory.getLog(WebFrontEnd.class);
    
    public final static int DEFAULT_PORT = 50200;
    public final static String API_PREFIX = "/api/v1/";
    public final static String USER_PROFILE_PATH = "user-profile";

    private final EnergyConservingDataNodeFilter dataNodeFilter;
    private final Server webServer;
    private final ObjectMapper objectMapper;
    
    public WebFrontEnd(EnergyConservingDataNodeFilter dataNodeFilter) {
        this(dataNodeFilter, DEFAULT_PORT);
    }
    
    public WebFrontEnd(EnergyConservingDataNodeFilter dataNodeFilter, int port) {
        this.dataNodeFilter = dataNodeFilter;
        this.webServer = new Server(port);
        this.webServer.setHandler(this);
        try {
            this.webServer.start();
        } catch (Exception e) {
            LOG.error("Can't start web front end: " + e.getMessage());
            e.printStackTrace();
        }
        LOG.info("Web front end start at port " + port + ".");

        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    public void handle(String path, Request baseRequest, HttpServletRequest request,
            HttpServletResponse response) throws IOException, ServletException {

        if (path.equalsIgnoreCase(API_PREFIX + USER_PROFILE_PATH)) {
            if (request.getMethod().equals("PUT")) {
                if (request.getParameter("username") != null && request.getParameter("profile") != null) {
                    try {
                        EnergyMode newMode = EnergyMode.valueOf(request.getParameter("profile").toUpperCase());
                        this.dataNodeFilter.getUserEnergyMapping().put(request.getParameter("username"), newMode);
                        response.setStatus(HttpServletResponse.SC_OK);
                    } catch(IllegalArgumentException e) {
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    }
                } else
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            } else
                response.setStatus(HttpServletResponse.SC_OK);

            response.setContentType("application/json");
            ObjectNode responseNode = this.objectMapper.createObjectNode();
            
            for(String user : this.dataNodeFilter.getUserEnergyMapping().keySet()) {
                ObjectNode userNode = responseNode.objectNode();
                userNode.put("username", user);
                userNode.put("profile", this.dataNodeFilter.getUserEnergyMapping().get(user).toString());
                responseNode.withArray("currentSettings").add(userNode);
            }
            
            for(EnergyMode mode : EnergyMode.values())
                responseNode.withArray("availableProfiles").add(mode.toString());
            
            this.objectMapper.writeValue(response.getOutputStream(), responseNode);
            baseRequest.setHandled(true);
        }
    }

    public void stopServer() throws Exception {
        this.webServer.stop();
        this.webServer.join();
    }
}
