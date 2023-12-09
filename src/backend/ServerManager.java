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
    private HashMap<SocketChannel, String> serverChannels = new HashMap<>();

    private Selector selector;
    private String ipAddress;
    private int portNumber;
    private String token;
    private int requestCount;
    private int redundancyDegree;

    public ServerManager() throws IOException {
        serverTable = new TreeMap<Long, String>();

        this.portNumber = 8000;
        this.ipAddress = "127.0.0.1";
        this.requestCount = 0;
        this.redundancyDegree = 2;
        this.findToken("backend/serverManagerToken.txt");
        System.out.println("Server manager token: " + this.token);
    }

    public void setRedundancyDegree(int redundancyDegree) {
        this.redundancyDegree = redundancyDegree;
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

            // distinguish between client and server
            if(serverChannels.containsKey(channel)) {
                String ipAddress = serverChannels.get(channel);
                System.out.println("Server with IP address " + ipAddress + " has closed the connection.");
                //removeServer(ipAddress);
                serverChannels.remove(channel);
            }
            else {
                System.out.println("Client has closed the connection.");
            }

            channel.close();
        }

    }

    private ArrayList<String> getServerWithId(long idHashed) {

        ArrayList<String> serverList = new ArrayList<String>();


        // get the iterator for the server table
        Iterator<Map.Entry<Long, String>> iterator = serverTable.entrySet().iterator();
        int degree = this.redundancyDegree;

        while(degree > 0) {
            if(!iterator.hasNext()) {
                iterator = serverTable.entrySet().iterator();
            }

            Map.Entry<Long, String> entry = iterator.next();
            if(entry.getKey() > idHashed) {
                serverList.add(entry.getValue());
                degree--;
            }
        }

        return serverList;
    }

    public void initServer() {
        
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
                            serverChannels.put(clientChannel, pair.getFirst());
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
                    ArrayList<String> serverList = getServerWithId(listId);

                    // add client channel to hashmap
                    clientChannels.put(requestCount, clientChannel);

                    for (String serverIp : serverList) {
                        SocketChannel server = SocketChannel.open(new InetSocketAddress(serverIp, 8000));

                        // send to the server the list id and the client channel
                        Message messageToSend = new Message(Message.Type.CREATE_LIST, listId);
                        messageToSend.setId(requestCount);
                        messageToSend.setSender(Message.Sender.SERVER_MANAGER);
                        messageToSend.sendMessage(server);
                    }

                    break;

                case LIST_CREATED:

                    System.out.println("Received list created message from server: " + clientChannel.getRemoteAddress());

                    var obj4 = message.getContent();
                    if(obj4.getClass() == ArrayList.class) {

                        ArrayList<Object> listObj = (ArrayList<Object>) obj4;

                        // get client channel from hashmap
                        int requestId = message.getId();
                        System.out.println("Received request id: " + requestId);
                        SocketChannel originalClientChannel = clientChannels.get(requestId);

                        if(originalClientChannel != null) {
                            // send list to client
                            Message messageToSend2 = new Message(Message.Type.LIST_CREATED, listObj);
                            messageToSend2.sendMessage(originalClientChannel);
                            messageToSend2.setSender(Message.Sender.SERVER_MANAGER);
                            System.out.println("Message sent to client: " + originalClientChannel);

                            // remove client channel from hashmap
                            clientChannels.remove(requestId);
                        }

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

                        ArrayList<String> serverList2 = getServerWithId(idHashed);

                        for (String serverIp : serverList2) {
                            SocketChannel server = SocketChannel.open(new InetSocketAddress(serverIp, 8000));

                            // send to the server the list id and the client channel
                            Message messageToSend = new Message(Message.Type.DELETE_LIST, idHashed);
                            System.out.println("Sending message to server: " + serverIp);
                            messageToSend.setId(requestCount);
                            messageToSend.setSender(Message.Sender.SERVER_MANAGER);
                            messageToSend.sendMessage(server);
                        }
                    }
                    break;

                case LIST_DELETED:
                    var obj7 = message.getContent();
                    int requestId = message.getId();
                    SocketChannel originalClientChannel = clientChannels.get(requestId);

                    if(originalClientChannel != null) {
                        // send to the client the IP address of the server that holds the list with the given id
                        Message messageToSend3 = new Message(Message.Type.LIST_DELETED, null);
                        messageToSend3.setSender(Message.Sender.SERVER_MANAGER);
                        messageToSend3.sendMessage(originalClientChannel);

                        clientChannels.remove(requestId);
                    }

                    break;
                case PUSH_LIST:
                    requestCount++;

                    var obj8 = message.getContent();
                    if(obj8.getClass() == ShoppingList.class) {
                        ShoppingList list = (ShoppingList) obj8;

                        clientChannels.put(requestCount, clientChannel);
                        System.out.println("putting client channel in hashmap: " + clientChannel);

                        ArrayList<String> serverList3 = getServerWithId(list.getId());

                        for(String serverIp : serverList3) {
                            SocketChannel server = SocketChannel.open(new InetSocketAddress(serverIp, 8000));

                            // send to the server the list id and the client channel
                            Message messageToSend = new Message(Message.Type.PUSH_LIST, list);
                            System.out.println("Sending message to server: " + serverIp);
                            messageToSend.setId(requestCount);
                            messageToSend.setSender(Message.Sender.SERVER_MANAGER);
                            messageToSend.sendMessage(server);
                        }

                    }
                    break;
                case LIST_PUSHED:
                    var obj9 = message.getContent();
                    int requestId2 = message.getId();
                    SocketChannel originalClientChannel2 = clientChannels.get(requestId2);

                    if(originalClientChannel2 != null) {
                        // send to the client the IP address of the server that holds the list with the given id
                        Message messageToSend5 = new Message(Message.Type.LIST_PUSHED, null);
                        messageToSend5.setSender(Message.Sender.SERVER_MANAGER);
                        messageToSend5.sendMessage(originalClientChannel2);

                        clientChannels.remove(requestId2);
                    }

                    break;
                case PULL_LIST:
                    requestCount++;

                    var obj10 = message.getContent();
                    if(obj10.getClass() == String.class) {
                        System.out.println("Received list id: " + obj10);
                        long idHashed = Long.parseLong((String) obj10);

                        clientChannels.put(requestCount, clientChannel);
                        System.out.println("putting client channel in hashmap: " + clientChannel);

                        ArrayList<String> serverList4 = getServerWithId(idHashed);

                        for(String serverIp : serverList4) {
                            SocketChannel server = SocketChannel.open(new InetSocketAddress(serverIp, 8000));

                            // send to the server the list id and the client channel
                            Message messageToSend = new Message(Message.Type.PULL_LIST, idHashed);
                            System.out.println("Sending message to server: " + serverIp);
                            messageToSend.setId(requestCount);
                            messageToSend.setSender(Message.Sender.SERVER_MANAGER);
                            messageToSend.sendMessage(server);
                        }

                    }
                    break;
                case LIST_PULLED:

                    var obj11 = message.getContent();
                    int requestId3 = message.getId();
                    SocketChannel originalClientChannel3 = clientChannels.get(requestId3);

                    if(originalClientChannel3 != null) {
                        // send to the client the IP address of the server that holds the list with the given id
                        Message messageToSend6 = new Message(Message.Type.LIST_PULLED, obj11);
                        messageToSend6.setSender(Message.Sender.SERVER_MANAGER);
                        messageToSend6.sendMessage(originalClientChannel3);

                        clientChannels.remove(requestId3);
                    }
                    break;
                case LIST_NOT_FOUND:

                    int requestId4 = message.getId();
                    SocketChannel originalClientChannel4 = clientChannels.get(requestId4);

                    if(originalClientChannel4 != null) {
                        // send to the client the IP address of the server that holds the list with the given id
                        Message messageToSend7 = new Message(Message.Type.LIST_NOT_FOUND, null);
                        messageToSend7.setSender(Message.Sender.SERVER_MANAGER);
                        messageToSend7.sendMessage(originalClientChannel4);

                        clientChannels.remove(requestId4);
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
