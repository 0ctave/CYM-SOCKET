package fr.craftyourmind.socket.api;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import fr.craftyourmind.socket.api.messages.ReceivedMessageNotifier;
import fr.craftyourmind.socket.api.messages.ResponseMessage;
import fr.craftyourmind.socket.api.netty.BungeeToSpigotConnection;
import fr.craftyourmind.socket.utils.ExtraPreconditions;
import fr.craftyourmind.socket.utils.SockExchangeConstants;
import fr.craftyourmind.socket.utils.TieIn;
import fr.craftyourmind.socket.utils.Side;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

public class SocketApi {
    private static SocketApi instance;
    private final TieIn tieIn;

    private final ScheduledExecutorService scheduledExecutorService;
    private final ReceivedMessageNotifier messageNotifier;
    private final Side side;

    public SocketApi(TieIn tieIn, ScheduledExecutorService scheduledExecutorService, ReceivedMessageNotifier messageNotifier, Side side) {

        instance = this;
        this.tieIn = tieIn;
        this.scheduledExecutorService = scheduledExecutorService;
        this.messageNotifier = messageNotifier;
        this.side = side;
    }

    public static SocketApi instance() {
        return instance;
    }

    private static byte[] getBytesForMovingPlayers(String serverName, Set<String> playerNames) {
        int approxSize = ((playerNames.size() * 20) + serverName.length() + 32) * 2;
        ByteArrayDataOutput out = ByteStreams.newDataOutput(approxSize);

        out.writeUTF(serverName);

        out.writeInt(playerNames.size());
        for (String playerName : playerNames) {
            out.writeUTF(playerName);
        }

        return out.toByteArray();
    }

    private static byte[] getBytesForSendingCommands(String serverName, List<String> commands) {
        int commandsCount = commands.size();
        int approxSize = (commandsCount * 60 + 32) * 2;
        ByteArrayDataOutput out = ByteStreams.newDataOutput(approxSize);

        out.writeUTF(serverName);

        out.writeInt(commandsCount);
        for (String command : commands) {
            out.writeUTF(command);
        }

        return out.toByteArray();
    }

    private static byte[] getBytesForSendingChatMessages(List<String> messages, String playerName, String serverName) {
        int messagesCount = messages.size();
        int approxSize = (messagesCount * 60) * 2;
        ByteArrayDataOutput out = ByteStreams.newDataOutput(approxSize);

        out.writeInt(messages.size());
        for (String message : messages) {
            out.writeUTF(message);
        }

        if (playerName != null) {
            out.writeBoolean(true); // PlayerName
            out.writeUTF(playerName);
        } else if (serverName != null) {
            out.writeBoolean(false); // ServerName
            out.writeUTF(serverName);
        } else {
            throw new IllegalArgumentException("Both playerName and serverName are null");
        }

        return out.toByteArray();
    }

    /**
     * Get the SpigotServerInfo for a server using a name
     *
     * @param serverName Name of the server to lookup
     * @return {@link SpigotServerInfo} or null if not found
     */
    public SpigotServerInfo getServerInfo(String serverName) {
        return tieIn.getServerInfo(serverName);
    }

    /**
     * @return List of {@link SpigotServerInfo} of all known servers
     */
    public Collection<SpigotServerInfo> getServerInfos() {
        return tieIn.getServerInfos();
    }

    /**
     * @return {@link ScheduledExecutorService} managed by SockExchange
     */
    public ScheduledExecutorService getScheduledExecutorService() {
        return scheduledExecutorService;
    }

    /**
     * @return {@link ReceivedMessageNotifier} used for registering and
     * un-registering listeners
     */
    public ReceivedMessageNotifier getMessageNotifier() {
        return messageNotifier;
    }

    /**
     * Sends bytes to Bungee (if connected)
     * <p>
     * To avoid extra memory usage, the API assumes the following parameters
     * are not modified after this method is called: messageBytes
     *
     * @param channelName  Name of channel to send bytes to
     * @param messageBytes Bytes to send
     */
    public void sendToBungee(String channelName, byte[] messageBytes) {
        sendToBungee(channelName, messageBytes, null, 0);
    }

    /**
     * Sends bytes to Bungee (if connected) and expects a response
     * <p>
     * To avoid extra memory usage, the API assumes the following parameters
     * are not modified after this method is called: messageBytes
     *
     * @param channelName     Name of channel to send bytes to
     * @param messageBytes    Bytes to send
     * @param consumer        Consumer to run once there is a response (or a failure)
     * @param timeoutInMillis Milliseconds to wait for a response before returning a timeout response
     */
    public void sendToBungee(String channelName, byte[] messageBytes, Consumer<ResponseMessage> consumer, long timeoutInMillis) {
        tieIn.getConnection().sendToBungee(channelName, messageBytes, consumer, timeoutInMillis);
    }

