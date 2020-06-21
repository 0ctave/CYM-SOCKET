package fr.craftyourmind.socket.utils;

import fr.craftyourmind.socket.api.SpigotServerInfo;
import fr.craftyourmind.socket.api.netty.BungeeToSpigotConnection;
import fr.craftyourmind.socket.api.netty.SpigotToBungeeConnection;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public interface TieIn {
    boolean doesRegistrationPasswordMatch(String password);

    SpigotToBungeeConnection getConnection();

    BungeeToSpigotConnection getConnection(String spigotServerName);

    Collection<BungeeToSpigotConnection> getConnections();

    SpigotServerInfo getServerInfo(String serverName);

    Collection<SpigotServerInfo> getServerInfos();

    String getServerNameForPlayer(String playerName);

    void sendChatMessagesToPlayer(String playerName, List<String> messages);

    void sendChatMessagesToConsole(List<String> messages);

    Set<String> getOnlinePlayerNames();

    Set<String> getOnlinePlayerNames(String serverName);

    void isPlayerOnServer(String serverName, String playerName, Consumer<Boolean> consumer);

    void isPlayerOnServer(String playerName, Consumer<Boolean> consumer);
}
