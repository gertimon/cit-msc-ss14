package de.tuberlin.cit.project.energy.zabbix;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class ZabbixAPITestServer extends AbstractHandler {
    private final Server server;
    private String lastRequest = null;
    private String nextResponse = null;

    public ZabbixAPITestServer(int port) throws Exception {
        this.server = new Server(port);
        this.server.setHandler(this);
        this.server.start();
    }

    public void stopServer() throws Exception {
        this.server.stop();
    }

    public void setNextResponse(String nextResponse) {
        this.nextResponse = nextResponse;
        this.lastRequest = null;
    }

    public String getLastRequest() {
        return lastRequest;
    }

    @Override
    public void handle(String path, Request baseRequest, HttpServletRequest request,
            HttpServletResponse response) throws IOException, ServletException {

        BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()));
        this.lastRequest = "";
        String line;
        while((line = reader.readLine()) != null)
            this.lastRequest += line;

        if (nextResponse != null) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println(nextResponse);
        } else {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        baseRequest.setHandled(true);
    }
}
