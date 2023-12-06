package client;

import utils.Message;
import utils.MurmurHash;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Client {
    private String serverManagerIp;
    private int serverPort;
    private InetSocketAddress serverAddress;

    public Client(String serverManagerIp, int serverPort) {
        this.serverManagerIp = serverManagerIp;
        this.serverPort = serverPort;
        this.serverAddress = new InetSocketAddress(serverManagerIp, serverPort);
    }

    public void startClient() throws IOException, InterruptedException {
        SocketChannel server = SocketChannel.open(serverAddress);


        // Close the socket
        server.close();
    }

    public void sendRandomLong(SocketChannel server) throws IOException {
        // Generate a random long value
        long randomLong = new Random().nextLong();
        long idHashed = MurmurHash.hash_x86_32(Long.toString(randomLong).getBytes(), Long.toString(randomLong).getBytes().length, 0);

        Message message = new Message(Message.Type.GET_LIST, idHashed);
        message.sendMessage(server);

        System.out.println("Sent long value to server: " + idHashed);
    }

    public void getClientInput() throws IOException, ClassNotFoundException {

        // Get input from user
        System.out.println("Select an option: ");
        System.out.println("1. Create new List");
        System.out.println("2. Get List");
        System.out.println("3. Exit");

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String input = reader.readLine();

        switch (input) {
            case "1":
                Message message = new Message(Message.Type.CREATE_LIST, null);
                message.sendMessage(SocketChannel.open(serverAddress));

                // receive list
                Message receivedMessage = Message.readMessage(SocketChannel.open(serverAddress));

                if(receivedMessage.getType() == Message.Type.LIST_CREATED) {
                    System.out.println("List created successfully");
                } else {
                    System.out.println("Error creating list");
                }



                break;

        }

    }

    public static void main(String[] args) throws IOException, InterruptedException {
        // Example usage
        Client client = new Client("127.0.0.1", 8000);
        client.startClient();
        client.sendRandomLong(SocketChannel.open(client.serverAddress));
    }
}