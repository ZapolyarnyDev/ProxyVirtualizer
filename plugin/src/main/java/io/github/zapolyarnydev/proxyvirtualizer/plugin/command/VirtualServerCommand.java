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
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public final class VirtualServerCommand implements SimpleCommand {

    private static final List<String> SUBCOMMANDS = List.of(
            "help", "list", "launch", "stop", "connect", "disconnect", "allow-protocol", "deny-protocol", "packet-map"
    );

    private final ServerContainer serverContainer;
    private final Launcher launcher;
    private final Connector connector;

    public VirtualServerCommand(ServerContainer serverContainer, Launcher launcher, Connector connector) {
        this.serverContainer = serverContainer;
        this.launcher = launcher;
        this.connector = connector;
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
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return serverContainer.getServers().stream()
                    .map(VirtualServer::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .sorted()
                    .toList();
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
            message(source, "Connected to virtual server: " + serverOptional.get().getName());
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

    private void sendHelp(CommandSource source) {
        List<String> lines = Arrays.asList(
                "/vserver list",
                "/vserver launch <name>",
                "/vserver stop <name>",
                "/vserver connect <name>",
                "/vserver disconnect",
                "/vserver allow-protocol <server> <protocolVersion>",
                "/vserver deny-protocol <server> <protocolVersion>",
                "/vserver packet-map <server> <packetKey> <protocolVersion> <packetVersion>"
        );

        message(source, "ProxyVirtualizer commands:");
        for (String line : lines) {
            message(source, " - " + line);
        }
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
}
