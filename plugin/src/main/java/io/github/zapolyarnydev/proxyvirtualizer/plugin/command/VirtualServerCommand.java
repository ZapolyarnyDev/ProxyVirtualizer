package io.github.zapolyarnydev.proxyvirtualizer.plugin.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import io.github.zapolyarnydev.proxyvirtualizer.api.connector.Connector;
import io.github.zapolyarnydev.proxyvirtualizer.api.exception.PlayerAlreadyConnectedException;
import io.github.zapolyarnydev.proxyvirtualizer.api.exception.VirtualServerAlreadyLaunchedException;
import io.github.zapolyarnydev.proxyvirtualizer.api.registry.ServerContainer;
import io.github.zapolyarnydev.proxyvirtualizer.api.server.Launcher;
import io.github.zapolyarnydev.proxyvirtualizer.api.server.VirtualServer;
import io.github.zapolyarnydev.proxyvirtualizer.plugin.packet.VelocityVirtualPacketSender;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public final class VirtualServerCommand implements SimpleCommand {

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
    private final Launcher launcher;
    private final Connector connector;
    private final VelocityVirtualPacketSender packetSender;

    public VirtualServerCommand(
            ServerContainer serverContainer,
            Launcher launcher,
            Connector connector,
            VelocityVirtualPacketSender packetSender
    ) {
        this.serverContainer = serverContainer;
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
            case "disconnect", "leave" -> handleDisconnect(invocation.source());
            case "allow-protocol" -> handleAllowProtocol(invocation.source(), args);
            case "deny-protocol" -> handleDenyProtocol(invocation.source(), args);
            case "packet-map" -> handlePacketMap(invocation.source(), args);
            case "packet" -> handlePacket(invocation.source(), args);
            default -> {
                message(invocation.source(), "Unknown subcommand: " + args[0]);
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
            return filterPrefix(List.of("keepalive", "chat", "actionbar", "title", "disconnect"), args[1]);
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
            message(source, "No virtual servers launched.");
            return;
        }

        String names = servers.stream().map(VirtualServer::getName).sorted().collect(Collectors.joining(", "));
        message(source, "Virtual servers (" + servers.size() + "): " + names);
    }

    private void handleLaunch(CommandSource source, String[] args) {
        if (args.length < 2) {
            message(source, "Usage: /vserver launch <name>");
            return;
        }

        try {
            VirtualServer virtualServer = launcher.launch(args[1]);
            message(source, "Launched virtual server: " + virtualServer.getName());
        } catch (VirtualServerAlreadyLaunchedException exception) {
            message(source, exception.getMessage());
        } catch (IllegalArgumentException exception) {
            message(source, "Invalid server name: " + exception.getMessage());
        }
    }

    private void handleStop(CommandSource source, String[] args) {
        if (args.length < 2) {
            message(source, "Usage: /vserver stop <name>");
            return;
        }

        launcher.stop(args[1]);
        message(source, "Stop requested for virtual server: " + args[1]);
    }

    private void handleConnect(CommandSource source, String[] args) {
        if (!(source instanceof Player player)) {
            message(source, "This subcommand can only be used by a player.");
            return;
        }

        if (args.length < 2) {
            message(source, "Usage: /vserver connect <name>");
            return;
        }

        Optional<VirtualServer> serverOptional = serverContainer.findServerByName(args[1]);
        if (serverOptional.isEmpty()) {
            message(source, "Virtual server not found: " + args[1]);
            return;
        }

        try {
            boolean connected = connector.connect(serverOptional.get(), player);
            if (!connected) {
                message(source, "Your protocol version is not supported by this virtual server.");
                return;
            }
            message(source, "Connected to virtual server: " + serverOptional.get().getName()
                    + " (backend connection detached)");
        } catch (PlayerAlreadyConnectedException exception) {
            message(source, exception.getMessage());
        }
    }

    private void handleDisconnect(CommandSource source) {
        if (!(source instanceof Player player)) {
            message(source, "This subcommand can only be used by a player.");
            return;
        }

        boolean disconnected = connector.disconnect(player);
        if (!disconnected) {
            message(source, "You are not connected to a virtual server.");
            return;
        }

        connector.sendToPreviousServer(player);
        message(source, "Disconnected from virtual server.");
    }

    private void handleAllowProtocol(CommandSource source, String[] args) {
        if (args.length < 3) {
            message(source, "Usage: /vserver allow-protocol <server> <protocolVersion>");
            return;
        }

        Optional<VirtualServer> serverOptional = serverContainer.findServerByName(args[1]);
        if (serverOptional.isEmpty()) {
            message(source, "Virtual server not found: " + args[1]);
            return;
        }

        Integer protocolVersion = parseInt(args[2]);
        if (protocolVersion == null) {
            message(source, "Protocol version must be an integer.");
            return;
        }

        serverOptional.get().allowProtocolVersion(protocolVersion);
        message(source, "Allowed protocol " + protocolVersion + " for " + serverOptional.get().getName());
    }

    private void handleDenyProtocol(CommandSource source, String[] args) {
        if (args.length < 3) {
            message(source, "Usage: /vserver deny-protocol <server> <protocolVersion>");
            return;
        }

        Optional<VirtualServer> serverOptional = serverContainer.findServerByName(args[1]);
        if (serverOptional.isEmpty()) {
            message(source, "Virtual server not found: " + args[1]);
            return;
        }

        Integer protocolVersion = parseInt(args[2]);
        if (protocolVersion == null) {
            message(source, "Protocol version must be an integer.");
            return;
        }

        serverOptional.get().disallowProtocolVersion(protocolVersion);
        message(source, "Denied protocol " + protocolVersion + " for " + serverOptional.get().getName());
    }

    private void handlePacketMap(CommandSource source, String[] args) {
        if (args.length < 5) {
            message(source, "Usage: /vserver packet-map <server> <packetKey> <protocolVersion> <packetVersion>");
            return;
        }

        Optional<VirtualServer> serverOptional = serverContainer.findServerByName(args[1]);
        if (serverOptional.isEmpty()) {
            message(source, "Virtual server not found: " + args[1]);
            return;
        }

        Integer protocolVersion = parseInt(args[3]);
        Integer packetVersion = parseInt(args[4]);
        if (protocolVersion == null || packetVersion == null) {
            message(source, "Protocol version and packet version must be integers.");
            return;
        }

        VirtualServer.PacketVersionRule rule =
                serverOptional.get().registerPacketVersion(args[2], protocolVersion, packetVersion);
        message(source, "Packet mapping set: " + rule.packetKey()
                + " protocol=" + rule.protocolVersion()
                + " packet=" + rule.packetVersion());
    }

    private void handlePacket(CommandSource source, String[] args) {
        if (args.length < 3) {
            message(source, "Usage: /vserver packet <keepalive|chat|actionbar|title|disconnect> <server> [payload]");
            return;
        }

        String action = args[1].toLowerCase(Locale.ROOT);
        Optional<VirtualServer> serverOptional = serverContainer.findServerByName(args[2]);
        if (serverOptional.isEmpty()) {
            message(source, "Virtual server not found: " + args[2]);
            return;
        }

        VirtualServer virtualServer = serverOptional.get();
        switch (action) {
            case "keepalive" -> {
                int sent = packetSender.broadcastKeepAlive(virtualServer);
                message(source, "KeepAlive sent to " + sent + " player(s) in " + virtualServer.getName());
            }
            case "chat" -> {
                String text = joinTail(args, 3);
                if (text.isBlank()) {
                    message(source, "Usage: /vserver packet chat <server> <message>");
                    return;
                }
                int sent = packetSender.broadcastChat(virtualServer, Component.text(text));
                message(source, "Chat packet sent to " + sent + " player(s).");
            }
            case "actionbar" -> {
                String text = joinTail(args, 3);
                if (text.isBlank()) {
                    message(source, "Usage: /vserver packet actionbar <server> <message>");
                    return;
                }
                int sent = packetSender.broadcastActionBar(virtualServer, Component.text(text));
                message(source, "ActionBar packet sent to " + sent + " player(s).");
            }
            case "title" -> {
                String text = joinTail(args, 3);
                if (text.isBlank()) {
                    message(source, "Usage: /vserver packet title <server> <title text>");
                    return;
                }
                int sent = packetSender.broadcastTitle(virtualServer, Component.text(text), Component.empty());
                message(source, "Title packet sent to " + sent + " player(s).");
            }
            case "disconnect" -> {
                String text = joinTail(args, 3);
                Component reason = Component.text(text.isBlank() ? "Disconnected from virtual server" : text);
                int sent = packetSender.broadcastDisconnect(virtualServer, reason);
                message(source, "Disconnect packet sent to " + sent + " player(s).");
            }
            default -> message(source, "Unknown packet action: " + action);
        }
    }

    private void sendHelp(CommandSource source) {
        List<String> lines = Arrays.asList(
                "/vserver list",
                "/vserver launch <name>",
                "/vserver stop <name>",
                "/vserver connect <name>",
                "/vserver disconnect",
                "/vserver allow-protocol <server> <protocolVersion>",
                "/vserver deny-protocol <server> <protocolVersion>",
                "/vserver packet-map <server> <packetKey> <protocolVersion> <packetVersion>",
                "/vserver packet keepalive <server>",
                "/vserver packet actionbar <server> <message>",
                "/vserver packet chat <server> <message>",
                "/vserver packet title <server> <title>",
                "/vserver packet disconnect <server> [reason]"
        );

        message(source, "ProxyVirtualizer commands:");
        for (String line : lines) {
            message(source, " - " + line);
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

    private static void message(CommandSource source, String message) {
        source.sendMessage(Component.text("[ProxyVirtualizer] " + message));
    }

    private static Integer parseInt(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String joinTail(String[] args, int fromIndex) {
        if (args.length <= fromIndex) {
            return "";
        }
        return String.join(" ", Arrays.copyOfRange(args, fromIndex, args.length)).trim();
    }
}
