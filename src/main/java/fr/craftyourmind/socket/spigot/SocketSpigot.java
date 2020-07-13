//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package fr.craftyourmind.socket.spigot;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import fr.craftyourmind.socket.api.ExpirableConsumer;
import fr.craftyourmind.socket.api.Socket;
import fr.craftyourmind.socket.api.SocketApi;
import fr.craftyourmind.socket.api.SpigotServerInfo;
import fr.craftyourmind.socket.api.messages.ReceivedMessageNotifier;
import fr.craftyourmind.socket.api.messages.ResponseMessage;
import fr.craftyourmind.socket.api.messages.ResponseStatus;
import fr.craftyourmind.socket.api.netty.BungeeToSpigotConnection;
import fr.craftyourmind.socket.api.netty.SocketClient;
import fr.craftyourmind.socket.api.netty.SpigotToBungeeConnection;
import fr.craftyourmind.socket.api.scheduler.AwaitableExecutor;
import fr.craftyourmind.socket.api.scheduler.ScheduledExecutorServiceWrapper;
import fr.craftyourmind.socket.utils.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class SocketSpigot extends JavaPlugin implements TieIn {


    private final SpigotConfiguration config = new SpigotConfiguration();

    private ScheduledThreadPoolExecutor threadPoolExecutor;
    private AwaitableExecutor awaitableExecutor;
    private BasicLogger LOGGER;
    private ReceivedMessageNotifier messageNotifier;
    private LongIdCounterMap<ExpirableConsumer<ResponseMessage>> responseConsumerMap;
    private SpigotToBungeeConnection connection;
    private SocketClient socketClient;
    private ScheduledFuture<?> consumerTimeoutCleanupFuture;

    private PlayerUpdateChannelListener playerUpdateChannelListener;
    private KeepAliveChannelListener keepAliveChannelListener;
    private MoveOtherToCommand moveOtherToCommand;
    private MoveToCommand moveToCommand;
    private RunCmdCommand runCmdCommand;
    private ChatMessageChannelListener chatMessageChannelListener;
    private RunCmdChannelListener runCmdChannelListener;
    private SpigotKeepAliveSender spigotKeepAliveSender;

    private static Socket socket;

    @Override
    public void onLoad() {
        saveDefaultConfig();
    }

    @Override
    public void onEnable() {
        reloadConfig();
        config.read(getConfig());

        LOGGER = new CYMLogger(getLogger(), config.inDebugMode());

        ScheduledExecutorServiceWrapper wrappedThreadPool = new ScheduledExecutorServiceWrapper(buildThreadPoolExecutor());
        awaitableExecutor = new AwaitableExecutor(wrappedThreadPool);
        messageNotifier = new ReceivedMessageNotifier(awaitableExecutor);
        responseConsumerMap = new LongIdCounterMap<>();

        consumerTimeoutCleanupFuture = threadPoolExecutor.scheduleWithFixedDelay(this::checkForConsumerTimeouts, 5, 5, TimeUnit.SECONDS);

        connection = new SpigotToBungeeConnection(config.getServerName(), config.getRegistrationPassword(), awaitableExecutor, messageNotifier, responseConsumerMap, LOGGER);

        SocketApi api = new SocketApi(this, threadPoolExecutor, messageNotifier, Side.CLIENT);

        socket = new Socket(Side.CLIENT, LOGGER);
        getLogger().info("The Client Socket is setup");

        playerUpdateChannelListener = new PlayerUpdateChannelListener(api);
        playerUpdateChannelListener.register();

        keepAliveChannelListener = new KeepAliveChannelListener(api);
        keepAliveChannelListener.register();

        moveOtherToCommand = new MoveOtherToCommand(this, config.getMessageFormatMap(), api);
        moveOtherToCommand.register();

        moveToCommand = new MoveToCommand(this, config.getMessageFormatMap(), api);
        moveToCommand.register();

        runCmdChannelListener = new RunCmdChannelListener(this, LOGGER, api);
        runCmdChannelListener.register();

        runCmdCommand = new RunCmdCommand(this, runCmdChannelListener, config.getMessageFormatMap(), api);
        runCmdCommand.register();

        chatMessageChannelListener = new ChatMessageChannelListener(this, api);
        chatMessageChannelListener.register();

        spigotKeepAliveSender = new SpigotKeepAliveSender(api, 2000);
        spigotKeepAliveSender.register();

        try {
            socketClient = new SocketClient(config.getHostName(), config.getPort(), connection);
            socketClient.start();
        } catch (Exception e) {
            getLogger().severe("============================================================");
            getLogger().severe("The SockExchange client could not be started. Refer to the stacktrace below.");
            getLogger().severe("Regardless, reconnects will be attempted.");
            e.printStackTrace();
            getLogger().severe("============================================================");
        }

        ScheduledExecutorService executorService = api.getScheduledExecutorService();
        executorService.schedule(() -> socket.handshake(config.getServerName()), 3, TimeUnit.SECONDS);
    }

    @Override
    public void onDisable() {
        shutdownAwaitableExecutor();
        socketClient.shutdown();
        spigotKeepAliveSender.unregister();
        chatMessageChannelListener.unregister();
        runCmdCommand.unregister();
        runCmdChannelListener.unregister();
        moveToCommand.unregister();
        moveOtherToCommand.unregister();
        keepAliveChannelListener.unregister();
        playerUpdateChannelListener.unregister();
        consumerTimeoutCleanupFuture.cancel(false);
        responseConsumerMap.clear();
        shutdownThreadPoolExecutor();
    }

    @Override
    public boolean doesRegistrationPasswordMatch(String password) {
        return false;
    }

    @Override
    public SpigotToBungeeConnection getConnection() {
        return connection;
    }

    @Override
    public BungeeToSpigotConnection getConnection(String spigotServerName) {
        return null;
    }

    @Override
    public Collection<BungeeToSpigotConnection> getConnections() {
        return null;
    }

    @Override
    public SpigotServerInfo getServerInfo(String serverName) {
        return keepAliveChannelListener.getServerInfo(serverName);
    }

    @Override
    public Collection<SpigotServerInfo> getServerInfos() {
        return keepAliveChannelListener.getServerInfos();
    }

    @Override
    public String getServerNameForPlayer(String playerName) {
        return null;
    }

    @Override
    public void sendChatMessagesToPlayer(String playerName, List<String> messages) {

    }

    @Override
    public void sendChatMessagesToConsole(List<String> messages) {
        getServer().getScheduler().runTask(this, () ->
        {
            CommandSender receiver = getServer().getConsoleSender();
            for (String message : messages) {
                receiver.sendMessage(message);
            }
        });
    }

    @Override
    public Set<String> getOnlinePlayerNames() {
        return playerUpdateChannelListener.getOnlinePlayerNames();
    }

    @Override
    public Set<String> getOnlinePlayerNames(String serverName) {
        return null;
    }

    @Override
    public void isPlayerOnServer(String serverName, String playerName, Consumer<Boolean> consumer) {

    }

    @Override
    public void isPlayerOnServer(String playerName, Consumer<Boolean> consumer) {
        getServer().getScheduler().runTask(this, () ->
        {
            Player player = getServer().getPlayerExact(playerName);
            consumer.accept(player != null);
        });
    }

    public void executeSync(Runnable runnable) {
        Preconditions.checkNotNull(runnable, "runnable");

        getServer().getScheduler().runTask(this, runnable);
    }

    private void checkForConsumerTimeouts() {
        long currentTimeMillis = System.currentTimeMillis();

        responseConsumerMap.removeIf((entry) ->
        {
            ExpirableConsumer<ResponseMessage> responseConsumer = entry.getValue();

            if (responseConsumer.getExpiresAtMillis() > currentTimeMillis) {
                return false;
            }

            awaitableExecutor.execute(() ->
            {
                ResponseMessage responseMessage = new ResponseMessage(ResponseStatus.TIMED_OUT);
                responseConsumer.accept(responseMessage);
            });

            return true;
        });
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

    private ScheduledThreadPoolExecutor buildThreadPoolExecutor() {
        ThreadFactoryBuilder factoryBuilder = new ThreadFactoryBuilder();
        factoryBuilder.setNameFormat("SockExchange-Scheduler-Thread-%d");

        ThreadFactory threadFactory = factoryBuilder.build();
        threadPoolExecutor = new ScheduledThreadPoolExecutor(2, threadFactory);

        threadPoolExecutor.setMaximumPoolSize(8);
        threadPoolExecutor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        threadPoolExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        return threadPoolExecutor;
    }

    private void shutdownThreadPoolExecutor() {
        if (!threadPoolExecutor.isShutdown()) {
            // Disable new tasks from being submitted to service
            threadPoolExecutor.shutdown();

            getLogger().info("ScheduledThreadPoolExecutor being shutdown()");

            try {
                // Await termination for a minute
                if (!threadPoolExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                    // Force shutdown
                    threadPoolExecutor.shutdownNow();

                    getLogger().severe("ScheduledThreadPoolExecutor being shutdownNow()");

                    // Await termination again for another minute
                    if (!threadPoolExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                        getLogger().severe("ScheduledThreadPoolExecutor not shutdown after shutdownNow()");
                    }
                }
            } catch (InterruptedException ex) {
                getLogger().severe("ScheduledThreadPoolExecutor shutdown interrupted");

                // Re-cancel if current thread also interrupted
                threadPoolExecutor.shutdownNow();

                getLogger().severe("ScheduledThreadPoolExecutor being shutdownNow()");

                // Preserve interrupt status
                Thread.currentThread().interrupt();
            }
        }
    }

    public static Socket getSocket() {
        return socket;
    }
}