    /**
     * Sends bytes to one server (if online)
     * <p>
     * To avoid extra memory usage, the API assumes the following parameters
     * are not modified after this method is called: messageBytes
     *
     * @param channelName  Name of channel to send bytes to
     * @param messageBytes Bytes to send
     * @param serverName   Name of the server to send to
     */
    public void sendToServer(String channelName, byte[] messageBytes, String serverName) {
        sendToServer(channelName, messageBytes, serverName, null, 0);
    }

    /**
     * Sends bytes to one server (if online) and expects a response
     * <p>
     * To avoid extra memory usage, the API assumes the following parameters
     * are not modified after this method is called: messageBytes
     *
     * @param channelName     Name of channel to send bytes to
     * @param messageBytes    Bytes to send
     * @param serverName      Name of the server to send to
     * @param consumer        Consumer to run once there is a response (or a failure)
     * @param timeoutInMillis Milliseconds to wait for a response before returning a timeout response
     */
    public void sendToServer(String channelName, byte[] messageBytes, String serverName, Consumer<ResponseMessage> consumer, long timeoutInMillis) {
        switch (side) {
            case CLIENT:
                tieIn.getConnection().sendToServer(channelName, messageBytes, serverName, consumer, timeoutInMillis);
            case SERVER:
                BungeeToSpigotConnection connection = (BungeeToSpigotConnection) tieIn.getConnection(serverName);
                Preconditions.checkNotNull(connection, "Unknown serverName: %s", serverName);

                connection.sendToServer(channelName, messageBytes, consumer, timeoutInMillis);
        }

    }

    /**
     * Sends bytes to a server (if online)
     * <p>
     * To avoid extra memory usage, the API assumes the following parameters
     * are not modified after this method is called: messageBytes
     *
     * @param channelName  Name of channel to send bytes to
     * @param messageBytes Bytes to send
     * @param playerName   Name of the player to find and then their server to send bytes to
     */
    public void sendToServerOfPlayer(String channelName, byte[] messageBytes, String playerName) {
        sendToServerOfPlayer(channelName, messageBytes, playerName, null, 0);
    }

    /**
     * Sends bytes to a server (if online) and expects a response
     * <p>
     * To avoid extra memory usage, the API assumes the following parameters
     * are not modified after this method is called: messageBytes
     *
     * @param channelName     Name of channel to send bytes to
     * @param messageBytes    Bytes to send
     * @param playerName      Name of the player to find and then their server to send bytes to
     * @param consumer        Consumer to run once there is a response (or a failure)
     * @param timeoutInMillis Milliseconds to wait for a response before returning a timeout response
     */
    protected void sendToServerOfPlayer(String channelName, byte[] messageBytes, String playerName, Consumer<ResponseMessage> consumer, long timeoutInMillis) {
        tieIn.isPlayerOnServer(playerName, getServerName(), (online) ->
        {
            if (online) {
                tieIn.getConnection().sendToServer(channelName, messageBytes, getServerName(), consumer,
                        timeoutInMillis);
            } else {
                tieIn.getConnection().sendToServerOfPlayer(channelName, messageBytes, playerName, consumer,
                        timeoutInMillis);
            }
        });
    }

    /**
     * Sends bytes to all online servers
     * <p>
     * To avoid extra memory usage, the API assumes the following parameters
     * are not modified after this method is called: messageBytes
     *
     * @param channelName  Name of channel to send bytes to
     * @param messageBytes Bytes to send
     */
    public void sendToServers(String channelName, byte[] messageBytes) {
        sendToServers(channelName, messageBytes, Collections.emptyList());
    }

    /**
     * Sends bytes to a list of servers (if online)
     * <p>
     * To avoid extra memory usage, the API assumes the following parameters
     * are not modified after this method is called: messageBytes, serverNames
     *
     * @param channelName  Name of channel to send bytes to
     * @param messageBytes Bytes to send
     * @param serverNames  List of server names to send bytes to
     */
    protected void sendToServers(String channelName, byte[] messageBytes, List<String> serverNames) {
        switch (side) {
            case SERVER:
                ExtraPreconditions.checkNotEmpty(channelName, "channelName");
                Preconditions.checkNotNull(messageBytes, "messageBytes");

                if (serverNames == null || serverNames.isEmpty()) {
                    for (BungeeToSpigotConnection connection : tieIn.getConnections()) {
                        connection.sendToServer(channelName, messageBytes, null, 0);
                    }
                } else {
                    for (String serverName : serverNames) {
                        if (serverName == null || serverName.isEmpty()) {
                            continue;
                        }

                        BungeeToSpigotConnection connection = tieIn.getConnection(serverName);

                        if (connection == null) {
                            continue;
                        }

                        connection.sendToServer(channelName, messageBytes, null, 0);
                    }
                }
            case CLIENT:
                tieIn.getConnection().sendToServers(channelName, messageBytes, serverNames);
        }
    }

