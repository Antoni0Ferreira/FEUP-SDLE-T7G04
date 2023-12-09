package client.client1;

import shopping.ShoppingList;
import utils.Message;
import utils.Database;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.sql.SQLOutput;
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
    private ShoppingList currentList;
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

        if(input.equals("2") || input.equals("3") || input.equals("4") || input.equals("5") || input.equals("6")){
            System.out.println("Input list id: ");
            input = scanner.nextLine();
            inputObj.setListId(input);
        }

        return inputObj;
    }

    private InputObj getInsideListInput() throws IOException{
        System.out.println("Current list: ");
        currentList.displayShoppingList();
        System.out.println("Select an option: ");
        System.out.println("1. Add or update item");
        System.out.println("2. Remove item");
        System.out.println("9. Exit");

        String input = scanner.nextLine();
        InputObj inputObj = new InputObj(input);

        switch (input){
            case "1":
                System.out.println("Input item name: ");
                input = scanner.nextLine();
                inputObj.setItemName(input);

                System.out.println("Input item quantity: ");
                input = scanner.nextLine();
                inputObj.setQuantity(Integer.parseInt(input));

                currentList.addItem(inputObj.getItemName(), inputObj.getQuantity());

                // delete database file regarding the current list and create a new one with the updated list
                Database.deleteFile(filepathPrefix + currentListId.toString() + ".ser");
                Database.writeToFile(currentList, filepathPrefix + currentListId.toString() + ".ser");

                break;
            case "2":
                System.out.println("Input item name: ");
                input = scanner.nextLine();
                inputObj.setItemName(input);

                System.out.println("Input item quantity: ");
                input = scanner.nextLine();
                inputObj.setQuantity(Integer.parseInt(input));

                currentList.removeItem(inputObj.getItemName(), inputObj.getQuantity());


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
                    request1.setSender(Message.Sender.CLIENT);
                    request1.sendMessage(serverChannel);

                    Message response1 = Message.readMessage(serverChannel);
                    if(response1.getType() == Message.Type.LIST_CREATED){
                        System.out.println("\nList created successfully");
                    }
                    else{
                        System.out.println("\nError creating list");
                        break;
                    }

                    // check if list is an ArrayList
                    var listObj = response1.getContent();
                    if(listObj.getClass() == ArrayList.class){
                        ArrayList<Object> list = (ArrayList<Object>) listObj;
                        ShoppingList createdList = (ShoppingList) list.get(0);
                        Long listId = (Long) list.get(1);

                        Database.writeToFile(createdList, filepathPrefix +
                                listId.toString() + ".ser" );
                        currentList = createdList;
                        currentListId = listId;
                        insideList = true;
                    }
                    else{
                        System.out.println("\nError creating list");
                    }
                    break;

                case "2":

                    String listId = input.getListId();

                    File file = new File(filepathPrefix + listId + ".ser");
                    if(!file.exists()){
                        System.out.println("\nList not found locally");
                        break;
                    }

                    currentList = (ShoppingList) Database.readFromFile(filepathPrefix + listId + ".ser");
                    currentListId = Long.parseLong(listId);

                    insideList = true;
                    break;

                case "3":

                    String listId3 = input.getListId();

                    File file2 = new File(filepathPrefix + listId3 + ".ser");
                    if(!file2.exists()){
                        System.out.println("\nList not found locally");
                        break;
                    }

                    // delete database file regarding the current list and create a new one with the updated list
                    Database.deleteFile(filepathPrefix + listId3 + ".ser");
                    System.out.println("\nList deleted locally");


                    Message message3 = new Message(Message.Type.DELETE_LIST, listId3);
                    message3.setSender(Message.Sender.CLIENT);
                    message3.sendMessage(serverChannel);

                    Message response3 = Message.readMessage(serverChannel);
                    if(response3.getType() == Message.Type.LIST_DELETED){
                        System.out.println("\nList deleted remotely");

                    } else if(response3.getType() == Message.Type.LIST_NOT_FOUND) {
                        System.out.println("\nList not found remotely");
                    } else {
                        System.out.println("\nError deleting list");
                    }

                    break;

                case "4":

                    String listId4 = input.getListId();

                    // check if list exists locally
                    File file3 = new File(filepathPrefix + listId4 + ".ser");
                    if(!file3.exists()){
                        System.out.println("\nList not found locally");
                        break;
                    }

                    currentList = (ShoppingList) Database.readFromFile(filepathPrefix + listId4 + ".ser");
                    Message message4 = new Message(Message.Type.PUSH_LIST, currentList);
                    message4.setSender(Message.Sender.CLIENT);
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
                    message5.setSender(Message.Sender.CLIENT);
                    message5.sendMessage(serverChannel);

                    Message response5 = Message.readMessage(serverChannel);
                    if(response5.getType() == Message.Type.LIST_PULLED){
                        currentList = (ShoppingList) response5.getContent();
                        currentListId = currentList.getId();
                        insideList = true;

                        // check if list exists locally
                        File file4 = new File(filepathPrefix + listId5 + ".ser");
                        if(!file4.exists()){
                            Database.writeToFile(currentList, filepathPrefix + listId5 + ".ser");
                            System.out.println("\nList pulled successfully");
                        }
                        else{
                            ShoppingList localList = (ShoppingList) Database.readFromFile(filepathPrefix + listId5 + ".ser");
                            localList.mergeShoppingList(currentList.getShoppingList());
                            Database.deleteFile(filepathPrefix + listId5 + ".ser");
                            Database.writeToFile(localList, filepathPrefix + listId5 + ".ser");
                            System.out.println("\nList pulled successfully");
                        }

                    } else if (response5.getType() == Message.Type.LIST_NOT_FOUND) {
                        System.out.println("\nList not found");
                    } else {
                        System.out.println("\nError pulling list");
                    }

                    break;

/*
                case "6":

                    String listId6 = input.getListId();

                    // check if list exists locally
                    File file4 = new File(filepathPrefix + listId6 + ".ser");
                    if(!file4.exists()){
                        System.out.println("\nList not found locally");
                        break;
                    }

                    currentList = (ShoppingList) Database.readFromFile(filepathPrefix + listId6 + ".ser");

                    Message message6 = new Message(Message.Type.SYNC, currentList);
                    message6.sendMessage(serverChannel);

                    Message response6 = Message.readMessage(serverChannel);
                    if(response6.getType() == Message.Type.SYNC_OK){
                        currentList = (ShoppingList) response6.getContent();
                        currentListId = currentList.getId();
                        insideList = true;

                        // check if list exists locally
                        File file5 = new File(filepathPrefix + listId6 + ".ser");
                        if(!file4.exists()){
                            Database.writeToFile(currentList, filepathPrefix + listId6 + ".ser");
                            System.out.println("\nList pulled successfully");
                        }
                        else{
                            ShoppingList localList = (ShoppingList) Database.readFromFile(filepathPrefix + listId6 + ".ser");
                            localList.mergeShoppingList(currentList.getShoppingList());
                            Database.deleteFile(filepathPrefix + listId6 + ".ser");
                            Database.writeToFile(localList, filepathPrefix + listId6 + ".ser");
                            System.out.println("\nList pulled successfully");
                        }


                    }
                    else{
                        System.out.println("\nError syncing list");
                    }

                    break;
*/

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

/*        ShoppingList shoppingList = new ShoppingList();
        shoppingList.addItem("Apple", 6);

        shoppingList.displayShoppingList();

        ShoppingList shoppingList2 = new ShoppingList();
        shoppingList2.addItem("Apple", 1);

        shoppingList.mergeShoppingList(shoppingList2.getShoppingList());
        shoppingList.displayShoppingList();
        shoppingList2.displayShoppingList();*/

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