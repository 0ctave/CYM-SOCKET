package fr.craftyourmind.socket.api;

import fr.craftyourmind.socket.api.listener.DummyListener;
import fr.craftyourmind.socket.api.listener.Listener;

import java.util.*;

public class Tunnel {

    private final String channel;
    public Map<UUID, Message> requests = new HashMap<>();
    private Listener listener;
    private Socket socket;

    public Tunnel(String channel) {
        this.channel = channel;
        this.listener = new DummyListener();
    }

    public String getChannel() {
        return channel;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public void sendToServer(Message request, String serverName) {
        requests.put(request.getUUID(), request);
        socket.sendToServer(channel, request, serverName);
    }

    public void sendToServers(Message request) {
        requests.put(request.getUUID(), request);
        socket.sendToServers(channel, request);
    }

    public void sendToServers(Message request, List<String> serverName) {
        requests.put(request.getUUID(), request);
        socket.sendToServers(channel, request, serverName);
    }

    public void sendToBungee(Message request) {
        requests.put(request.getUUID(), request);
        socket.sendToBungee(channel, request);
    }

    public void sendPlayersToServer(Set<String> playerNames, String serverName) {
        socket.sendPlayersToServer(playerNames, serverName);
    }

    public void sendChatMessagesToPlayers(List<String> messages, Set<String> playerNames, String serverName) {
        socket.sendChatMessagesToPlayers(messages, playerNames, serverName);
    }

    public Listener getListener() {
        return listener;
    }

    public Tunnel setListener(Listener listener) {
        this.listener = listener;
        return this;
    }

    public Map<UUID, Message> getRequests() {
        return requests;
    }
}
