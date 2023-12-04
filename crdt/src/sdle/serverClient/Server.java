package sdle.serverClient;

import java.net.ServerSocket;
import java.net.InetSocketAddress;
import java.io.IOException;

public class Server {
    private String ipAddress;
    private int portNumber;
    private ServerSocket serverSocket;

    public Server(String ipAddress, int portNumber) {
        this.ipAddress = ipAddress;
        this.portNumber = portNumber;

        try {
            // Create a server socket and bind it to the specified IP address and port number
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(this.ipAddress, this.portNumber));
            System.out.println("Server started at " + this.ipAddress + ":" + this.portNumber);
        } catch (IOException e) {
            System.out.println("Could not start server on " + this.ipAddress + ":" + this.portNumber);
            e.printStackTrace();
        }
    }

    // Method to start the server (for example, start listening for connections)
    public void start() {
        // Implementation of your server's logic (e.g., accept client connections)

    }

    // Method to stop the server
    public void stop() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                System.out.println("Server stopped");
            }
        } catch (IOException e) {
            System.out.println("Error stopping the server");
            e.printStackTrace();
        }
    }
}