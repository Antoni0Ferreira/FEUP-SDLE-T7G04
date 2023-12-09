package backend.server4;



import shopping.ShoppingList;
import utils.Database;
import utils.Message;
import utils.MurmurHash;
import utils.Pair;

import java.io.*;
import java.net.ServerSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

public class Server {
    private String ipAddress;
    private int portNumber;
    private ServerSocket serverSocket;
    private Selector selector;
    private Long idHashed;
    private SortedMap<Long, String> serverTable;

    private InetSocketAddress serverManagerSocketAddress;

    private String token;

    public Server(String ipAddress, int portNumber) throws IOException {
        this.ipAddress = ipAddress;
        this.portNumber = portNumber;
        this.serverTable = new TreeMap<Long, String>();
        this.serverManagerSocketAddress = new InetSocketAddress("127.0.0.1", 8000);

        this.findToken("backend/serverToken.txt");
        System.out.println("Server token: " + this.token);

        this.idHashed = MurmurHash.hash_x86_32(this.ipAddress.getBytes(), this.ipAddress.getBytes().length, 0);

    }

    // copy constructor
    public Server(Server server) {
        this.ipAddress = server.getIpAddress();
        this.portNumber = server.getPortNumber();
        this.serverSocket = server.serverSocket;
        this.selector = server.selector;
        this.serverTable = server.serverTable;
        this.serverManagerSocketAddress = server.serverManagerSocketAddress;
        this.token = server.token;
    }

    private void startServer() throws IOException, ClassNotFoundException {
        // Create a selector
        this.selector = Selector.open();

        // Register the server socket channel, indicating an interest in accepting new connections
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);

        serverChannel.socket().bind(new InetSocketAddress(this.ipAddress, this.portNumber));

        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        if(!authenticate()){
            System.out.println("Authentication failed");
            System.exit(1);
        }

        // Wait for an event one of the registered channels
        while (true) {

            // This may block for a long time. Upon returning, the selected set contains keys of the ready channels
            int numKeys = selector.select();

            if (numKeys == 0) {
                continue;
            }

            // Get the keys corresponding to the activity that has been detected
            // This step is necessary to retrieve the I/O channel corresponding to the event
            // that has been detected
            for (SelectionKey key : selector.selectedKeys()) {
                if (key.isAcceptable()) {
                    accept(key);
                } else if (key.isReadable()) {
                    read(key);
                } else if (key.isWritable()) {
                    // write(key);
                }
            }

            // printStatus();

            // Remove the selected keys, because we've dealt with them
            selector.selectedKeys().clear();
        }

    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPortNumber() {
        return portNumber;
    }

    public void setServerTable(SortedMap<Long, String> serverTable) {
        this.serverTable = serverTable;
    }

    public boolean authenticate() throws IOException, ClassNotFoundException {
        SocketChannel serverManager = SocketChannel.open(serverManagerSocketAddress);

        Pair<String, String> messageContent = new Pair<String, String>(this.ipAddress, this.token);

        // create Message
        Message message = new Message(Message.Type.AUTH, messageContent);

        // send Message
        if(message.sendMessage(serverManager)){
            System.out.println("Sent message to server manager: " + message);
        }
        else {
            System.out.println("Failed to send message to server manager: " + message);
        }

        // receive Message
        Message receivedMessage = Message.readMessage(serverManager);

        switch (receivedMessage.getType()) {
            case AUTH_OK:
                System.out.println("Received message with type: " + receivedMessage.getType() + " and content: " + receivedMessage.getContent());
                break;
            case AUTH_FAIL:
                System.out.println("Authentication failed");
                break;
            default:
                System.out.println("Unknown message type");
                break;
        }

        return true;
    }

    public void accept(SelectionKey key) throws IOException {
        // For an accept to be pending the channel must be a server socket channel.
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

        // Accept the connection and make it non-blocking
        SocketChannel socketChannel = serverSocketChannel.accept();
        Socket socket = socketChannel.socket();
        socketChannel.configureBlocking(false);

        // Register the new SocketChannel with our Selector, indicating
        // we'd like to be notified when there's data waiting to be read
        socketChannel.register(this.selector, SelectionKey.OP_READ);
    }