    /**
     * Sends players (if they are online) to the server (if the server is online)
     *
     * @param playerNames Set of player names to move
     * @param serverName  Name of the server the player should be moved to
     */
    public void movePlayers(Set<String> playerNames, String serverName) {
        ExtraPreconditions.checkNotEmpty(playerNames, "playerNames");
        ExtraPreconditions.checkElements(playerNames, (str) -> str != null && !str.isEmpty(),
                "Null or empty name in playerNames");
        ExtraPreconditions.checkNotEmpty(serverName, "serverName");

        byte[] messageBytes = getBytesForMovingPlayers(serverName, playerNames);
        sendToBungee(SockExchangeConstants.Channels.MOVE_PLAYERS, messageBytes);
    }

    /**
     * Sends a list of commands to Bungee
     * <p>
     * To avoid extra memory usage, the API assumes the following parameters
     * are not modified after this method is called: commands
     *
     * @param commands List of commands to run in order
     */
    public void sendCommandsToBungee(List<String> commands) {
        ExtraPreconditions.checkNotEmpty(commands, "commands");
        ExtraPreconditions.checkElements(commands, (str) -> str != null && !str.isEmpty(),
                "Null or empty string in commands");

        byte[] messageBytes = getBytesForSendingCommands(getServerName(), commands);
        sendToBungee(SockExchangeConstants.Channels.RUN_CMD, messageBytes);
    }

    /**
     * Sends a list of commands to multiple servers (if online)
     * <p>
     * To avoid extra memory usage, the API assumes the following parameters
     * are not modified after this method is called: commands
     *
     * @param commands    List of commands to run in order
     * @param serverNames Name of the server to send to
     */
    public void sendCommandsToServers(List<String> commands, List<String> serverNames) {
        ExtraPreconditions.checkNotEmpty(commands, "commands");
        ExtraPreconditions.checkElements(commands, (str) -> str != null && !str.isEmpty(),
                "Null or empty string in commands");

        byte[] messageBytes;
        if (side == Side.CLIENT)
            messageBytes = getBytesForSendingCommands(getServerName(), commands);
        else
            messageBytes = getBytesForSendingCommands(commands);
        sendToServers(SockExchangeConstants.Channels.RUN_CMD, messageBytes, serverNames);
    }

    /**
     * Sends a list of messages (in order) to a player or console
     * <p>
     * If the player name is not null, empty, or console, the messages will be sent to the player
     * by the given name. Otherwise, the server name will be used to send messages to that server's
     * console. "Bungee" is a valid server name for the Bungee console.
     * <p>
     * To avoid extra memory usage, the API assumes the following parameters
     * are not modified after this method is called: messages
     *
     * @param messages   List of messages to send
     * @param playerName Name of the player to send messages to
     * @param serverName Name of the server to send messages to
     */
    public void sendChatMessages(List<String> messages, String playerName, String serverName) {
        ExtraPreconditions.checkNotEmpty(messages, "messages");
        ExtraPreconditions.checkElements(messages, Objects::nonNull, "Null message in messages");

        // If a player name is provided and it's not the console, send it from Bungee
        if (playerName != null && !playerName.isEmpty() && !playerName.equalsIgnoreCase("console")) {
            tieIn.sendChatMessagesToPlayer(playerName, messages);
            return;
        }

        ExtraPreconditions.checkNotEmpty(serverName, "serverName");

        // Shortcut (since the message is for this server's console)
        if (serverName.equalsIgnoreCase("Bungee")) {
            tieIn.sendChatMessagesToConsole(messages);
            return;
        }

        byte[] messageBytes;
        if (side == Side.CLIENT)
            messageBytes = getBytesForSendingChatMessages(messages, null, serverName);
        else
            messageBytes = getBytesForSendingChatMessages(messages);

        sendToServer(SockExchangeConstants.Channels.CHAT_MESSAGES, messageBytes, serverName);
    }

    private byte[] getBytesForSendingCommands(List<String> commands) {
        int commandsCount = commands.size();
        int approxSize = (commandsCount * 60 + 32) * 2;
        ByteArrayDataOutput out = ByteStreams.newDataOutput(approxSize);

        out.writeUTF("Bungee");

        out.writeInt(commandsCount);
        for (String command : commands) {
            out.writeUTF(command);
        }

        return out.toByteArray();
    }

    private byte[] getBytesForSendingChatMessages(List<String> messages) {
        int messagesCount = messages.size();
        int approxSize = (messagesCount * 60) * 2;
        ByteArrayDataOutput out = ByteStreams.newDataOutput(approxSize);

        out.writeInt(messagesCount);
        for (String message : messages) {
            out.writeUTF(message);
        }

        return out.toByteArray();
    }

    /**
     * @return Name of the current server
     */
    public String getServerName() {
        if (side == Side.SERVER)
            return "bungee";
        return tieIn.getConnection().getServerName();
    }
}
