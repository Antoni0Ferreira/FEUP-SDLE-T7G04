package backend.server1;



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

    private String filepathPrefix;
    private String ipAddress;
    private int portNumber;
    private ServerSocket serverSocket;
    private Selector selector;
    private Long idHashed;
    private SortedMap<Long, String> serverTable;
    private InetSocketAddress serverManagerSocketAddress;
    private HashSet<Long> listIds;
    private HashMap<SocketChannel, String> serverChannels = new HashMap<>();
    private String token;
    private int redundancyDegree;

    public Server(String ipAddress, int portNumber) throws IOException {
        this.ipAddress = ipAddress;
        this.portNumber = portNumber;
        this.serverTable = new TreeMap<Long, String>();
        this.serverManagerSocketAddress = new InetSocketAddress("127.0.0.1", 8000);
        this.listIds = new HashSet<Long>();
        this.redundancyDegree = 2;
        this.filepathPrefix = "backend/server1/";

        this.findToken("backend/serverToken.txt");
        //System.out.println("Server token: " + this.token);

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
        this.listIds = server.listIds;
        this.redundancyDegree = server.redundancyDegree;
        this.filepathPrefix = server.filepathPrefix;
    }

    public void setRedundancyDegree(int redundancyDegree) {
        this.redundancyDegree = redundancyDegree;
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
            //System.out.println("Authentication failed");
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
            //System.out.println("Sent message to server manager: " + message);
        }
        else {
            //System.out.println("Failed to send message to server manager: " + message);
        }

        // receive Message
        Message receivedMessage = Message.readMessage(serverManager);

        switch (receivedMessage.getType()) {
            case AUTH_OK:
                //System.out.println("Received message with type: " + receivedMessage.getType() + " and content: " + receivedMessage.getContent());
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

        try {
            Message message = Message.readMessage(channel);
            System.out.println("Received message with type: " + message.getType() + " and content: " + message.getContent());
            dealWithMessage(message, channel);
        } catch (EOFException e) {
            System.out.println("Server disconnected");
            channel.close();
        } catch (IOException | ClassNotFoundException e) {
            if(serverChannels.containsKey(channel)) {
                String ipAddress = serverChannels.get(channel);
                System.out.println("Server with IP address " + ipAddress + " has closed the connection.");
                //removeServer(ipAddress);

            }

            channel.close();
        }




    }

    private Set<String> getServerWithId(long idHashed) {

        Set<String> serverList = new HashSet<String>();

        // get the iterator for the server table
        Iterator<Map.Entry<Long, String>> iterator = serverTable.entrySet().iterator();
        int degree = this.redundancyDegree;

        while(degree > 0) {
            if(!iterator.hasNext()) {
                idHashed = 0;
                iterator = serverTable.entrySet().iterator();
            }

            Map.Entry<Long, String> entry = iterator.next();
            if(entry.getKey() >= idHashed) {
                serverList.add(entry.getValue());
                degree--;
            }
        }

        System.out.println("Server list: " + serverList);

        return serverList;
    }

    private Long calculateLeftServerLimit(Long newServerId) {
        // get the iterator for the server table
        Iterator<Map.Entry<Long, String>> iterator = serverTable.entrySet().iterator();
        Iterator<Map.Entry<Long, String>> newServerIt = null;

        if(this.serverTable.size() <= this.redundancyDegree) {
            return newServerId + 1;
        }

        int numNexts = this.serverTable.size() - this.redundancyDegree;
        long leftServerLimit = 0;

        if(iterator.hasNext()) {
            Map.Entry<Long, String> entry = iterator.next();
            while(!Objects.equals(entry.getKey(), newServerId)){

                entry = iterator.next();
            }
            newServerIt = iterator;
        }

        while(numNexts > 0) {
            if(!newServerIt.hasNext()) {
                newServerIt = serverTable.entrySet().iterator();
            }
            Map.Entry<Long, String> entry = newServerIt.next();
            leftServerLimit = entry.getKey();
            System.out.println("Left server limit: " + leftServerLimit);
            numNexts--;
        }


        return leftServerLimit;
    }

    private boolean listInRange(Long listId, Long leftLimit, Long rightLimit ) {
        if(rightLimit < leftLimit){
            // list should be sent if list id < right limit or list id > left limit
            return listId > leftLimit || listId < rightLimit;
        } else {
            // list should be sent if list id < right limit and list id > left limit
            return listId > leftLimit && listId < rightLimit;
        }
    }

    private void dealWithMessage(Message message, SocketChannel channel) throws IOException, ClassNotFoundException {
        try {
            switch (message.getType()){
                case UPDATE_TABLE:
                    var obj = message.getContent();
                    if(obj.getClass() == TreeMap.class) {
                        SortedMap<Long, String> serverTable = (SortedMap<Long, String>) obj;
                        this.setServerTable(serverTable);
                    }
                    break;

                case ADD_SERVER:
                    var obj2 = message.getContent();
                    if(obj2.getClass() == Long.class){
                        Long newServerId = (Long) obj2;

                        Long rightNewServerLimit = newServerId;
                        Long leftNewServerLimit = calculateLeftServerLimit(newServerId);

                        System.out.println("New Server Left limit: " + leftNewServerLimit);
                        System.out.println("New Server Right limit: " + newServerId);

                        System.out.println("------------------");

                        Long leftServerLimit = calculateLeftServerLimit(this.idHashed);
                        Long rightServerLimit = this.idHashed;

                        System.out.println("Server Left limit: " + leftServerLimit);
                        System.out.println("Server Right limit: " + this.idHashed);

                        // print list ids
                        System.out.println("\nList ids:");
                        for(Long listId : listIds){
                            System.out.println(listId);
                        }


                        // iterate through list ids
                        Iterator<Long> iterator = listIds.iterator();

                        while(iterator.hasNext()){
                            Long listId = iterator.next();

                            System.out.printf("List id: %d\n", listId);

                            // if list is in range of new server
                            if(listInRange(listId, leftNewServerLimit, rightNewServerLimit)){
                                // send list to server and remove from database
                                Object listObj  = Database.readFromFile(this.filepathPrefix
                                        + listId.toString() + ".ser");

                                if (listObj.getClass() == ShoppingList.class  ) {
                                    ShoppingList shoppingList = (ShoppingList) listObj;

                                    // delete list from database if exists else create
                                    if(!listInRange(listId, leftServerLimit, rightServerLimit)){
                                        Database.deleteFile(this.filepathPrefix + listId.toString() + ".ser" );
                                    }

                                    try {
                                        String socketAddress = serverTable.get(newServerId);
                                        SocketChannel serverChannel = SocketChannel.open(
                                                new InetSocketAddress(socketAddress, 8000));

                                        Message messageToSend = new Message(Message.Type.PUSH_LIST, shoppingList);
                                        messageToSend.setId(message.getId());
                                        messageToSend.setSender(Message.Sender.SERVER);
                                        messageToSend.sendMessage(serverChannel);
                                        serverChannels.put(serverChannel, socketAddress);

                                    } catch (IOException e) {
                                        System.out.println("Server is unavailable.");

                                    }


                                    // after sending list to new server, close connection
                                }
                                else {
                                    System.out.println("Error reading list");
                                    break;
                                }
                            }

                        }

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
                        messageToSend.setSender(Message.Sender.SERVER);
                        messageToSend.sendMessage(serverManager);

                        // store list in database
                        Database.writeToFile(shoppingList, this.filepathPrefix + listId.toString() + ".ser" );
                        listIds.add(listId);

                    }

                    break;
                case DELETE_LIST:
                    var obj4 = message.getContent();
                    if(obj4.getClass() == Long.class) {

                        Long listId = (Long) obj4;

                        // check if file exists
                        File file = new File(this.filepathPrefix + listId.toString() + ".ser");

                        if(!file.exists()) {
                            System.out.println("List not found");

                            SocketChannel serverManager = SocketChannel.open(serverManagerSocketAddress);

                            Message messageToSend = new Message(Message.Type.LIST_NOT_FOUND, null);
                            messageToSend.setId(message.getId());
                            messageToSend.setSender(Message.Sender.SERVER);
                            messageToSend.sendMessage(serverManager);
                            break;
                        }

                        // delete list from database
                        Database.deleteFile(this.filepathPrefix + listId.toString() + ".ser" );
                        listIds.remove(listId);

                        System.out.println("Server manager socket address: " + serverManagerSocketAddress);
                        SocketChannel serverManager = SocketChannel.open(serverManagerSocketAddress);

                        // send list to ServerManager
                        Message messageToSend = new Message(Message.Type.LIST_DELETED, null);
                        messageToSend.setId(message.getId());
                        messageToSend.setSender(Message.Sender.SERVER);
                        messageToSend.sendMessage(serverManager);

                    }

                    break;
                case PUSH_LIST:
                    var obj5 = message.getContent();
                    if(obj5.getClass() == ShoppingList.class) {

                        ShoppingList list = (ShoppingList) obj5;
                        Long listId = list.getId();

                        System.out.println("Server manager socket address: " + serverManagerSocketAddress);
                        SocketChannel serverManager = SocketChannel.open(serverManagerSocketAddress);

                        // check if file exists
                        File file = new File(this.filepathPrefix + listId.toString() + ".ser");
                        if(!file.exists()) {
                            Database.writeToFile(list, this.filepathPrefix + listId.toString() + ".ser" );

                            // send list to ServerManager
                            Message messageToSend = new Message(Message.Type.LIST_PUSHED, listId);
                            messageToSend.setId(message.getId());
                            messageToSend.setSender(Message.Sender.SERVER);
                            messageToSend.sendMessage(serverManager);
                            listIds.add(listId);
                            break;
                        }

                        // read list from database
                        Object listObj  = Database.readFromFile(this.filepathPrefix
                                + listId.toString() + ".ser");

                        if (listObj.getClass() == ShoppingList.class) {
                            ShoppingList shoppingList = (ShoppingList) listObj;

                            // merge lists
                            shoppingList.mergeShoppingList(list.getShoppingList());

                            // delete list from database if exists else create
                            Database.deleteFile(this.filepathPrefix + listId.toString() + ".ser" );

                            // store list in database
                            Database.writeToFile(shoppingList, this.filepathPrefix + listId.toString() + ".ser" );


                            // send list to ServerManager
                            Message messageToSend = new Message(Message.Type.LIST_PUSHED, listId);
                            messageToSend.setId(message.getId());
                            messageToSend.setSender(Message.Sender.SERVER);
                            messageToSend.sendMessage(serverManager);

                            System.out.println("\nList ids:");

                            for(Long id : listIds){
                                System.out.println(id);
                            }

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
                        File file = new File(this.filepathPrefix + listId.toString() + ".ser");
                        if(!file.exists()) {
                            System.out.println("List not found");

                            SocketChannel serverManager = SocketChannel.open(serverManagerSocketAddress);

                            Message messageToSend = new Message(Message.Type.LIST_NOT_FOUND, null);
                            messageToSend.setId(message.getId());
                            messageToSend.sendMessage(serverManager);
                            break;
                        }

                        Object listObj  = Database.readFromFile(this.filepathPrefix
                                + listId.toString() + ".ser");

                        if (listObj.getClass() == ShoppingList.class) {

                            SocketChannel serverManager = SocketChannel.open(serverManagerSocketAddress);
                            ShoppingList list = (ShoppingList) listObj;

                            Message messageToSend = new Message(Message.Type.LIST_PULLED, list);
                            messageToSend.setId(message.getId());
                            messageToSend.setSender(Message.Sender.SERVER);
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
                        File file = new File("backend/server1/" + listId.toString() + ".ser");
                        if(!file.exists()) {
                            Database.writeToFile(list, "backend/server1/" + listId.toString() + ".ser" );
                            break;
                        }

                        // read list from database
                        Object listObj  = Database.readFromFile("backend/server1/"
                                + listId.toString() + ".ser");

                        if (listObj.getClass() == ShoppingList.class) {
                            ShoppingList shoppingList = (ShoppingList) listObj;

                            // merge lists
                            shoppingList.mergeShoppingList(list.getShoppingList());

                            // delete list from database if exists else create
                            Database.deleteFile("backend/server1/" + listId.toString() + ".ser" );

                            // store list in database
                            Database.writeToFile(shoppingList, "backend/server1/" + listId.toString() + ".ser" );

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
