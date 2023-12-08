package client;

import utils.Message;
import utils.MurmurHash;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Client {
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
        System.out.println("3. Exit");

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String input = reader.readLine();

        InputObj inputObj = new InputObj(input);

        if(input.equals("2")){
            // ask for list id
            System.out.println("Input list id: ");
            input = reader.readLine();
            inputObj.setListId(input);
        }

        return inputObj;
    }

    private InputObj getInsideListInput() throws IOException{
        System.out.println("Select an option: ");
        System.out.println("1. Add/update item");
        System.out.println("2. Remove item");
        System.out.println("3. Delete list");
        System.out.println("4. Push list");
        System.out.println("5. Pull list");
        System.out.println("6. Exit");

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
                inputObj.setItemName("");

        }


        return inputObj;
    }

    private void communicate(InputObj input) throws IOException {
        if(!insideList){
            switch (input.getOption()){
                case "1":
                    Message message1 = new Message(Message.Type.CREATE_LIST, "");
                    message1.sendMessage(SocketChannel.open(serverManagerSocketAddress));

                    //TODO - Receive response
                    break;
                case "2":


                    // TODO - Receive response
                case "3":
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