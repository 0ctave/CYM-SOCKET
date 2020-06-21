//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package fr.craftyourmind.socket.bungee;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import fr.craftyourmind.socket.api.ExpirableConsumer;
import fr.craftyourmind.socket.api.Socket;
import fr.craftyourmind.socket.api.SocketApi;
import fr.craftyourmind.socket.api.SpigotServerInfo;
import fr.craftyourmind.socket.api.netty.SpigotToBungeeConnection;
import fr.craftyourmind.socket.utils.*;
import fr.craftyourmind.socket.api.messages.ReceivedMessageNotifier;
import fr.craftyourmind.socket.api.messages.ResponseMessage;
import fr.craftyourmind.socket.api.messages.ResponseStatus;
import fr.craftyourmind.socket.api.netty.BungeeToSpigotConnection;
import fr.craftyourmind.socket.api.netty.SocketServer;
import fr.craftyourmind.socket.api.scheduler.AwaitableExecutor;
import fr.craftyourmind.socket.api.scheduler.ScheduledExecutorServiceWrapper;
import fr.craftyourmind.socket.utils.Side;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class SocketBungee extends Plugin implements TieIn {

    private BungeeConfiguration config = new BungeeConfiguration();
    private BasicLogger LOGGER;

    private ScheduledThreadPoolExecutor threadPoolExecutor;
    private AwaitableExecutor awaitableExecutor;
    private LongIdCounterMap<ExpirableConsumer<ResponseMessage>> responseConsumerMap;
    private ScheduledFuture<?> consumerTimeoutCleanupFuture;
    private CaseInsensitiveMap<BungeeToSpigotConnection> spigotConnectionMap;
    private SocketServer socketServer;

    private OnlinePlayerUpdateSender onlinePlayerUpdateSender;
    private BungeeKeepAliveSender bungeeKeepAliveSender;
    private RunCmdBungeeCommand runCmdBungeeCommand;
    private MovePlayersChannelListener movePlayersChannelListener;
    private RunCmdChannelListener runCmdChannelListener;
    private ChatMessageChannelListener chatMessageChannelListener;

    private static Socket socket;

    @Override
    public void onEnable() {
        if (!loadConfig())
            return;

        LOGGER = new CYMLogger(getLogger(), config.inDebugMode());

        ScheduledExecutorServiceWrapper wrappedThreadPool = new ScheduledExecutorServiceWrapper(initThreadPoolExecutor());
        awaitableExecutor = new AwaitableExecutor(wrappedThreadPool);
        ReceivedMessageNotifier messageNotifier = new ReceivedMessageNotifier(awaitableExecutor);
        responseConsumerMap = new LongIdCounterMap<>();
        consumerTimeoutCleanupFuture = threadPoolExecutor.scheduleWithFixedDelay(
                this::checkForConsumerTimeouts, 5, 5, TimeUnit.SECONDS);

        spigotConnectionMap = new CaseInsensitiveMap<>(new ConcurrentHashMap<>());
        for (String serverName : getProxy().getServersCopy().keySet()) {
            BungeeToSpigotConnection connection = new BungeeToSpigotConnection(serverName, awaitableExecutor, messageNotifier, responseConsumerMap, LOGGER, this);
            spigotConnectionMap.put(serverName, connection);
        }

        SocketApi api = new SocketApi(this, wrappedThreadPool, messageNotifier, Side.SERVER);

        onlinePlayerUpdateSender = new OnlinePlayerUpdateSender(this, api, 500);
        onlinePlayerUpdateSender.register();

        bungeeKeepAliveSender = new BungeeKeepAliveSender(this, api, 200);
        bungeeKeepAliveSender.register();

        runCmdBungeeCommand = new RunCmdBungeeCommand(this, config.getMessageFormatMap(), api);
        runCmdBungeeCommand.register();

        movePlayersChannelListener = new MovePlayersChannelListener(this, api);
        movePlayersChannelListener.register();

        runCmdChannelListener = new RunCmdChannelListener(this, LOGGER, api);
        runCmdChannelListener.register();

        chatMessageChannelListener = new ChatMessageChannelListener(api);
        chatMessageChannelListener.register();

        try {
            socketServer = new SocketServer(config.getPort(), config.getConnectionThreads(), this);
            socketServer.start();
        } catch (Exception e) {
            getLogger().severe("============================================================");
            getLogger().severe("The SockExchange server could not be started. Refer to the stacktrace below.");
            e.printStackTrace();
            getLogger().severe("============================================================");
        }

        socket = new Socket(Side.SERVER, LOGGER);
    }

    @Override
    public void onDisable() {
        shutdownAwaitableExecutor();
        socketServer.shutdown();
        chatMessageChannelListener.unregister();
        runCmdChannelListener.unregister();
        movePlayersChannelListener.unregister();
        runCmdBungeeCommand.unregister();
        bungeeKeepAliveSender.unregister();
        onlinePlayerUpdateSender.unregister();
        spigotConnectionMap.clear();
        consumerTimeoutCleanupFuture.cancel(false);
        responseConsumerMap.clear();
        shutdownThreadPoolExecutor();
    }

    private boolean loadConfig() {
        ConfigurationProvider yamlProvider = ConfigurationProvider.getProvider(YamlConfiguration.class);
        File file = BungeeResourceUtil.saveResource(this, "bungee-config.yml", "config.yml");

        try {
            Configuration loadedConfig = yamlProvider.load(file);
            config.read(loadedConfig);
            return true;
        } catch (IOException ex) {
            getLogger().severe("============================================================");
            getLogger().severe("The CYM-SOCKET configuration file could not be loaded.");
            ex.printStackTrace();
            getLogger().severe("============================================================");
        }

        return false;
    }

    @Override
    public boolean doesRegistrationPasswordMatch(String password) {
        return config.doesRegistrationPasswordMatch(password);
    }

    @Override
    public SpigotToBungeeConnection getConnection() {
        return null;
    }

    @Override
    public BungeeToSpigotConnection getConnection(String serverName) {
        ExtraPreconditions.checkNotEmpty(serverName, "serverName");

        return spigotConnectionMap.get(serverName);
    }

    @Override
    public Collection<BungeeToSpigotConnection> getConnections() {
        return Collections.unmodifiableCollection(spigotConnectionMap.values());
    }

    @Override
    public SpigotServerInfo getServerInfo(String serverName) {
        Preconditions.checkNotNull(serverName, "serverName");

        BungeeToSpigotConnection connection = spigotConnectionMap.get(serverName);

        if (connection == null) {
            return null;
        }

        boolean isPrivate = config.isPrivateServer(connection.getServerName());
        return new SpigotServerInfo(connection.getServerName(), connection.hasChannel(), isPrivate);
    }

    @Override
    public List<SpigotServerInfo> getServerInfos() {
        Collection<BungeeToSpigotConnection> connections = spigotConnectionMap.values();
        List<SpigotServerInfo> result = new ArrayList<>(connections.size());

        for (BungeeToSpigotConnection connection : connections) {
            boolean isPrivate = config.isPrivateServer(connection.getServerName());
            SpigotServerInfo serverInfo = new SpigotServerInfo(connection.getServerName(),
                    connection.hasChannel(), isPrivate);

            result.add(serverInfo);
        }

        return Collections.unmodifiableList(result);
    }

    @Override
    public String getServerNameForPlayer(String playerName) {
        ProxiedPlayer player = getProxy().getPlayer(playerName);

        if (player == null) {
            return null;
        }

        Server server = player.getServer();

        if (server == null) {
            return null;
        }

        return server.getInfo().getName();
    }

    @Override
    public void sendChatMessagesToPlayer(String playerName, List<String> messages) {
        ProxiedPlayer proxyPlayer = getProxy().getPlayer(playerName);
        if (proxyPlayer != null) {
            for (String message : messages) {
                proxyPlayer.sendMessage(new TextComponent(message));
            }
        }
    }

    @Override
    public void sendChatMessagesToConsole(List<String> messages) {
        CommandSender console = getProxy().getConsole();
        for (String message : messages) {
            console.sendMessage(new TextComponent(message));
        }
    }

    @Override
    public Set<String> getOnlinePlayerNames() {
        return null;
    }

    @Override
    public Set<String> getOnlinePlayerNames(String serverName) {
        Set<String> players = new HashSet<>();
        for (ProxiedPlayer player : getProxy().getServersCopy().get(serverName).getPlayers())
            players.add(player.getName());
        return players;
    }

    @Override
    public void isPlayerOnServer(String serverName, String playerName, Consumer<Boolean> consumer) {
        for (ProxiedPlayer player : getProxy().getServersCopy().get(serverName).getPlayers())
            if (player.getName().equals(playerName))
                consumer.accept(true);
        consumer.accept(false);
    }

    @Override
    public void isPlayerOnServer(String playerName, Consumer<Boolean> consumer) {

    }

    private void checkForConsumerTimeouts() {
        long currentTimeMillis = System.currentTimeMillis();

        responseConsumerMap.removeIf((entry) ->
        {
            ExpirableConsumer<ResponseMessage> responseConsumer = entry.getValue();

            if (responseConsumer.getExpiresAtMillis() > currentTimeMillis) {
                // Keep the entry
                return false;
            }

            awaitableExecutor.execute(() ->
            {
                ResponseMessage responseMessage = new ResponseMessage(ResponseStatus.TIMED_OUT);
                responseConsumer.accept(responseMessage);
            });

            // Remove the entry
            return true;
        });
    }

    private ScheduledThreadPoolExecutor initThreadPoolExecutor() {
        ThreadFactoryBuilder factoryBuilder = new ThreadFactoryBuilder();
        factoryBuilder.setNameFormat("SockExchange-Scheduler-Thread-%d");

        ThreadFactory threadFactory = factoryBuilder.build();
        threadPoolExecutor = new ScheduledThreadPoolExecutor(2, threadFactory);

        threadPoolExecutor.setMaximumPoolSize(8);
        threadPoolExecutor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        threadPoolExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        return threadPoolExecutor;
    }

    private void shutdownAwaitableExecutor() {
        try {
            awaitableExecutor.setAcceptingTasks(false);
            awaitableExecutor.awaitTasksWithSleep(10, 1000);
            awaitableExecutor.shutdown();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    private void shutdownThreadPoolExecutor() {
        if (!threadPoolExecutor.isShutdown()) {
            threadPoolExecutor.shutdown();

            getLogger().info("ScheduledThreadPoolExecutor being shutdown()");

            try {
                if (!threadPoolExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                    threadPoolExecutor.shutdownNow();

                    getLogger().severe("ScheduledThreadPoolExecutor being shutdownNow()");
                    if (!threadPoolExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                        getLogger().severe("ScheduledThreadPoolExecutor not shutdown after shutdownNow()");
                    }
                }
            } catch (InterruptedException ex) {
                getLogger().severe("ScheduledThreadPoolExecutor shutdown interrupted");
                threadPoolExecutor.shutdownNow();
                getLogger().severe("ScheduledThreadPoolExecutor being shutdownNow()");
                Thread.currentThread().interrupt();
            }
        }
    }

    public static Socket getSocket() {
        return socket;
    }
}
