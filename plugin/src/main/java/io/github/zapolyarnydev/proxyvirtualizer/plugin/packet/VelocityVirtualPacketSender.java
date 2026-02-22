package io.github.zapolyarnydev.proxyvirtualizer.plugin.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.zapolyarnydev.proxyvirtualizer.api.connector.ConnectionStorage;
import io.github.zapolyarnydev.proxyvirtualizer.api.server.VirtualServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;

import java.lang.reflect.Constructor;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public final class VelocityVirtualPacketSender {
    private static final String BYTE_BUF_CLASS = "io.netty.buffer.ByteBuf";
    private static final String UNPOOLED_CLASS = "io.netty.buffer.Unpooled";
    private static final String PROTOCOL_UTILS_CLASS = "com.velocitypowered.proxy.protocol.ProtocolUtils";
    private static final String DIMENSION_INFO_CLASS = "com.velocitypowered.proxy.connection.registry.DimensionInfo";
    private static final String RESPAWN_PACKET_CLASS = "com.velocitypowered.proxy.protocol.packet.RespawnPacket";
    private static final String COMPOUND_BINARY_TAG_CLASS = "net.kyori.adventure.nbt.CompoundBinaryTag";
    private static final String FASTUTIL_PAIR_CLASS = "it.unimi.dsi.fastutil.Pair";

    private static final int OVERWORLD_DIMENSION_ID = 0;
    private static final int NETHER_DIMENSION_ID = 1;
    private static final int GAME_EVENT_PACKET_ID_1_21_4 = 0x23;
    private static final int PLAYER_POSITION_PACKET_ID_1_21_4 = 0x42;
    private static final int GAME_EVENT_START_WAITING_FOR_LEVEL_CHUNKS = 13;
    private static final long LIMBO_SEED_HASH = 0L;
    private static final short GAMEMODE_SPECTATOR = 3;
    private static final short PREVIOUS_GAMEMODE_UNKNOWN = -1;
    private static final byte RESPAWN_KEEP_NOTHING = 0;
    private static final int LIMBO_SEA_LEVEL = 63;
    private static final double LIMBO_X = 0.0D;
    private static final double LIMBO_Y = 1024.0D;
    private static final double LIMBO_Z = 0.0D;
    private static final float LIMBO_YAW = 0.0F;
    private static final float LIMBO_PITCH = 0.0F;
    private static final int TELEPORT_FLAGS_ABSOLUTE = 0;

    private final ProxyServer proxyServer;
    private final ConnectionStorage connectionStorage;
    private final AtomicInteger teleportIdSequence = new AtomicInteger(1);

    public VelocityVirtualPacketSender(ProxyServer proxyServer, ConnectionStorage connectionStorage) {
        this.proxyServer = Objects.requireNonNull(proxyServer, "proxyServer");
        this.connectionStorage = Objects.requireNonNull(connectionStorage, "connectionStorage");
    }

    public boolean sendKeepAlive(VirtualServer virtualServer, Player player) {
        if (!canSend(virtualServer, player, VirtualPacketKeys.KEEP_ALIVE)) {
            return false;
        }

        try {
            player.getClass().getMethod("sendKeepAlive").invoke(player);
            return true;
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    public boolean sendChat(VirtualServer virtualServer, Player player, Component message) {
        if (!canSend(virtualServer, player, VirtualPacketKeys.CHAT)) {
            return false;
        }

        player.sendMessage(message);
        return true;
    }

    public boolean sendActionBar(VirtualServer virtualServer, Player player, Component message) {
        if (!canSend(virtualServer, player, VirtualPacketKeys.ACTION_BAR)) {
            return false;
        }

        player.sendActionBar(message);
        return true;
    }

    public boolean sendTitle(VirtualServer virtualServer, Player player, Component title, Component subtitle) {
        if (!canSend(virtualServer, player, VirtualPacketKeys.TITLE)) {
            return false;
        }

        player.showTitle(Title.title(title, subtitle));
        return true;
    }

    public boolean disconnectClient(VirtualServer virtualServer, Player player, Component reason) {
        if (!canSend(virtualServer, player, VirtualPacketKeys.DISCONNECT)) {
            return false;
        }
        player.disconnect(reason);
        return true;
    }

    public boolean bootstrapVoidLimbo(VirtualServer virtualServer, Player player) {
        if (!canSend(virtualServer, player, VirtualPacketKeys.LIMBO_BOOTSTRAP)) {
            return false;
        }

        if (!ProtocolVersion.MINECRAFT_1_21_4.equals(player.getProtocolVersion())) {
            return false;
        }

        if (!canSend(virtualServer, player, VirtualPacketKeys.RESPAWN)) {
            return false;
        }
        if (!canSend(virtualServer, player, VirtualPacketKeys.GAME_EVENT)) {
            return false;
        }
        if (!canSend(virtualServer, player, VirtualPacketKeys.PLAYER_POSITION)) {
            return false;
        }

        try {
            Object connection = player.getClass().getMethod("getConnection").invoke(player);
            if (connection == null) {
                return false;
            }

            Object netherRespawn = createRespawnPacket(
                    player.getProtocolVersion(),
                    NETHER_DIMENSION_ID,
                    "minecraft:the_nether",
                    false
            );
            Object overworldRespawn = createRespawnPacket(
                    player.getProtocolVersion(),
                    OVERWORLD_DIMENSION_ID,
                    "minecraft:overworld",
                    true
            );

            connection.getClass().getMethod("write", Object.class).invoke(connection, netherRespawn);
            connection.getClass().getMethod("write", Object.class).invoke(connection, overworldRespawn);
            sendStartWaitingForLevelChunksGameEvent(virtualServer, player, connection);
            sendSynchronizePlayerPosition(virtualServer, player, connection);

            sendKeepAlive(virtualServer, player);
            return true;
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    public int broadcastKeepAlive(VirtualServer virtualServer) {
        int sent = 0;
        for (Player player : proxyServer.getAllPlayers()) {
            if (sendKeepAlive(virtualServer, player)) {
                sent++;
            }
        }
        return sent;
    }

    public int broadcastChat(VirtualServer virtualServer, Component message) {
        int sent = 0;
        for (Player player : proxyServer.getAllPlayers()) {
            if (sendChat(virtualServer, player, message)) {
                sent++;
            }
        }
        return sent;
    }

    public int broadcastActionBar(VirtualServer virtualServer, Component message) {
        int sent = 0;
        for (Player player : proxyServer.getAllPlayers()) {
            if (sendActionBar(virtualServer, player, message)) {
                sent++;
            }
        }
        return sent;
    }

    public int broadcastTitle(VirtualServer virtualServer, Component title, Component subtitle) {
        int sent = 0;
        for (Player player : proxyServer.getAllPlayers()) {
            if (sendTitle(virtualServer, player, title, subtitle)) {
                sent++;
            }
        }
        return sent;
    }

    public int broadcastDisconnect(VirtualServer virtualServer, Component reason) {
        int sent = 0;
        for (Player player : proxyServer.getAllPlayers()) {
            if (disconnectClient(virtualServer, player, reason)) {
                sent++;
            }
        }
        return sent;
    }

    public int broadcastVoidLimboBootstrap(VirtualServer virtualServer) {
        int sent = 0;
        for (Player player : proxyServer.getAllPlayers()) {
            if (bootstrapVoidLimbo(virtualServer, player)) {
                sent++;
            }
        }
        return sent;
    }

    private boolean canSend(VirtualServer virtualServer, Player player, String packetKey) {
        Objects.requireNonNull(virtualServer, "virtualServer");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(packetKey, "packetKey");

        boolean inTargetVirtualServer = connectionStorage.getVirtualServer(player)
                .map(virtualServer::equals)
                .orElse(false);
        if (!inTargetVirtualServer) {
            return false;
        }

        int protocolVersion = player.getProtocolVersion().getProtocol();
        if (!virtualServer.isProtocolVersionSupported(protocolVersion)) {
            return false;
        }

        return virtualServer.getPacketVersionMatrix().containsKey(packetKey)
                ? virtualServer.getPacketVersion(packetKey, protocolVersion).isPresent()
                : true;
    }

    private Object createRespawnPacket(
            ProtocolVersion protocolVersion,
            int dimensionId,
            String levelName,
            boolean flatWorld
    ) throws ReflectiveOperationException {
        ClassLoader classLoader = getClass().getClassLoader();

        Class<?> dimensionInfoClass = Class.forName(DIMENSION_INFO_CLASS, true, classLoader);
        Constructor<?> dimensionInfoConstructor = dimensionInfoClass.getConstructor(
                String.class,
                String.class,
                boolean.class,
                boolean.class,
                ProtocolVersion.class
        );
        Object dimensionInfo = dimensionInfoConstructor.newInstance("", levelName, flatWorld, false, protocolVersion);

        Class<?> respawnPacketClass = Class.forName(RESPAWN_PACKET_CLASS, true, classLoader);
        Class<?> compoundBinaryTagClass = Class.forName(COMPOUND_BINARY_TAG_CLASS, true, classLoader);
        Class<?> pairClass = Class.forName(FASTUTIL_PAIR_CLASS, true, classLoader);

        Constructor<?> respawnConstructor = respawnPacketClass.getConstructor(
                int.class,
                long.class,
                short.class,
                short.class,
                String.class,
                byte.class,
                dimensionInfoClass,
                short.class,
                compoundBinaryTagClass,
                pairClass,
                int.class,
                int.class
        );

        return respawnConstructor.newInstance(
                dimensionId,
                LIMBO_SEED_HASH,
                (short) 0,
                GAMEMODE_SPECTATOR,
                "default",
                RESPAWN_KEEP_NOTHING,
                dimensionInfo,
                PREVIOUS_GAMEMODE_UNKNOWN,
                null,
                null,
                0,
                LIMBO_SEA_LEVEL
        );
    }

    private void sendStartWaitingForLevelChunksGameEvent(
            VirtualServer virtualServer,
            Player player,
            Object connection
    ) throws ReflectiveOperationException {
        ClassLoader classLoader = getClass().getClassLoader();
        Class<?> unpooledClass = Class.forName(UNPOOLED_CLASS, true, classLoader);
        Object byteBuf = unpooledClass.getMethod("buffer").invoke(null);

        Class<?> byteBufClass = Class.forName(BYTE_BUF_CLASS, true, classLoader);
        Class<?> protocolUtilsClass = Class.forName(PROTOCOL_UTILS_CLASS, true, classLoader);

        int packetId = virtualServer.getPacketVersion(VirtualPacketKeys.GAME_EVENT, player.getProtocolVersion().getProtocol())
                .map(VirtualServer.PacketVersionRule::packetVersion)
                .orElse(GAME_EVENT_PACKET_ID_1_21_4);

        protocolUtilsClass.getMethod("writeVarInt", byteBufClass, int.class)
                .invoke(null, byteBuf, packetId);
        byteBufClass.getMethod("writeByte", int.class)
                .invoke(byteBuf, GAME_EVENT_START_WAITING_FOR_LEVEL_CHUNKS);
        byteBufClass.getMethod("writeFloat", float.class)
                .invoke(byteBuf, 0.0F);

        connection.getClass().getMethod("write", Object.class).invoke(connection, byteBuf);
    }

    private void sendSynchronizePlayerPosition(
            VirtualServer virtualServer,
            Player player,
            Object connection
    ) throws ReflectiveOperationException {
        ClassLoader classLoader = getClass().getClassLoader();
        Class<?> unpooledClass = Class.forName(UNPOOLED_CLASS, true, classLoader);
        Object byteBuf = unpooledClass.getMethod("buffer").invoke(null);

        Class<?> byteBufClass = Class.forName(BYTE_BUF_CLASS, true, classLoader);
        Class<?> protocolUtilsClass = Class.forName(PROTOCOL_UTILS_CLASS, true, classLoader);

        int protocol = player.getProtocolVersion().getProtocol();
        int packetId = virtualServer.getPacketVersion(VirtualPacketKeys.PLAYER_POSITION, protocol)
                .map(VirtualServer.PacketVersionRule::packetVersion)
                .orElse(PLAYER_POSITION_PACKET_ID_1_21_4);
        int teleportId = nextTeleportId();

        protocolUtilsClass.getMethod("writeVarInt", byteBufClass, int.class)
                .invoke(null, byteBuf, packetId);
        protocolUtilsClass.getMethod("writeVarInt", byteBufClass, int.class)
                .invoke(null, byteBuf, teleportId);

        byteBufClass.getMethod("writeDouble", double.class).invoke(byteBuf, LIMBO_X);
        byteBufClass.getMethod("writeDouble", double.class).invoke(byteBuf, LIMBO_Y);
        byteBufClass.getMethod("writeDouble", double.class).invoke(byteBuf, LIMBO_Z);
        byteBufClass.getMethod("writeDouble", double.class).invoke(byteBuf, 0.0D); // velocity X
        byteBufClass.getMethod("writeDouble", double.class).invoke(byteBuf, 0.0D); // velocity Y
        byteBufClass.getMethod("writeDouble", double.class).invoke(byteBuf, 0.0D); // velocity Z
        byteBufClass.getMethod("writeFloat", float.class).invoke(byteBuf, LIMBO_YAW);
        byteBufClass.getMethod("writeFloat", float.class).invoke(byteBuf, LIMBO_PITCH);
        byteBufClass.getMethod("writeInt", int.class).invoke(byteBuf, TELEPORT_FLAGS_ABSOLUTE);

        connection.getClass().getMethod("write", Object.class).invoke(connection, byteBuf);
    }

    private int nextTeleportId() {
        int id = teleportIdSequence.getAndIncrement();
        if (id > 0) {
            return id;
        }
        teleportIdSequence.set(2);
        return 1;
    }
}
