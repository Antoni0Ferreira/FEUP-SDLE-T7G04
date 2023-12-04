package sdle.serverClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ServerRing {

    private Map<Integer, String> serverTable;
    public static void main(String[] args) {
        int numberOfServers = 5; // Number of servers to simulate

        // Starting IP address in the loopback range (127.0.0.0/8)
        int ipStart = 1; // Start with 127.0.0.1

        // Use a common port for simplicity, but you can also use different ports
        int port = 8000;

        List<Server> servers = new ArrayList<>();

        for (int i = 0; i < numberOfServers; i++) {
            String ipAddress = "127.0.0." + (ipStart + i);
            Server server = new Server(ipAddress, port);
            server.start();
            servers.add(server);
            System.out.println("Created server at " + ipAddress + ":" + port);
        }

        // Add logic to manage the servers as needed
    }

    public void addServer(Integer serverId, String ipAddress) {
        Integer hashedServerId = serverId.hashCode();
        serverTable.put(hashedServerId, ipAddress);
    }

    public void removeServer(Integer serverId) {
        Integer hashedServerId = serverId.hashCode();
        serverTable.remove(hashedServerId);
    }
}
