package sdle.serverClient;

import java.io.DataInputStream;
import java.net.ServerSocket;
import java.net.InetSocketAddress;
import java.io.IOException;
import java.net.Socket;
import java.util.SortedMap;
import java.util.TreeMap;

public class Server implements Runnable {
    private String ipAddress;
    private int portNumber;
    private ServerSocket serverSocket;

    private SortedMap<Long, Server> serverTable;

    public Server(String ipAddress, int portNumber) {
        this.ipAddress = ipAddress;
        this.portNumber = portNumber;
        this.serverTable = new TreeMap<Long, Server>();

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

    private void handleClientConnection(Socket clientSocket) {
        try (DataInputStream dis = new DataInputStream(clientSocket.getInputStream())) {
            // Read the long value sent by the client
            long receivedLong = dis.readLong();
            System.out.println("Received long value from client: " + receivedLong);
        } catch (IOException e) {
            System.out.println("Error handling client connection: " + e.getMessage());
            // Handle exception
        } finally {
            // Close client socket
            try {
                clientSocket.close();
            } catch (IOException e) {
                // Handle exception
            }
        }
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(portNumber);
            System.out.println("Server started on " + ipAddress + ":" + portNumber);

            while (!Thread.currentThread().isInterrupted()) {
                // Accept a client connection
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected");

                // Handle client connection in a separate method
                handleClientConnection(clientSocket);
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
            // Handle exception

        } finally {
            // Close server socket
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    // Handle exception
                }
            }
        }
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPortNumber() {
        return portNumber;
    }

    public void setServerTable(SortedMap<Long, Server> serverTable) {
        this.serverTable = serverTable;
    }

    // Method to deal with the client's request
    public void handleRequest() {
        // read from the client's socket and print the received message

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
