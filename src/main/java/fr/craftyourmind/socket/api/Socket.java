package fr.craftyourmind.socket.api;


import fr.craftyourmind.socket.api.listener.ResponseListener;
import fr.craftyourmind.socket.api.listener.SocketListener;
import fr.craftyourmind.socket.utils.BasicLogger;
import fr.craftyourmind.socket.utils.Side;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Socket {


    private final Side side;
    private final SocketApi api;

    private static BasicLogger logger;

    private Map<String, Tunnel> tunnelMap = new HashMap<>();
    private SocketListener listener;
    private ResponseListener responseListener;

    long timeout = TimeUnit.SECONDS.toMillis(5);

    public Socket(Side side, BasicLogger logger) {
        this.side = side;
        this.api = SocketApi.instance();

        listener = new SocketListener(this);
        api.getMessageNotifier().register("CYM", listener);
        responseListener = new ResponseListener(this);

        this.logger = logger;
        logger.info("Socket initialized");
    }

    public void addTunnel(Tunnel tunnel) {
        tunnel.setSocket(this);
        tunnelMap.put(tunnel.getChannel(), tunnel);
        api.getMessageNotifier().register(tunnel.getChannel(), listener);
    }

    public SocketApi getAPI() {
        return api;
    }

    protected void sendToServer(String channel, Message message, String serverName) {
        message.setServer(api.getServerName());
        message.setChannel(channel);
        api.sendToServer(channel, message.toBytes(), serverName, responseListener, timeout);
    }

    protected void sendToServers(String channel, Message message) {
        message.setServer(api.getServerName());
        message.setChannel(channel);
        api.sendToServers(channel, message.toBytes());
    }

    protected void sendToServers(String channel, Message message, List<String> servers) {
        message.setServer(api.getServerName());
        message.setChannel(channel);
        api.sendToServers(channel, message.toBytes(), servers);
    }

    protected void sendToBungee(String channel, Message message) {
        message.setServer(api.getServerName());
        message.setChannel(channel);
        api.sendToBungee(channel, message.toBytes(), responseListener, timeout);
    }

    public void handshake(String serverName) {
        sendToBungee("CYM", new Message("handshake", serverName));
    }


    public Tunnel getTunnel(String channel) {
        return tunnelMap.get(channel);
    }

    public static BasicLogger getLogger() {
        return logger;
    }
}