    private void read(SelectionKey key) throws IOException, ClassNotFoundException {
        SocketChannel channel = (SocketChannel) key.channel();

        if (!channel.isOpen()) {
            System.out.println("Channel is closed");
            return;
        }

        Message message = Message.readMessage(channel);

        System.out.println("Received message with type: " + message.getType() + " and content: " + message.getContent());

        dealWithMessage(message, channel);

    }

    private void dealWithMessage(Message message, SocketChannel channel){
        try {
            switch (message.getType()){
                case UPDATE_TABLE:
                    var obj = message.getContent();
                    if(obj.getClass() == TreeMap.class) {
                        SortedMap<Long, String> serverTable = (SortedMap<Long, String>) obj;
                        this.setServerTable(serverTable);
                    }
                    break;

                case CREATE_LIST:
                    var obj3 = message.getContent();
                    if(obj3.getClass() == Long.class) {

                        Long listId = (Long) obj3;

                        // create new shopping list
                        ShoppingList shoppingList = new ShoppingList();
                        shoppingList.setId(listId);

                        // create content list to send
                        List<Object> content = new ArrayList<Object>();
                        content.add(shoppingList);
                        content.add(listId);
                        System.out.println("Server manager socket address: " + serverManagerSocketAddress);
                        SocketChannel serverManager = SocketChannel.open(serverManagerSocketAddress);

                        // send list to ServerManager
                        Message messageToSend = new Message(Message.Type.LIST_CREATED, content);
                        messageToSend.setId(message.getId());
                        messageToSend.sendMessage(serverManager);

                        // store list in database
                        Database.writeToFile(shoppingList, "backend/server4/" + listId.toString() + ".ser" );

                    }

                    break;
                case DELETE_LIST:
                    var obj4 = message.getContent();
                    if(obj4.getClass() == Long.class) {

                        Long listId = (Long) obj4;

                        // check if file exists
                        File file = new File("backend/server4/" + listId.toString() + ".ser");

                        if(!file.exists()) {
                            System.out.println("List not found");

                            SocketChannel serverManager = SocketChannel.open(serverManagerSocketAddress);

                            Message messageToSend = new Message(Message.Type.LIST_NOT_FOUND, null);
                            messageToSend.setId(message.getId());
                            messageToSend.sendMessage(serverManager);
                            break;
                        }

                        // delete list from database
                        Database.deleteFile("backend/server4/" + listId.toString() + ".ser" );

                        System.out.println("Server manager socket address: " + serverManagerSocketAddress);
                        SocketChannel serverManager = SocketChannel.open(serverManagerSocketAddress);

                        // send list to ServerManager
                        Message messageToSend = new Message(Message.Type.LIST_DELETED, null);
                        messageToSend.setId(message.getId());
                        messageToSend.sendMessage(serverManager);

                    }

                    break;
                case PUSH_LIST:
                    var obj5 = message.getContent();
                    if(obj5.getClass() == ShoppingList.class) {

                        ShoppingList list = (ShoppingList) obj5;
                        Long listId = list.getId();

                        // check if file exists
                        File file = new File("backend/server4/" + listId.toString() + ".ser");
                        if(!file.exists()) {
                            Database.writeToFile(list, "backend/server4/" + listId.toString() + ".ser" );
                            break;
                        }

                        // read list from database
                        Object listObj  = Database.readFromFile("backend/server4/"
                                + listId.toString() + ".ser");

                        if (listObj.getClass() == ShoppingList.class) {
                            ShoppingList shoppingList = (ShoppingList) listObj;

                            // merge lists
                            shoppingList.mergeShoppingList(list.getShoppingList());

                            // delete list from database if exists else create
                            Database.deleteFile("backend/server4/" + listId.toString() + ".ser" );

                            // store list in database
                            Database.writeToFile(shoppingList, "backend/server4/" + listId.toString() + ".ser" );

                            System.out.println("Server manager socket address: " + serverManagerSocketAddress);
                            SocketChannel serverManager = SocketChannel.open(serverManagerSocketAddress);

                            // send list to ServerManager
                            Message messageToSend = new Message(Message.Type.LIST_PUSHED, null);
                            messageToSend.setId(message.getId());
                            messageToSend.sendMessage(serverManager);

                        } else {
                            System.out.println("Error reading list");
                            break;
                        }
                    }

                    break;
                case PULL_LIST:
                    var obj6 = message.getContent();
                    if(obj6.getClass() == Long.class) {

                        Long listId = (Long) obj6;

                        // check if file exists
                        File file = new File("backend/server4/" + listId.toString() + ".ser");
                        if(!file.exists()) {
                            System.out.println("List not found");

                            SocketChannel serverManager = SocketChannel.open(serverManagerSocketAddress);

                            Message messageToSend = new Message(Message.Type.LIST_NOT_FOUND, null);
                            messageToSend.setId(message.getId());
                            messageToSend.sendMessage(serverManager);
                            break;
                        }

                        Object listObj  = Database.readFromFile("backend/server4/"
                                + listId.toString() + ".ser");

                        if (listObj.getClass() == ShoppingList.class) {

                            SocketChannel serverManager = SocketChannel.open(serverManagerSocketAddress);
                            ShoppingList list = (ShoppingList) listObj;

                            Message messageToSend = new Message(Message.Type.LIST_PULLED, list);
                            messageToSend.setId(message.getId());
                            messageToSend.sendMessage(serverManager);
                            System.out.println("Sent list to client: " + list);

                        } else {
                            System.out.println("Error reading list");
                            break;
                        }
                    }
                    break;
/*                case SYNC:
                    var obj7 = message.getContent();
                    if(obj7.getClass() == ShoppingList.class) {

                        ShoppingList list = (ShoppingList) obj7;
                        Long listId = list.getId();

                        // check if file exists
                        File file = new File("backend/server4/" + listId.toString() + ".ser");
                        if(!file.exists()) {
                            Database.writeToFile(list, "backend/server4/" + listId.toString() + ".ser" );
                            break;
                        }

                        // read list from database
                        Object listObj  = Database.readFromFile("backend/server4/"
                                + listId.toString() + ".ser");

                        if (listObj.getClass() == ShoppingList.class) {
                            ShoppingList shoppingList = (ShoppingList) listObj;

                            // merge lists
                            shoppingList.mergeShoppingList(list.getShoppingList());

                            // delete list from database if exists else create
                            Database.deleteFile("backend/server4/" + listId.toString() + ".ser" );

                            // store list in database
                            Database.writeToFile(shoppingList, "backend/server4/" + listId.toString() + ".ser" );

                            System.out.println("Server manager socket address: " + serverManagerSocketAddress);
                            SocketChannel serverManager = SocketChannel.open(serverManagerSocketAddress);

                            // send list to ServerManager
                            Message messageToSend = new Message(Message.Type.SYNC_OK, shoppingList);
                            messageToSend.setId(message.getId());
                            messageToSend.sendMessage(serverManager);

                        } else {
                            System.out.println("Error reading list");
                            break;
                        }
                    }
                    break;*/
                default:
                    System.out.println("Unknown message type");
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    private void printStatus(){
        System.out.println("Server status:");
        System.out.println("Server table:");
        for (var entry : serverTable.entrySet()) {
            System.out.println(entry.getKey() + " " + entry.getValue());
        }
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

    public void sendMessage(String message, SocketChannel channel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.put(message.getBytes());
        channel.write(buffer);

        System.out.println("Sent message to client: " + message);
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        // receive port and ip as argument
        // create a server object
        if(args.length != 2) {
            System.out.println("Usage: java Server <ipAddress> <portNumber>");
            System.exit(1);
        }
        String ipAddress = args[0];
        int portNumber = Integer.parseInt(args[1]);
        Server server = new Server(ipAddress, portNumber);
        server.startServer();
    }
}