/*
 * SockExchange - Server and Client for BungeeCord and Spigot communication
 * Copyright (C) 2017 tracebachi@gmail.com (GeeItsZee)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.craftyourmind.socket.bungee;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataInput;
import fr.craftyourmind.socket.utils.SockExchangeConstants.Channels;
import fr.craftyourmind.socket.api.SocketApi;
import fr.craftyourmind.socket.api.messages.ReceivedMessage;
import fr.craftyourmind.socket.utils.Registerable;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.function.Consumer;


public class MovePlayersChannelListener implements Consumer<ReceivedMessage>, Registerable {
    private final SocketBungee plugin;
    private final SocketApi api;

    public MovePlayersChannelListener(SocketBungee plugin, SocketApi api) {
        Preconditions.checkNotNull(plugin, "plugin");
        Preconditions.checkNotNull(api, "api");

        this.plugin = plugin;
        this.api = api;
    }

    @Override
    public void register() {
        api.getMessageNotifier().register(Channels.MOVE_PLAYERS, this);
    }

    @Override
    public void unregister() {
        api.getMessageNotifier().unregister(Channels.MOVE_PLAYERS, this);
    }

    @Override
    public void accept(ReceivedMessage message) {
        ByteArrayDataInput in = message.getDataInput();
        String serverName = in.readUTF();
        ProxyServer proxy = plugin.getProxy();

        int count = in.readInt();
        for (int i = 0; i < count; i++) {
            String playerName = in.readUTF();

            ProxiedPlayer player = proxy.getPlayer(playerName);
            if (player == null) {
                continue;
            }

            ServerInfo serverInfo = proxy.getServerInfo(serverName);
            if (serverInfo == null) {
                continue;
            }

            player.connect(serverInfo);
        }
    }
}
