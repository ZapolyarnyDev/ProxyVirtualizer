package io.github.zapolyarnydev.proxyvirtualizer.plugin.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.zapolyarnydev.proxyvirtualizer.api.connector.Connector;
import io.github.zapolyarnydev.proxyvirtualizer.api.exception.PlayerAlreadyConnectedException;
import io.github.zapolyarnydev.proxyvirtualizer.api.exception.VirtualServerAlreadyLaunchedException;
import io.github.zapolyarnydev.proxyvirtualizer.api.registry.ServerContainer;
import io.github.zapolyarnydev.proxyvirtualizer.api.server.Launcher;
import io.github.zapolyarnydev.proxyvirtualizer.api.server.VirtualServer;
import io.github.zapolyarnydev.proxyvirtualizer.plugin.packet.VelocityVirtualPacketSender;
import io.github.zapolyarnydev.proxyvirtualizer.plugin.text.AdventureComponentParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public final class VirtualServerCommand implements SimpleCommand {
    private static final AdventureComponentParser COMPONENT_PARSER = new AdventureComponentParser();

    private static final List<String> SUBCOMMANDS = List.of(
            "help",
            "list",
            "launch",
            "stop",
            "connect",
            "disconnect",
            "allow-protocol",
            "deny-protocol",
            "packet-map",
            "packet"
    );

    private final ServerContainer serverContainer;
    private final ProxyServer proxyServer;
    private final Launcher launcher;
    private final Connector connector;
    private final VelocityVirtualPacketSender packetSender;

    public VirtualServerCommand(
            ServerContainer serverContainer,
            ProxyServer proxyServer,
            Launcher launcher,
            Connector connector,
            VelocityVirtualPacketSender packetSender
    ) {
        this.serverContainer = serverContainer;
        this.proxyServer = proxyServer;
        this.launcher = launcher;
        this.connector = connector;
        this.packetSender = packetSender;
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 0) {
            sendHelp(invocation.source());
            return;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        switch (subcommand) {
            case "help" -> sendHelp(invocation.source());
            case "list" -> handleList(invocation.source());
            case "launch" -> handleLaunch(invocation.source(), args);
            case "stop" -> handleStop(invocation.source(), args);
            case "connect" -> handleConnect(invocation.source(), args);
            case "disconnect", "leave" -> handleDisconnect(invocation.source(), args);
            case "allow-protocol" -> handleAllowProtocol(invocation.source(), args);
            case "deny-protocol" -> handleDenyProtocol(invocation.source(), args);
            case "packet-map" -> handlePacketMap(invocation.source(), args);
            case "packet" -> handlePacket(invocation.source(), args);
            default -> {
                error(invocation.source(), "Unknown subcommand: " + args[0]);
                sendHelp(invocation.source());
            }
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 0) {
            return SUBCOMMANDS;
        }

        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return SUBCOMMANDS.stream().filter(value -> value.startsWith(prefix)).toList();
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        if (List.of("stop", "connect", "allow-protocol", "deny-protocol", "packet-map").contains(subcommand)
                && args.length == 2) {
            return suggestServerNames(args[1]);
        }

        if ("packet".equals(subcommand) && args.length == 2) {
            return filterPrefix(List.of("limbo", "keepalive", "chat", "actionbar", "title", "disconnect"), args[1]);
        }

        if ("packet".equals(subcommand) && args.length == 3) {
            return suggestServerNames(args[2]);
        }

        return List.of();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("proxyvirtualizer.command");
    }

    private void handleList(CommandSource source) {
        List<VirtualServer> servers = new ArrayList<>(serverContainer.getServers());
        if (servers.isEmpty()) {
            info(source, "No virtual servers launched.");
            return;
        }

        String names = servers.stream().map(VirtualServer::getName).sorted().collect(Collectors.joining(", "));
        info(source, "Virtual servers (" + servers.size() + "): " + names);
    }

    private void handleLaunch(CommandSource source, String[] args) {
        if (args.length < 2) {
            usage(source, "/vserver launch <name>");
            return;
        }

        try {
            VirtualServer virtualServer = launcher.launch(args[1]);
            success(source, "Launched virtual server: " + virtualServer.getName());
        } catch (VirtualServerAlreadyLaunchedException exception) {
            error(source, exception.getMessage());
        } catch (IllegalArgumentException exception) {
            error(source, "Invalid server name: " + exception.getMessage());
        }
    }

    private void handleStop(CommandSource source, String[] args) {
        if (args.length < 2) {
            usage(source, "/vserver stop <name>");
            return;
        }

        launcher.stop(args[1]);
        success(source, "Stop requested for virtual server: " + args[1]);
    }

    private void handleConnect(CommandSource source, String[] args) {
        if (args.length < 2) {
            usage(source, "/vserver connect <name> [player]");
            return;
        }

        Player player = resolveTargetPlayer(source, args, 2);
        if (player == null) {
            return;
        }

        Optional<VirtualServer> serverOptional = serverContainer.findServerByName(args[1]);
        if (serverOptional.isEmpty()) {
            error(source, "Virtual server not found: " + args[1]);
            return;
        }

        try {
            boolean connected = connector.connect(serverOptional.get(), player);
            if (!connected) {
                error(source, "Failed to enter virtual server. Expected limbo-capable client/protocol (target: 1.21.4) and successful bootstrap.");
                return;
            }
            success(source, "Connected " + player.getUsername() + " to virtual server: " + serverOptional.get().getName()
                    + " (backend connection detached)");
        } catch (PlayerAlreadyConnectedException exception) {
            error(source, exception.getMessage());
        }
    }

    private void handleDisconnect(CommandSource source, String[] args) {
        Player player = resolveTargetPlayer(source, args, 1);
        if (player == null) {
            return;
        }

        boolean disconnected = connector.disconnect(player);
        if (!disconnected) {
            info(source, player.getUsername() + " is not connected to a virtual server.");
            return;
        }

        connector.sendToPreviousServer(player);
        success(source, "Disconnected " + player.getUsername() + " from virtual server.");
    }

    private void handleAllowProtocol(CommandSource source, String[] args) {
        if (args.length < 3) {
            usage(source, "/vserver allow-protocol <server> <protocolVersion>");
            return;
        }

        Optional<VirtualServer> serverOptional = serverContainer.findServerByName(args[1]);
        if (serverOptional.isEmpty()) {
            error(source, "Virtual server not found: " + args[1]);
            return;
        }

        Integer protocolVersion = parseInt(args[2]);
        if (protocolVersion == null) {
            error(source, "Protocol version must be an integer.");
            return;
        }

        serverOptional.get().allowProtocolVersion(protocolVersion);
        success(source, "Allowed protocol " + protocolVersion + " for " + serverOptional.get().getName());
    }

    private void handleDenyProtocol(CommandSource source, String[] args) {
        if (args.length < 3) {
            usage(source, "/vserver deny-protocol <server> <protocolVersion>");
            return;
        }

        Optional<VirtualServer> serverOptional = serverContainer.findServerByName(args[1]);
        if (serverOptional.isEmpty()) {
            error(source, "Virtual server not found: " + args[1]);
            return;
        }

        Integer protocolVersion = parseInt(args[2]);
        if (protocolVersion == null) {
            error(source, "Protocol version must be an integer.");
            return;
        }

        serverOptional.get().disallowProtocolVersion(protocolVersion);
        success(source, "Denied protocol " + protocolVersion + " for " + serverOptional.get().getName());
    }

    private void handlePacketMap(CommandSource source, String[] args) {
        if (args.length < 5) {
            usage(source, "/vserver packet-map <server> <packetKey> <protocolVersion> <packetVersion>");
            return;
        }

        Optional<VirtualServer> serverOptional = serverContainer.findServerByName(args[1]);
        if (serverOptional.isEmpty()) {
            error(source, "Virtual server not found: " + args[1]);
            return;
        }

        Integer protocolVersion = parseInt(args[3]);
        Integer packetVersion = parseInt(args[4]);
        if (protocolVersion == null || packetVersion == null) {
            error(source, "Protocol version and packet version must be integers.");
            return;
        }

        VirtualServer.PacketVersionRule rule =
                serverOptional.get().registerPacketVersion(args[2], protocolVersion, packetVersion);
        success(source, "Packet mapping set: " + rule.packetKey()
                + " protocol=" + rule.protocolVersion()
                + " packet=" + rule.packetVersion());
    }

    private void handlePacket(CommandSource source, String[] args) {
        if (args.length < 3) {
            usage(source, "/vserver packet <limbo|keepalive|chat|actionbar|title|disconnect> <server> [payload]");
            return;
        }

        String action = args[1].toLowerCase(Locale.ROOT);
        Optional<VirtualServer> serverOptional = serverContainer.findServerByName(args[2]);
        if (serverOptional.isEmpty()) {
            error(source, "Virtual server not found: " + args[2]);
            return;
        }

        VirtualServer virtualServer = serverOptional.get();
        switch (action) {
            case "limbo" -> {
                int sent = packetSender.broadcastVoidLimboBootstrap(virtualServer);
                success(source, "Void limbo bootstrap sent to " + sent + " player(s) in " + virtualServer.getName() + " (target 1.21.4).");
            }
            case "keepalive" -> {
                int sent = packetSender.broadcastKeepAlive(virtualServer);
                success(source, "KeepAlive sent to " + sent + " player(s) in " + virtualServer.getName());
            }
            case "chat" -> {
                String text = joinTail(args, 3);
                if (text.isBlank()) {
                    usage(source, "/vserver packet chat <server> <message>");
                    return;
                }
                int sent = packetSender.broadcastChat(virtualServer, COMPONENT_PARSER.parse(text));
                success(source, "Chat packet sent to " + sent + " player(s).");
            }
            case "actionbar" -> {
                String text = joinTail(args, 3);
                if (text.isBlank()) {
                    usage(source, "/vserver packet actionbar <server> <message>");
                    return;
                }
                int sent = packetSender.broadcastActionBar(virtualServer, COMPONENT_PARSER.parse(text));
                success(source, "ActionBar packet sent to " + sent + " player(s).");
            }
            case "title" -> {
                String text = joinTail(args, 3);
                if (text.isBlank()) {
                    usage(source, "/vserver packet title <server> <title[||subtitle]>");
                    return;
                }
                String[] titleParts = text.split("\\|\\|", 2);
                Component title = COMPONENT_PARSER.parse(titleParts[0]);
                Component subtitle = titleParts.length > 1 ? COMPONENT_PARSER.parse(titleParts[1]) : Component.empty();
                int sent = packetSender.broadcastTitle(virtualServer, title, subtitle);
                success(source, "Title packet sent to " + sent + " player(s).");
            }
            case "disconnect" -> {
                String text = joinTail(args, 3);
                Component reason = text.isBlank()
                        ? Component.text("Disconnected from virtual server")
                        : COMPONENT_PARSER.parse(text);
                int sent = packetSender.broadcastDisconnect(virtualServer, reason);
                success(source, "Disconnect packet sent to " + sent + " player(s).");
            }
            default -> error(source, "Unknown packet action: " + action);
        }
    }

    private void sendHelp(CommandSource source) {
        List<String> lines = Arrays.asList(
                "/vserver list",
                "/vserver launch <name>",
                "/vserver stop <name>",
                "/vserver connect <name>",
                "/vserver connect <name> <player>",
                "/vserver disconnect",
                "/vserver disconnect <player>",
                "/vserver allow-protocol <server> <protocolVersion>",
                "/vserver deny-protocol <server> <protocolVersion>",
                "/vserver packet-map <server> <packetKey> <protocolVersion> <packetVersion>",
                "/vserver packet limbo <server>",
                "/vserver packet keepalive <server>",
                "/vserver packet actionbar <server> <message>",
                "/vserver packet chat <server> <message>",
                "/vserver packet title <server> <title[||subtitle]>",
                "/vserver packet disconnect <server> [reason]",
                "Message formats: mm:<...> | legacy:&a... | json:{...} (default tries MiniMessage)"
        );

        info(source, "ProxyVirtualizer commands:");
        for (String line : lines) {
            helpLine(source, line);
        }
    }

    private List<String> suggestServerNames(String rawPrefix) {
        String prefix = rawPrefix.toLowerCase(Locale.ROOT);
        return serverContainer.getServers().stream()
                .map(VirtualServer::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                .sorted()
                .toList();
    }

    private static List<String> filterPrefix(List<String> values, String rawPrefix) {
        String prefix = rawPrefix.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value.startsWith(prefix))
                .toList();
    }

    private static void info(CommandSource source, String message) {
        send(source, "INFO", NamedTextColor.AQUA, message, NamedTextColor.GRAY);
    }

    private static void success(CommandSource source, String message) {
        send(source, "OK", NamedTextColor.GREEN, message, NamedTextColor.GRAY);
    }

    private static void error(CommandSource source, String message) {
        send(source, "ERR", NamedTextColor.RED, message, NamedTextColor.GRAY);
    }

    private static void usage(CommandSource source, String usage) {
        send(source, "USAGE", NamedTextColor.YELLOW, usage, NamedTextColor.GOLD);
    }

    private static void helpLine(CommandSource source, String line) {
        Component body = line.startsWith("/")
                ? Component.text(" - ", NamedTextColor.DARK_GRAY)
                .append(Component.text(line, NamedTextColor.GOLD))
                : Component.text(" - " + line, NamedTextColor.GRAY);
        source.sendMessage(prefix().append(body));
    }

    private static void send(
            CommandSource source,
            String tag,
            NamedTextColor tagColor,
            String message,
            NamedTextColor messageColor
    ) {
        source.sendMessage(prefix()
                .append(Component.text("[" + tag + "] ", tagColor, TextDecoration.BOLD))
                .append(Component.text(message, messageColor)));
    }

    private static Component prefix() {
        return Component.text("[", NamedTextColor.DARK_GRAY)
                .append(Component.text("ProxyVirtualizer", NamedTextColor.AQUA, TextDecoration.BOLD))
                .append(Component.text("] ", NamedTextColor.DARK_GRAY));
    }

    private static Integer parseInt(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Player resolveTargetPlayer(CommandSource source, String[] args, int playerArgIndex) {
        if (args.length > playerArgIndex) {
            String playerName = args[playerArgIndex];
            Optional<Player> playerOptional = proxyServer.getPlayer(playerName);
            if (playerOptional.isEmpty()) {
                error(source, "Player not found: " + playerName);
                return null;
            }
            return playerOptional.get();
        }

        if (source instanceof Player player) {
            return player;
        }

        error(source, "This subcommand requires a player argument when used from console.");
        return null;
    }

    private static String joinTail(String[] args, int fromIndex) {
        if (args.length <= fromIndex) {
            return "";
        }
        return String.join(" ", Arrays.copyOfRange(args, fromIndex, args.length)).trim();
    }
}
