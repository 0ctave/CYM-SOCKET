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
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import fr.craftyourmind.socket.utils.SockExchangeConstants.Channels;
import fr.craftyourmind.socket.api.SocketApi;
import fr.craftyourmind.socket.api.SpigotServerInfo;
import fr.craftyourmind.socket.utils.Registerable;

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class BungeeKeepAliveSender implements Registerable {
    private final SocketBungee plugin;
    private final SocketApi api;
    private final long updatePeriodMillis;
    private ScheduledFuture<?> updateFuture;

    public BungeeKeepAliveSender(SocketBungee plugin, SocketApi api, long updatePeriodMillis) {
        Preconditions.checkNotNull(plugin, "plugin");
        Preconditions.checkNotNull(api, "api");
        Preconditions.checkArgument(updatePeriodMillis > 0, "updatePeriodMillis");

        this.plugin = plugin;
        this.api = api;
        this.updatePeriodMillis = updatePeriodMillis;
    }

    @Override
    public void register() {
        updateFuture = api.getScheduledExecutorService().scheduleAtFixedRate(this::sendKeepAlive, updatePeriodMillis, updatePeriodMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void unregister() {
        if (updateFuture != null) {
            updateFuture.cancel(false);
            updateFuture = null;
        }
    }

    private void sendKeepAlive() {
        List<SpigotServerInfo> serverInfos = plugin.getServerInfos();
        ByteArrayDataOutput out = ByteStreams.newDataOutput(256);

        out.writeInt(serverInfos.size());

        for (SpigotServerInfo serverInfo : serverInfos) {
            out.writeUTF(serverInfo.getServerName());
            out.writeBoolean(serverInfo.isOnline());
            out.writeBoolean(serverInfo.isPrivate());
        }

        api.sendToServers(Channels.KEEP_ALIVE, out.toByteArray());
    }
}
