package fr.craftyourmind.socket.spigot;

import fr.craftyourmind.socket.utils.MessageFormatMap;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

public class SpigotConfiguration {
    private String hostName;
    private int port;
    private String serverName;
    private String registrationPassword;
    private MessageFormatMap messageFormatMap;
    private boolean debugMode;

    void read(ConfigurationSection configuration) {
        hostName = configuration.getString("SockExchangeClient.HostName");
        port = configuration.getInt("SockExchangeClient.Port", 20000);
        serverName = configuration.getString("SockExchangeClient.ServerName", "");
        registrationPassword = configuration.getString("SockExchangeClient.Password", "FreshSocks");
        debugMode = configuration.getBoolean("DebugMode", false);
        messageFormatMap = new MessageFormatMap();

        ConfigurationSection formats = configuration.getConfigurationSection("Formats");
        if (formats != null) {
            for (String formatKey : formats.getKeys(false)) {
                String formatValue = formats.getString(formatKey, "");
                messageFormatMap.put(formatKey, ChatColor.translateAlternateColorCodes('&', formatValue));
            }
        }
    }

    String getHostName() {
        return hostName;
    }

    int getPort() {
        return port;
    }

    String getServerName() {
        return serverName;
    }

    String getRegistrationPassword() {
        return registrationPassword;
    }

    MessageFormatMap getMessageFormatMap() {
        return messageFormatMap;
    }

    boolean inDebugMode() {
        return debugMode;
    }
}
