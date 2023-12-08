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
import java.util.Scanner;

public class Client {
    private String filepathPrefix = "client1-db/";
    private String serverManagerIp;
    private int serverPort;
    private InetSocketAddress serverManagerSocketAddress;
    private Selector selector;

    private Scanner scanner;

    private boolean insideList = false;
    private ArrayList<Long> currentList = new ArrayList<>();
    private Long currentListId;

    public Client(String serverManagerIp, int serverPort) {
        this.serverManagerIp = serverManagerIp;
        this.serverPort = serverPort;
        this.serverManagerSocketAddress = new InetSocketAddress(serverManagerIp, serverPort);
        this.scanner = new Scanner(System.in);
    }

    public void startClient() throws IOException, InterruptedException, ClassNotFoundException {

        this.selector = Selector.open();

        SocketChannel clientChannel = SocketChannel.open(serverManagerSocketAddress);
        clientChannel.configureBlocking(false);

        boolean exit = false;

        while(!exit){
            System.out.println();
            InputObj input = getClientInput();
            communicate(input, clientChannel);
        }

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
        System.out.println("9. Exit");

        String input = scanner.nextLine();

        InputObj inputObj = new InputObj(input);

        if(input.equals("2") || input.equals("3") || input.equals("4") || input.equals("5")){
            System.out.println("Input list id: ");
            input = scanner.nextLine();
            inputObj.setListId(input);
        }

        return inputObj;
    }

    private InputObj getInsideListInput() throws IOException{
        System.out.println("Current list: " + currentList.toString());
        System.out.println("Select an option: ");
        System.out.println("1. Add or update item");
        System.out.println("2. Remove item");
        System.out.println("9. Exit");

        String input = scanner.nextLine();
        InputObj inputObj = new InputObj(input);

        switch (input){
            case "1":
/*                System.out.println("Input item name: ");
                input = reader.readLine();
                inputObj.setItemName(input);

                System.out.println("Input item quantity: ");
                input = reader.readLine();
                inputObj.setQuantity(Integer.parseInt(input));*/
                System.out.println("\nInput new number: ");
                input = scanner.nextLine();
                currentList.add(Long.parseLong(input));

                // delete database file regarding the current list and create a new one with the updated list
                Database.deleteFile(filepathPrefix + currentListId.toString() + ".ser");
                Database.writeToFile(currentList, filepathPrefix + currentListId.toString() + ".ser");

                break;
            case "2":
/*                System.out.println("Input item name: ");
                input = reader.readLine();
                inputObj.setItemName(input);
                inputObj.setQuantity(0);*/
                System.out.println("\nInput number to remove: ");
                input = scanner.nextLine();
                currentList.remove(Long.parseLong(input));

                // delete database file regarding the current list and create a new one with the updated list
                Database.deleteFile(filepathPrefix + currentListId.toString() + ".ser");
                Database.writeToFile(currentList, filepathPrefix + currentListId.toString() + ".ser");

                break;
            case "9":
                insideList = false;
                inputObj.setOption("");

                break;
        }

        return inputObj;
    }

    private void communicate(InputObj input, SocketChannel serverChannel) throws IOException, ClassNotFoundException {

        if(!insideList){
            switch (input.getOption()){
                case "1":
                    Message request1 = new Message(Message.Type.CREATE_LIST, "");
                    request1.sendMessage(serverChannel);

                    Message response1 = Message.readMessage(serverChannel);
                    if(response1.getType() == Message.Type.LIST_CREATED){
                        var list = response1.getContent();
                        System.out.println(list.toString());
                    }
                    else{
                        System.out.println("\nError creating list");
                    }

                    // check if list is an ArrayList
                    var listObj = response1.getContent();
                    if(listObj.getClass() == ArrayList.class){
                        ArrayList<Object> list = (ArrayList<Object>) listObj;
                        ArrayList<Long> createdList = (ArrayList<Long>) list.get(0);
                        Long listId = (Long) list.get(1);

                        System.out.println("\nList created successfully");

                        Database.writeToFile(createdList, filepathPrefix +
                                listId.toString() + ".ser" );
                        currentList = createdList;
                        currentListId = listId;
                    }
                    else{
                        System.out.println("\nError creating list");
                    }
                    break;

                case "2":

                    String listId = input.getListId();

                    // get list from database
                    try {
                        currentList = (ArrayList<Long>) Database.readFromFile(filepathPrefix + listId + ".ser");
                        currentListId = Long.parseLong(listId);

                    } catch (IOException | ClassNotFoundException e) {
                        System.out.println("\nList not found");
                        break;
                    }

                    insideList = true;

                    break;

                case "3":

                    String listId3 = input.getListId();
                    Message message3 = new Message(Message.Type.DELETE_LIST, listId3);
                    message3.sendMessage(serverChannel);

                    Message response3 = Message.readMessage(serverChannel);
                    if(response3.getType() == Message.Type.LIST_DELETED){

                        // delete database file regarding the current list and create a new one with the updated list
                        Database.deleteFile(filepathPrefix + listId3 + ".ser");
                        System.out.println("\nList deleted successfully");
                    } else if(response3.getType() == Message.Type.LIST_NOT_FOUND) {
                        System.out.println("\nList not found");
                    } else {
                        System.out.println("\nError deleting list");
                    }

                    break;

                case "4":

                    String listId4 = input.getListId();
                    // get list from database
                    try {
                        currentList = (ArrayList<Long>) Database.readFromFile(filepathPrefix + listId4 + ".ser");

                    } catch (IOException | ClassNotFoundException e) {
                        System.out.println("\nList not found");
                        break;
                    }

                    ArrayList<Object> content = new ArrayList<>();
                    content.add(currentList);
                    content.add(listId4);
                    Message message4 = new Message(Message.Type.PUSH_LIST, content);
                    message4.sendMessage(serverChannel);

                    Message response4 = Message.readMessage(serverChannel);
                    if(response4.getType() == Message.Type.LIST_PUSHED){
                        System.out.println("\nList pushed successfully");
                    }
                    else{
                        System.out.println("\nError pushing list");
                    }
                    break;
                case "5":

                        String listId5 = input.getListId();
                        Message message5 = new Message(Message.Type.PULL_LIST, listId5);
                        message5.sendMessage(serverChannel);

                        Message response5 = Message.readMessage(serverChannel);
                        if(response5.getType() == Message.Type.LIST_PULLED){
                            currentList = (ArrayList<Long>) response5.getContent();
                            currentListId = Long.parseLong(listId5);
                            insideList = true;

                        } else if (response5.getType() == Message.Type.LIST_NOT_FOUND) {
                            System.out.println("\nList not found");
                        } else {
                            System.out.println("\nError pulling list");
                        }

                        break;

                case "9":
                    // Close connection to the server
                    System.out.println("\nClosing connection to the server");
                    if (serverChannel != null && serverChannel.isOpen()) {
                        try {
                            serverChannel.close();
                        } catch (IOException e) {
                            System.err.println("\nError closing the server channel: " + e.getMessage());
                        }
                    }

                    System.out.println("\nExiting the application");
                    System.exit(0);
                    break;
                default:
                    break;
            }
        }
        else{

        }
    }

    private void clearInputBuffer(BufferedReader reader) throws IOException {
        while (reader.ready() && reader.readLine() != null) {
            // Loop until there's no more input
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