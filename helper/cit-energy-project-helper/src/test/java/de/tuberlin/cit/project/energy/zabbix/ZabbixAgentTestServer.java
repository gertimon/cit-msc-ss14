package de.tuberlin.cit.project.energy.zabbix;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Simple zabbix agent dummy server.
 *
 * @author Sascha
 */
public class ZabbixAgentTestServer implements Runnable {
    public static final int HEADER_LENGTH = 13;
    private String lastReceivedAgentMessage = null;
    private final ServerSocket serverSocket;
    private final Thread serverThread;

    public ZabbixAgentTestServer(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
        this.serverThread = new Thread(this);
        this.serverThread.start();
    }

    public void close() throws IOException {
        this.serverThread.interrupt();
        this.serverSocket.close();
    }

    public void resetLastReceivedAgentMessage() {
        this.lastReceivedAgentMessage = null;
    }

    public synchronized String waitForNextMessage(int timeout) throws InterruptedException {
        while(this.lastReceivedAgentMessage == null)
            this.wait(timeout);
        return this.lastReceivedAgentMessage;
    }

    public synchronized void setLastReceivedAgentMessage(String lastReceivedAgentMessage) {
        this.lastReceivedAgentMessage = lastReceivedAgentMessage;
        this.notifyAll();
    }

    @Override
    public void run() {
        try {
            while(!Thread.interrupted()) {
                Socket clientSocket = this.serverSocket.accept();
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                char buffer[] = new char[1024];
                StringBuilder sb = new StringBuilder();
                int charsReceived;
                while ((charsReceived = reader.read(buffer)) > 0)
                    sb.append(buffer, 0, charsReceived);
                sb.delete(0, HEADER_LENGTH); // skip header
                setLastReceivedAgentMessage(sb.toString());
            }
        } catch(IOException e) {
        }
    }
}
