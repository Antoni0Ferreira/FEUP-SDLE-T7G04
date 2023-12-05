package sdle.serverClient;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Random;

public class Client {
    private String serverIp;
    private int serverPort;

    public Client(String serverIp, int serverPort) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }

    public void sendRandomLong() {
        try (Socket socket = new Socket(serverIp, serverPort);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

            // Generate a random long value
            long randomLong = new Random().nextLong();

            // Send the long value to the server
            dos.writeLong(randomLong);
            System.out.println("Sent random long value: " + randomLong);

        } catch (IOException e) {
            System.out.println("Error connecting to server: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        // Example usage
        Client client = new Client("127.0.0.1", 8000); // Replace with your server's IP and port
        client.sendRandomLong();
    }
}
