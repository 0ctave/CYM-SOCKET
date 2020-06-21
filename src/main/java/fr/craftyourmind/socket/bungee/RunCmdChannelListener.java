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
import fr.craftyourmind.socket.utils.BasicLogger;
import fr.craftyourmind.socket.utils.Registerable;
import net.md_5.bungee.api.ProxyServer;

import java.util.function.Consumer;


public class RunCmdChannelListener implements Consumer<ReceivedMessage>, Registerable {
    private static final String LOG_FORMAT = "[RunCmd] Command sender from %s ran command /%s";

    private final SocketBungee plugin;
    private final BasicLogger basicLogger;
    private final SocketApi api;

    public RunCmdChannelListener(SocketBungee plugin, BasicLogger basicLogger, SocketApi api) {
        Preconditions.checkNotNull(plugin, "plugin");
        Preconditions.checkNotNull(basicLogger, "basicLogger");
        Preconditions.checkNotNull(api, "api");

        this.plugin = plugin;
        this.basicLogger = basicLogger;
        this.api = api;
    }

    @Override
    public void register() {
        api.getMessageNotifier().register(Channels.RUN_CMD, this);
    }

    @Override
    public void unregister() {
        api.getMessageNotifier().unregister(Channels.RUN_CMD, this);
    }

    @Override
    public void accept(ReceivedMessage message) {
        ByteArrayDataInput in = message.getDataInput();
        String sourceServerName = in.readUTF();
        ProxyServer proxy = plugin.getProxy();

        int commandCount = in.readInt();
        for (int i = 0; i < commandCount; i++) {
            String command = in.readUTF();

            basicLogger.info(LOG_FORMAT, sourceServerName, command);

            proxy.getPluginManager().dispatchCommand(proxy.getConsole(), command);
        }
    }
}
