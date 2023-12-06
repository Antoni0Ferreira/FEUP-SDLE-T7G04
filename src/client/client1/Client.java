package client.client1;

import utils.Message;
import utils.MurmurHash;
import utils.Database;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Random;

public class Client {
    private String filepathPrefix = "client1-db/";
    private String serverManagerIp;
    private int serverPort;
    private InetSocketAddress serverManagerSocketAddress;
    private Selector selector;
    private boolean insideList = false;

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

        while(!exit){
            InputObj input = getClientInput();
            communicate(input, clientChannel);
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

    public InputObj getClientInput() throws IOException, ClassNotFoundException {

        if(!insideList){
            return getInitialInput();
        }
        else{
            return getInsideListInput();
        }
    }

    private InputObj getInitialInput() throws IOException{
        System.out.println("Select an option: ");
        System.out.println("1. Create new List");
        System.out.println("2. Get List");
        System.out.println("3. Delete list");
        System.out.println("4. Push list");
        System.out.println("5. Pull list");
        System.out.println("9 Exit");

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String input = reader.readLine();

        InputObj inputObj = new InputObj(input);

        System.out.println("Input: " + input);

        if(input.equals("2") || input.equals("3") || input.equals("4") || input.equals("5")){
            System.out.println("Input list id: ");
            input = reader.readLine();
            inputObj.setListId(input);
        }

        return inputObj;
    }

    private InputObj getInsideListInput() throws IOException{
        System.out.println("Select an option: ");
        System.out.println("1. Add or update item");
        System.out.println("2. Remove item");
        System.out.println("9. Exit");

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String input = reader.readLine();
        InputObj inputObj = new InputObj(input);

        switch (input){
            case "1":
                System.out.println("Input item name: ");
                input = reader.readLine();
                inputObj.setItemName(input);

                System.out.println("Input item quantity: ");
                input = reader.readLine();
                inputObj.setQuantity(Integer.parseInt(input));
                break;
            case "2":
                System.out.println("Input item name: ");
                input = reader.readLine();
                inputObj.setItemName(input);
                inputObj.setQuantity(0);
                break;
            case "9":
                insideList = false;
                break;
        }

        return inputObj;
    }

    private void communicate(InputObj input, SocketChannel serverChannel) throws IOException, ClassNotFoundException {
        System.out.println("INSIDE LIST: " + insideList);

        if(!insideList){
            switch (input.getOption()){
                case "1":
                    System.out.println("PIÇA NO CU");
                    Message request1 = new Message(Message.Type.CREATE_LIST, "");
                    request1.sendMessage(serverChannel);

                    Message response1 = Message.readMessage(serverChannel);
                    System.out.println("PIÇA NO CU 1");
                    if(response1.getType() == Message.Type.LIST_CREATED){
                        System.out.println("PIÇA NO CU 2");
                        var list = response1.getContent();
                        System.out.println(list.toString());
                    }
                    else{
                        System.out.println("Error creating list");
                    }

                    System.out.println("PIÇA NO CU 3");

/*                    var list = response1.getContent();
                    if(list.getClass() == ArrayList.class){
                        System.out.println("List created successfully");
                        Database.writeToFile(list, filepathPrefix + "1.ser" );  *//* TODO - Replace with List id: response1.getContent()... *//*
                        insideList = true;
                    }
                    else{
                        System.out.println("Error creating list");
                    }*/


                    break;
                case "2":
                    Message message2 = new Message(Message.Type.GET_LIST, "");
                    message2.sendMessage(SocketChannel.open(serverManagerSocketAddress));

                    // TODO - Receive response
                case "9":
                    System.exit(0);
                    break;
                default:
                    System.out.println("Error");
                    break;
            }
        }
        else{

        }
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

    public class InputObj{
        private String option;
        private String listId;
        private String itemName;
        private Integer quantity;

        InputObj(String option){
            this.option = option;
        }

        public String getOption(){
            return this.option;
        }
        public void setOption(String option) {
            this.option = option;
        }

        public void setListId(String listId) {
            this.listId = listId;
        }

        public String getListId(){
            return this.listId;
        }

        public String getItemName() {
            return itemName;
        }

        public void setItemName(String itemName) {
            this.itemName = itemName;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }


    }
}