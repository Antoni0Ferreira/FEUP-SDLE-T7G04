package backend;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.spi.URLStreamHandlerProvider;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import shopping.ShoppingList;
import utils.Message;
import utils.MurmurHash;
import utils.Pair;
import utils.MurmurConstants;

public class ServerManager {

    private final SortedMap<Long, String> serverTable;

    private final Set<Long> listIds = new HashSet<Long>();

    private HashMap<Integer, SocketChannel> clientChannels = new HashMap<Integer, SocketChannel>();

    private final ExecutorService serverConnectionPool = Executors.newFixedThreadPool(5);

    private Selector selector;
    private String ipAddress;
    private int portNumber;
    private String token;
    private int requestCount;

    public ServerManager() throws IOException {
        serverTable = new TreeMap<Long, String>();

        this.portNumber = 8000;
        this.ipAddress = "127.0.0.1";
        requestCount = 0;
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

    public void removeServer(String ipAddress) {
        long ipAddressHash = MurmurHash.hash_x86_32(ipAddress.getBytes(), ipAddress.getBytes().length, 0);
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

        // check if channel is open
        if(!key.channel().isOpen()) {
            System.out.println("Channel is closed");
            return;
        }

        SocketChannel channel = (SocketChannel) key.channel();

        try {
            Message message = Message.readMessage(channel);
            System.out.println("Received message with type: " + message.getType() + " and content: " + message.getContent());
            dealWithMessage(message, channel);
            // Process the message
        } catch (EOFException e) {
            System.out.println("Client has closed the connection.");
            channel.close();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error reading from client.");
            channel.close();
        }

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

                case CREATE_LIST:

                    requestCount++;

                    // create random list id
                    long longRandom = new Random().nextLong();
                    long listId = MurmurHash.hash_x86_32(Long.toString(longRandom).getBytes(),
                            Long.toString(longRandom).getBytes().length, 0);

                    while(listIds.contains(listId)) {
                        listId = new Random().nextLong();
                    }

                    // get server from server ring
                    String serverIp = getServerWithId(listId);
                    SocketChannel server = SocketChannel.open(new InetSocketAddress(serverIp, 8000));

                    // add client channel to hashmap
                    clientChannels.put(requestCount, clientChannel);

                    // send to the server the list id and the client channel
                    Message messageToSend = new Message(Message.Type.CREATE_LIST, listId);
                    messageToSend.setId(requestCount);
                    messageToSend.sendMessage(server);


                    break;

                case LIST_CREATED:

                    System.out.println("Received list created message from server: " + clientChannel.getRemoteAddress());

                    var obj4 = message.getContent();
                    if(obj4.getClass() == ArrayList.class) {

                        ArrayList<Object> listObj = (ArrayList<Object>) obj4;

                        // get client channel from hashmap
                        int requestId = message.getId();
                        SocketChannel originalClientChannel = clientChannels.get(requestId);

                        // send list to client
                        Message messageToSend2 = new Message(Message.Type.LIST_CREATED, listObj);
                        messageToSend2.sendMessage(originalClientChannel);
                        System.out.println("Message sent to client: " + originalClientChannel);

                        // remove client channel from hashmap
                        clientChannels.remove(requestId);
                    }
                    break;

                case DELETE_LIST:
                    requestCount++;

                    var obj6 = message.getContent();
                    if(obj6.getClass() == String.class) {
                        System.out.println("Received list id: " + obj6);
                        long idHashed = Long.parseLong((String) obj6);

                        clientChannels.put(requestCount, clientChannel);
                        System.out.println("putting client channel in hashmap: " + clientChannel);

                        String serverIp2 = getServerWithId(idHashed);
                        SocketChannel server2 = SocketChannel.open(new InetSocketAddress(serverIp2, 8000));
                        Message messageToSend3 = new Message(Message.Type.DELETE_LIST, idHashed);
                        System.out.println("Sending message to server: " + serverIp2);
                        messageToSend3.setId(requestCount);
                        messageToSend3.sendMessage(server2);
                    }
                    break;

                case LIST_DELETED:
                    var obj7 = message.getContent();
                    int requestId = message.getId();
                    SocketChannel originalClientChannel = clientChannels.get(requestId);

                    // send to the client the IP address of the server that holds the list with the given id
                    Message messageToSend4 = new Message(Message.Type.LIST_DELETED, null);
                    messageToSend4.sendMessage(originalClientChannel);

                    clientChannels.remove(requestId);
                    break;
                case PUSH_LIST:
                    requestCount++;

                    var obj8 = message.getContent();
                    if(obj8.getClass() == ShoppingList.class) {
                        ShoppingList list = (ShoppingList) obj8;

                        clientChannels.put(requestCount, clientChannel);
                        System.out.println("putting client channel in hashmap: " + clientChannel);

                        String serverIp3 = getServerWithId(list.getId());
                        SocketChannel server3 = SocketChannel.open(new InetSocketAddress(serverIp3, 8000));
                        Message messageToSend5 = new Message(Message.Type.PUSH_LIST, list);

                        System.out.println("Sending message to server: " + serverIp3);
                        messageToSend5.setId(requestCount);
                        messageToSend5.sendMessage(server3);
                    }
                    break;
                case LIST_PUSHED:
                    var obj9 = message.getContent();
                    int requestId2 = message.getId();
                    SocketChannel originalClientChannel2 = clientChannels.get(requestId2);

                    // send to the client the IP address of the server that holds the list with the given id
                    Message messageToSend6 = new Message(Message.Type.LIST_PUSHED, null);
                    messageToSend6.sendMessage(originalClientChannel2);

                    clientChannels.remove(requestId2);
                    break;
                case PULL_LIST:
                    requestCount++;

                    var obj10 = message.getContent();
                    if(obj10.getClass() == String.class) {
                        System.out.println("Received list id: " + obj10);
                        long idHashed = Long.parseLong((String) obj10);

                        clientChannels.put(requestCount, clientChannel);
                        System.out.println("putting client channel in hashmap: " + clientChannel);

                        String serverIp4 = getServerWithId(idHashed);
                        SocketChannel server4 = SocketChannel.open(new InetSocketAddress(serverIp4, 8000));
                        Message messageToSend7 = new Message(Message.Type.PULL_LIST, idHashed);
                        System.out.println("Sending message to server: " + serverIp4);
                        messageToSend7.setId(requestCount);
                        messageToSend7.sendMessage(server4);
                    }
                    break;
                case LIST_PULLED:

                    var obj11 = message.getContent();
                    int requestId3 = message.getId();
                    SocketChannel originalClientChannel3 = clientChannels.get(requestId3);

                    // send to the client the IP address of the server that holds the list with the given id
                    Message messageToSend8 = new Message(Message.Type.LIST_PULLED, obj11);
                    messageToSend8.sendMessage(originalClientChannel3);

                    clientChannels.remove(requestId3);
                    break;
                case LIST_NOT_FOUND:

                    int requestId4 = message.getId();
                    SocketChannel originalClientChannel4 = clientChannels.get(requestId4);

                    // send to the client the IP address of the server that holds the list with the given id
                    Message messageToSend9 = new Message(Message.Type.LIST_NOT_FOUND, null);
                    messageToSend9.sendMessage(originalClientChannel4);

                    clientChannels.remove(requestId4);
                    break;
/*                case SYNC:

                    requestCount++;

                    var obj12 = message.getContent();
                    if(obj12.getClass() == ShoppingList.class) {
                        ShoppingList list = (ShoppingList) obj12;

                        clientChannels.put(requestCount, clientChannel);
                        System.out.println("putting client channel in hashmap: " + clientChannel);

                        String serverIp5 = getServerWithId(list.getId());
                        SocketChannel server5 = SocketChannel.open(new InetSocketAddress(serverIp5, 8000));
                        Message messageToSend10 = new Message(Message.Type.SYNC, list);

                        System.out.println("Sending message to server: " + serverIp5);
                        messageToSend10.setId(requestCount);
                        messageToSend10.sendMessage(server5);
                    }
                    break;
                case SYNC_OK:

                    var obj13 = message.getContent();
                    int requestId5 = message.getId();
                    SocketChannel originalClientChannel5 = clientChannels.get(requestId5);

                    // send to the client the IP address of the server that holds the list with the given id
                    Message messageToSend11 = new Message(Message.Type.SYNC_OK, obj13);
                    messageToSend11.sendMessage(originalClientChannel5);

                    clientChannels.remove(requestId5);
                    break;
                default:
                    break;
            }*/
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
