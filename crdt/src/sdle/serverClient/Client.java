package sdle.serverClient;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
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

/*        for(int i = 0; i < 10; i++) {
            sendRandomLong(server);
            TimeUnit.SECONDS.sleep(1);
        }
        // Close the socket
        server.close();*/
    }

    public void sendRandomLong(SocketChannel server) throws IOException {
        // Generate a random long value
        long randomLong = new Random().nextLong();
        long idHashed = MurmurHash.hash_x86_32(Long.toString(randomLong).getBytes(), Long.toString(randomLong).getBytes().length, 0);

        Message message = new Message(Message.Type.GET_LIST, idHashed);
        message.sendMessage(server);

        System.out.println("Sent long value to server: " + idHashed);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        // Example usage
        Client client = new Client("127.0.0.1", 8000);
        client.startClient();
        client.sendRandomLong(SocketChannel.open(client.serverAddress));
    }
}
