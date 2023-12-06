package backend;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import utils.Message;
import utils.MurmurHash;
import utils.Pair;
import utils.MurmurConstants;

public class ServerManager {

    private final SortedMap<Long, String> serverTable;

    private final Set<Long> listIds = new HashSet<Long>();

    private HashMap<Long, SocketChannel> clientChannels = new HashMap<Long, SocketChannel>();

    private final ExecutorService serverConnectionPool = Executors.newFixedThreadPool(5);

    private Selector selector;

    private String ipAddress;
    private int portNumber;

    private String token;

    public ServerManager() throws IOException {
        serverTable = new TreeMap<Long, String>();

        this.portNumber = 8000;
        this.ipAddress = "127.0.0.1";
        this.findToken("backend/serverManagerToken.txt");
        System.out.println("Server manager token: " + this.token);
    }

    public void findToken(String tokenFilePath) throws IOException {
        String line;
        String storedToken = null;
        // if file not found create empty file
        try (BufferedReader reader = new BufferedReader(new FileReader(tokenFilePath))) {
            while ((line = reader.readLine()) != null) {
                storedToken = line;
            }
        }
        catch (FileNotFoundException e) {
            File file = new File(tokenFilePath);
            file.createNewFile();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        this.token = storedToken;
    }

    public void broadcastMessage(Message message) {
        System.out.println("Broadcasting message with type: " + message.getType() + " and content: " + message.getContent());
        try {
            // iterate server table and send it to the IP address
            for (Map.Entry<Long, String> entry : serverTable.entrySet()) {
                String ipAddress = entry.getValue();
                SocketChannel server = SocketChannel.open(new InetSocketAddress(ipAddress, 8000));

                message.sendMessage(server);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void addServer(String ipAddress) {
        long ipAddressHash = MurmurHash.hash_x86_32(ipAddress.getBytes(), ipAddress.getBytes().length, 0);
        serverTable.put(ipAddressHash, ipAddress);
        Message message = new Message(Message.Type.UPDATE_TABLE, this.serverTable);
        broadcastMessage(message);
    }

    public void removeServer(Server server) {
        long ipAddressHash = MurmurHash.hash_x86_32(server.getIpAddress().getBytes(), server.getIpAddress().getBytes().length, 0);
        serverTable.remove(ipAddressHash);
        Message message = new Message(Message.Type.UPDATE_TABLE, this.serverTable);
        broadcastMessage(message);
    }

    private void startServerManager() throws IOException, ClassNotFoundException {

        this.selector = Selector.open();
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.socket().bind(new InetSocketAddress(this.ipAddress, this.portNumber));
        serverChannel.register(this.selector, SelectionKey.OP_ACCEPT);
        System.out.println("Server manager started at " + this.ipAddress + ":" + this.portNumber);

        while(true){
            // printStatus();

            int readyCount = selector.select();
            if(readyCount == 0){
                continue;
            }

            Set<SelectionKey> readyKeys = selector.selectedKeys();
            Iterator iterator = readyKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = (SelectionKey) iterator.next();

                // Remove key so we don't process it twice
                iterator.remove();

                if (!key.isValid()) {
                    continue;
                }

                if (key.isAcceptable()) { // Accept client connections
                    this.accept(key);
                } else if (key.isReadable()) { // Read data from client
                    this.read(key);
                }
            }
        }
    }

    private void accept(SelectionKey key) throws IOException {

        //Retrieves the ServerSocketChannel from the SelectionKey object
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();

        //accepts the incoming client connection and configure it to operate in non-blocking mode
        SocketChannel channel = serverChannel.accept();
        channel.configureBlocking(false);

        //retrieves the Socket object from the socket channel
        Socket socket = channel.socket();
        SocketAddress remoteAddr = socket.getRemoteSocketAddress();
        System.out.println("Connected to: " + remoteAddr);

        //Register channel with selector for further IO operations - record it for read operations
        channel.register(this.selector, SelectionKey.OP_READ);

    }

    private void read(SelectionKey key) throws IOException, ClassNotFoundException {
        SocketChannel channel = (SocketChannel) key.channel();

        Message message = Message.readMessage(channel);
        System.out.println("Received message with type: " + message.getType() + " and content: " + message.getContent());

        dealWithMessage(message, channel);
    }

    private String getServerWithId(long idHashed) {

        // Check which server holds the list with the given id
        // iterate server table and send it to the IP address
        for (Map.Entry<Long, String> entry : serverTable.entrySet()) {
            if(entry.getKey() > idHashed) {
                return entry.getValue();
            }
        }
        return serverTable.get(serverTable.firstKey());
    }

    private void dealWithMessage(Message message, SocketChannel clientChannel) {
        try {
            switch (message.getType()){
                case AUTH:
                    var obj = message.getContent();
                    if(obj.getClass() == Pair.class) {
                        Pair<String, String> pair = (Pair<String, String>) obj;
                        if(pair.getSecond().equals(this.token)) {
                            Message confirmAuth = new Message(Message.Type.AUTH_OK, "Authentication successful");
                            confirmAuth.sendMessage(clientChannel);
                            addServer(pair.getFirst());
                        }
                        else {
                            Message confirmAuth = new Message(Message.Type.AUTH_FAIL, null);
                            confirmAuth.sendMessage(clientChannel);
                        }
                    }
                    break;
                case GET_LIST:
                    var obj2 = message.getContent();
                    if(obj2.getClass() == Long.class) {
                        Long idHashed = (Long) obj2;

                        String serverIp = getServerWithId(idHashed);
                        SocketChannel server = SocketChannel.open(new InetSocketAddress(serverIp, 8000));
                        Message messageToSend = new Message(Message.Type.GET_LIST, idHashed);
                        messageToSend.sendMessage(server);
                    }

                    break;
                case SEND_LIST:
                    var obj3 = message.getContent();
                    if(obj3.getClass() == Long.class) {
                        Long idHashed = (Long) obj3;

                        // send to the client the IP address of the server that holds the list with the given id
                        Message messageToSend = new Message(Message.Type.SEND_LIST, this.serverTable.get(idHashed));
                        messageToSend.sendMessage(clientChannel);
                        System.out.println("Sending message to client: " + this.serverTable.get(idHashed) + " with id: " + idHashed);
                    }
                    break;
                case CREATE_LIST:

                    // create random list id
                    Long longRandom = new Random().nextLong();
                    Long listId = MurmurHash.hash_x86_32(Long.toString(longRandom).getBytes(),
                            Long.toString(longRandom).getBytes().length, 0);

                    while(listIds.contains(listId)) {
                        listId = new Random().nextLong();
                    }

                    // get server from server ring
                    String serverIp = getServerWithId(listId);
                    SocketChannel server = SocketChannel.open(new InetSocketAddress(serverIp, 8000));

                    // add client channel to hashmap
                    clientChannels.put(listId, clientChannel);

                    // send to the server the list id and the client channel
                    Message messageToSend = new Message(Message.Type.CREATE_LIST, listId);
                    messageToSend.sendMessage(server);


                    break;
                case LIST_CREATED:

                    System.out.println("Received list created message from server: " + clientChannel.getRemoteAddress());

                    var obj4 = message.getContent();
                    if(obj4.getClass() == ArrayList.class) {

                        ArrayList<Object> listObj = (ArrayList<Object>) obj4;
                        ArrayList<Long> list = (ArrayList<Long>) listObj.get(0);

                        System.out.println("List content: ");
                        for (Object obj5 : listObj) {
                            System.out.println(obj5);
                        }

                        // get client channel from hashmap
                        SocketChannel originalClientChannel = clientChannels.get(listObj.get(1));


                        // send list to client
                        Message messageToSend2 = new Message(Message.Type.LIST_CREATED, list);
                        boolean sent = messageToSend2.sendMessage(originalClientChannel);
                        System.out.println("Message sent to client: " + originalClientChannel);

                    }
                    break;
                default:
                    break;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void printStatus(){
        System.out.println("Server manager status:");
        System.out.println("Server table:");
        for (Map.Entry<Long, String> entry : serverTable.entrySet()) {
            System.out.println(entry.getKey() + ":" + entry.getValue());
        }
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        ServerManager serverRing = new ServerManager();
        serverRing.startServerManager();
    }
    
}
