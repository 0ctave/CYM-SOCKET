package fr.craftyourmind.socket.bungee;

import fr.craftyourmind.socket.utils.CaseInsensitiveSet;
import fr.craftyourmind.socket.utils.MessageFormatMap;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.config.Configuration;

import java.util.HashSet;
import java.util.Objects;

public class BungeeConfiguration {
    private int port;
    private int connectionThreads;
    private String registrationPassword;
    private MessageFormatMap messageFormatMap;
    private boolean debugMode;
    private CaseInsensitiveSet privateServers = new CaseInsensitiveSet(new HashSet<>());

    void read(Configuration configuration) {
        port = configuration.getInt("SockExchangeServer.Port", 20000);
        connectionThreads = configuration.getInt("SockExchangeServer.Threads", 2);
        registrationPassword = configuration.getString("SockExchangeServer.Password", "FreshSocks");
        debugMode = configuration.getBoolean("DebugMode", false);
        messageFormatMap = new MessageFormatMap();

        privateServers.clear();
        privateServers.addAll(configuration.getStringList("PrivateServers"));

        Configuration formats = configuration.getSection("Formats");
        if (formats != null) {
            for (String formatKey : formats.getKeys()) {
                String formatValue = formats.getString(formatKey, "");
                messageFormatMap.put(formatKey, ChatColor.translateAlternateColorCodes('&', formatValue));
            }
        }
    }

    int getPort() {
        return port;
    }

    int getConnectionThreads() {
        return connectionThreads;
    }

    boolean doesRegistrationPasswordMatch(String input) {
        return Objects.equals(registrationPassword, input);
    }

    MessageFormatMap getMessageFormatMap() {
        return messageFormatMap;
    }

    boolean inDebugMode() {
        return debugMode;
    }

    boolean isPrivateServer(String serverName) {
        return privateServers.contains(serverName);
    }
}
