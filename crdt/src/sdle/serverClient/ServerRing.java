package sdle.serverClient;

import java.util.*;

public class ServerRing {

    private final SortedMap<Long, Server> serverTable;
    private final List<Thread> serverThreads;

    public ServerRing(int numberOfServers) {
        serverTable = new TreeMap<Long, Server>();
        serverThreads = new ArrayList<>();

        // Starting IP address in the loopback range (127.0.0.0/8)
        int ipStart = 1; // Start with 127.0.0.1

        // Use a common port for simplicity, but you can also use different ports
        int port = 8000;

        for (int i = 0; i < numberOfServers; i++) {
            String ipAddress = "127.0.0." + (ipStart + i);
            Server server = new Server(ipAddress, port);

            addServer(server);

            Thread serverThread = new Thread(server);
            serverThread.start();
            serverThreads.add(serverThread);

            System.out.println("Created server at " + ipAddress + ":" + port);
        }
    }

    public void updateServersTable() {
        for (Map.Entry<Long, Server> entry : serverTable.entrySet()) {
            entry.getValue().setServerTable(serverTable);
        }
    }

    public void addServer(Server server) {
        long ipAddressHash = MurmurHash.hash_x86_32(server.getIpAddress().getBytes(), server.getIpAddress().getBytes().length, 0);
        serverTable.put(ipAddressHash, server);
        updateServersTable();
    }

    public void removeServer(Server server) {
        long ipAddressHash = MurmurHash.hash_x86_32(server.getIpAddress().getBytes(), server.getIpAddress().getBytes().length, 0);
        serverTable.remove(ipAddressHash);
        updateServersTable();
    }

    public static void main(String[] args) {
        int numberOfServers = 5; // Number of servers to simulate
        ServerRing serverRing = new ServerRing(numberOfServers);
    }


}
