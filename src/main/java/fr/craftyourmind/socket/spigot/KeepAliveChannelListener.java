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
package fr.craftyourmind.socket.spigot;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataInput;
import fr.craftyourmind.socket.api.SocketApi;
import fr.craftyourmind.socket.api.SpigotServerInfo;
import fr.craftyourmind.socket.api.messages.ReceivedMessage;
import fr.craftyourmind.socket.utils.CaseInsensitiveMap;
import fr.craftyourmind.socket.utils.Registerable;
import fr.craftyourmind.socket.utils.SockExchangeConstants.Channels;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class KeepAliveChannelListener implements Registerable {
    private final SocketApi api;
    private final Consumer<ReceivedMessage> onChannelMessage;
    private volatile Map<String, SpigotServerInfo> serverInfoMap = Collections.emptyMap();

    public KeepAliveChannelListener(SocketApi api) {
        this.api = api;
        this.onChannelMessage = this::onKeepAliveChannelMessage;
    }

    @Override
    public void register() {
        api.getMessageNotifier().register(Channels.KEEP_ALIVE, onChannelMessage);
    }

    @Override
    public void unregister() {
        api.getMessageNotifier().unregister(Channels.KEEP_ALIVE, onChannelMessage);
    }

    public SpigotServerInfo getServerInfo(String serverName) {
        return serverInfoMap.get(serverName);
    }

    public Collection<SpigotServerInfo> getServerInfos() {
        return serverInfoMap.values();
    }

    private void onKeepAliveChannelMessage(ReceivedMessage receivedMessage) {
        ByteArrayDataInput in = receivedMessage.getDataInput();
        int count = in.readInt();
        CaseInsensitiveMap<SpigotServerInfo> map = new CaseInsensitiveMap<>(new HashMap<>(count));

        for (int i = 0; i < count; i++) {
            String serverName = in.readUTF();
            boolean online = in.readBoolean();
            boolean hidden = in.readBoolean();
            SpigotServerInfo serverInfo = new SpigotServerInfo(serverName, online, hidden);

            map.put(serverName, serverInfo);
        }

        serverInfoMap = Collections.unmodifiableMap(map);
    }
}
