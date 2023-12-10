package client.client1;

import shopping.ShoppingList;
import utils.Message;
import utils.Database;
import utils.MurmurHash;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Objects;
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

        boolean exit = false;

        while(!exit){
            System.out.println();
            InputObj input = getClientInput();
            communicate(input);
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

    private SocketChannel connectToServer() {

        try {
            this.selector = Selector.open();
            SocketChannel serverChannel = SocketChannel.open(serverManagerSocketAddress);
            serverChannel.configureBlocking(false);
            return serverChannel;
        } catch (IOException e) {
            System.err.println("\nError connecting to the server: " + e.getMessage());
        }

        return null;
    }

    private void disconnectFromServer(SocketChannel serverChannel) throws IOException {
        if (serverChannel != null && serverChannel.isOpen()) {
            try {
                serverChannel.close();
            } catch (IOException e) {
                System.err.println("\nError closing the server channel: " + e.getMessage());
            }
        }
    }

    private void communicate(InputObj input) throws IOException, ClassNotFoundException {


        if(!insideList){
            switch (input.getOption()){
                case "1":

                    // create random list id
                    long longRandom = new Random().nextLong();
                    Long listId = MurmurHash.hash_x86_32(Long.toString(longRandom).getBytes(),
                            Long.toString(longRandom).getBytes().length, 0);

                    // create new shopping list
                    ShoppingList shoppingList = new ShoppingList();
                    shoppingList.setId(listId);

                    Database.writeToFile(shoppingList, filepathPrefix +
                            listId.toString() + ".ser" );
                    currentList = shoppingList;
                    currentListId = listId;
                    insideList = true;

                    System.out.println("\nList created successfully. List ID: " + listId);

                    break;

                case "2":

                    String listIdInput = input.getListId();

                    File file = new File(filepathPrefix + listIdInput + ".ser");
                    if(!file.exists()){
                        System.out.println("\nList not found locally");
                        break;
                    }

                    currentList = (ShoppingList) Database.readFromFile(filepathPrefix + listIdInput + ".ser");
                    currentListId = Long.parseLong(listIdInput);
                    insideList = true;
                    break;

                case "3":

                    String listId3 = input.getListId();

                    File file2 = new File(filepathPrefix + listId3 + ".ser");
                    if(!file2.exists()){
                        System.out.println("\nList not found locally");
                    } else {
                        // delete database file regarding the current list and create a new one with the updated list
                        Database.deleteFile(filepathPrefix + listId3 + ".ser");
                        System.out.println("\nList deleted locally");
                    }

                    // ask if the user wants to delete the list remotely
                    System.out.println("\nDo you want to delete the list remotely?");
                    System.out.println("1. Yes");
                    System.out.println("2. No");
                    String input2 = scanner.nextLine();

                    if(!input2.equals("1")){
                        break;
                    }

                    SocketChannel serverChannel1 = connectToServer();

                    if(serverChannel1 == null){
                        break;
                    }

                    Message message3 = new Message(Message.Type.DELETE_LIST, listId3);
                    message3.setSender(Message.Sender.CLIENT);
                    message3.sendMessage(serverChannel1);

                    Message response3 = Message.readMessage(serverChannel1);
                    if(response3.getType() == Message.Type.LIST_DELETED){
                        System.out.println("\nList deleted remotely");

                    } else if(response3.getType() == Message.Type.LIST_NOT_FOUND) {
                        System.out.println("\nList not found remotely");
                    } else {
                        System.out.println("\nError deleting list");
                    }

                    this.disconnectFromServer(serverChannel1);

                    break;

                case "4":

                    String listId4 = input.getListId();

                    // check if list exists locally
                    File file3 = new File(filepathPrefix + listId4 + ".ser");
                    if(!file3.exists()){
                        System.out.println("\nList not found locally");
                        break;
                    }

                    SocketChannel serverChannel2 = connectToServer();

                    if(serverChannel2 == null){
                        break;
                    }

                    currentList = (ShoppingList) Database.readFromFile(filepathPrefix + listId4 + ".ser");
                    currentListId = Long.parseLong(listId4);
                    Message message4 = new Message(Message.Type.PUSH_LIST, currentList);
                    message4.setSender(Message.Sender.CLIENT);
                    message4.sendMessage(serverChannel2);

                    Message response4 = Message.readMessage(serverChannel2);
                    if(response4.getType() == Message.Type.LIST_PUSHED){
                        Long responseListId = (Long) response4.getContent();

                        if(!Objects.equals(responseListId, currentListId)){
                            // delete database file regarding the current list and create a new one with the updated list
                            Database.deleteFile(filepathPrefix + currentListId.toString() + ".ser");
                            Database.writeToFile(currentList, filepathPrefix + responseListId.toString() + ".ser");
                            System.out.println("\nList pushed successfully. List ID updated");
                            currentList.setId(responseListId);
                            currentListId = currentList.getId();
                            break;
                        } else {
                            System.out.println("\nList pushed successfully");
                            currentList.setInCloud(true);

                        }
                    }
                    else if(response4.getType() == Message.Type.LIST_NOT_FOUND){
                        System.out.println("\nList not found remotely");
                    }
                    else if(response4.getType() == Message.Type.SERVER_NOT_FOUND){
                        System.out.println("\nThere are no servers available");
                    }
                    else{
                        System.out.println("\nError pushing list");

                    }
                    this.disconnectFromServer(serverChannel2);
                    break;
                case "5":

                    SocketChannel serverChannel3 = connectToServer();

                    if(serverChannel3 == null){
                        break;
                    }

                    String listId5 = input.getListId();
                    Message message5 = new Message(Message.Type.PULL_LIST, listId5);
                    message5.setSender(Message.Sender.CLIENT);
                    message5.sendMessage(serverChannel3);

                    Message response5 = Message.readMessage(serverChannel3);
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
                    } else if (response5.getType() == Message.Type.SERVER_NOT_FOUND) {
                        System.out.println("\nThere are no servers available");
                    }
                    else {
                        System.out.println("\nError pulling list");
                    }

                    this.disconnectFromServer(serverChannel3);
                    break;

                case "9":
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

    public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
        // Example usage
        Client client = new Client("127.0.0.1", 8000);
        client.startClient();

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