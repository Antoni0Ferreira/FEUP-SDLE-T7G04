package sdle.serverClient;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Client {
    private String serverManagerIp;
    private int serverPort;
    private InetSocketAddress serverManagerSocketAddress;

    private Selector selector;

    public Client(String serverManagerIp, int serverPort) {
        this.serverManagerIp = serverManagerIp;
        this.serverPort = serverPort;
        this.serverManagerSocketAddress = new InetSocketAddress(serverManagerIp, serverPort);
    }

    public void startClient() throws IOException, InterruptedException, ClassNotFoundException {

        this.selector = Selector.open();

        SocketChannel clientChannel = SocketChannel.open(serverManagerSocketAddress);
        clientChannel.configureBlocking(false);
        boolean exit = false;

        try {
            while(!exit){
                getClientInput();


            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    public void sendRandomLong(SocketChannel server) throws IOException {
        // Generate a random long value
        long randomLong = new Random().nextLong();
        long idHashed = MurmurHash.hash_x86_32(Long.toString(randomLong).getBytes(), Long.toString(randomLong).getBytes().length, 0);

        Message message = new Message(Message.Type.GET_LIST, idHashed);
        message.sendMessage(server);

        System.out.println("Sent long value to server: " + idHashed);
    }

    public String getClientInput() throws IOException, ClassNotFoundException {

        // Get input from user
        System.out.println("Select an option: ");
        System.out.println("1. Create new List");
        System.out.println("2. Get List");
        System.out.println("3. Exit");

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String input = reader.readLine();

        switch (input) {
            case "1":
                Message message = new Message(Message.Type.CREATE_LIST, "");
                message.sendMessage(SocketChannel.open(serverManagerSocketAddress));

                // receive list
                Message receivedMessage = Message.readMessage(SocketChannel.open(serverManagerSocketAddress));

                if(receivedMessage.getType() == Message.Type.LIST_CREATED) {
                    System.out.println("List created successfully");
                } else {
                    System.out.println("Error creating list");
                }
                break;

        }

        return input;

    }

    private void dealWithMessage(Message message, SocketChannel server) throws IOException {
        switch (message.getType()) {
            case LIST_CREATED:
                System.out.println("List created successfully");
                break;
            default:
                System.out.println("Error creating list");
                break;
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
        // Example usage
        Client client = new Client("127.0.0.1", 8000);
        client.startClient();
        //client.sendRandomLong(SocketChannel.open(client.serverManagerSocketAddress));
    }
}
