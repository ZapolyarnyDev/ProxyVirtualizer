package io.github.zapolyarnydev.proxyvirtualizer.plugin.signal;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.zapolyarnydev.proxyvirtualizer.api.connector.ConnectionStorage;
import io.github.zapolyarnydev.proxyvirtualizer.api.signal.SignalBus;
import io.github.zapolyarnydev.proxyvirtualizer.api.signal.player.PlayerChatPayload;
import io.github.zapolyarnydev.proxyvirtualizer.api.signal.player.PlayerChatSignal;
import io.github.zapolyarnydev.proxyvirtualizer.api.signal.player.PlayerCommandPayload;
import io.github.zapolyarnydev.proxyvirtualizer.api.signal.player.PlayerCommandSignal;
import io.github.zapolyarnydev.proxyvirtualizer.api.signal.player.PlayerLookPayload;
import io.github.zapolyarnydev.proxyvirtualizer.api.signal.player.PlayerLookSignal;
import io.github.zapolyarnydev.proxyvirtualizer.api.signal.player.PlayerMovePayload;
import io.github.zapolyarnydev.proxyvirtualizer.api.signal.player.PlayerMoveSignal;
import io.github.zapolyarnydev.proxyvirtualizer.api.signal.player.PlayerPacketSignalKind;
import org.slf4j.Logger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VelocitySignalBridge {
    private static final String PIPELINE_HANDLER_MINECRAFT_DECODER = "minecraft-decoder";
    private static final String PIPELINE_HANDLER_MAIN = "handler";
    private static final String SIGNAL_TAP_PREFIX = "proxyvirtualizer-signal-tap-";

    private static final int PACKET_ID_SET_PLAYER_POSITION_1_21_4 = 0x1C;
    private static final int PACKET_ID_SET_PLAYER_POSITION_AND_ROTATION_1_21_4 = 0x1D;
    private static final int PACKET_ID_SET_PLAYER_ROTATION_1_21_4 = 0x1E;

    private final ProxyServer proxyServer;
    private final ConnectionStorage connectionStorage;
    private final SignalBus signalBus;
    private final Logger logger;
    private final Map<UUID, TapRegistration> installedTapNames = new ConcurrentHashMap<>();

    public VelocitySignalBridge(
            ProxyServer proxyServer,
            ConnectionStorage connectionStorage,
            SignalBus signalBus,
            Logger logger
    ) {
        this.proxyServer = Objects.requireNonNull(proxyServer, "proxyServer");
        this.connectionStorage = Objects.requireNonNull(connectionStorage, "connectionStorage");
        this.signalBus = Objects.requireNonNull(signalBus, "signalBus");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        installPacketTap(event.getPlayer());
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        uninstallPacketTap(event.getPlayer());
    }

    @Subscribe
    public void onChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!connectionStorage.isInVirtualServer(player)) {
            return;
        }
        ensurePacketTapInstalled(player);
        if (hasInstalledPacketTap(player)) {
            return;
        }
        signalBus.publish(new PlayerChatSignal(player, new PlayerChatPayload(event.getMessage())));
    }

    @Subscribe
    public void onCommand(CommandExecuteEvent event) {
        if (!(event.getCommandSource() instanceof Player player)) {
            return;
        }
        if (!connectionStorage.isInVirtualServer(player)) {
            return;
        }
        ensurePacketTapInstalled(player);
        if (hasInstalledPacketTap(player)) {
            return;
        }

        String rawCommand = event.getCommand();
        CommandParts parts = splitCommand(rawCommand);
        PlayerCommandPayload payload = new PlayerCommandPayload(
                rawCommand,
                parts.label(),
                List.copyOf(parts.arguments()),
                event.getInvocationInfo().source().name(),
                event.getInvocationInfo().signedState().name()
        );
        signalBus.publish(new PlayerCommandSignal(player, payload));
    }

    public void shutdown() {
        for (Player player : proxyServer.getAllPlayers()) {
            uninstallPacketTap(player);
        }
        installedTapNames.clear();
    }

    private boolean hasInstalledPacketTap(Player player) {
        TapRegistration registration = installedTapNames.get(player.getUniqueId());
        return registration != null && (registration.rawInstalled() || registration.decodedInstalled());
    }

    private void ensurePacketTapInstalled(Player player) {
        if (hasInstalledPacketTap(player)) {
            return;
        }
        installPacketTap(player);
    }

    private boolean hasRawPacketTap(Player player) {
        TapRegistration registration = installedTapNames.get(player.getUniqueId());
        return registration != null && registration.rawInstalled();
    }

    private void installPacketTap(Player player) {
        Objects.requireNonNull(player, "player");

        UUID playerId = player.getUniqueId();
        TapRegistration tapRegistration = new TapRegistration(
                SIGNAL_TAP_PREFIX + playerId + "-raw",
                SIGNAL_TAP_PREFIX + playerId + "-decoded",
                false,
                false
        );
        TapRegistration existing = installedTapNames.putIfAbsent(playerId, tapRegistration);
        if (existing != null) {
            return;
        }

        try {
            Object pipeline = resolvePipeline(player);
            if (pipeline == null) {
                installedTapNames.remove(playerId, tapRegistration);
                return;
            }

            boolean rawInstalled = installRawPacketTap(player, pipeline, tapRegistration.rawTapName());
            boolean decodedInstalled = installDecodedPacketTap(player, pipeline, tapRegistration.decodedTapName());
            installedTapNames.put(playerId, tapRegistration.withInstalled(rawInstalled, decodedInstalled));
            if (!rawInstalled && !decodedInstalled) {
                installedTapNames.remove(playerId);
                logger.debug("Failed to install signal packet taps for player {}", player.getUsername());
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            installedTapNames.remove(playerId);
            logger.debug("Unable to install signal packet taps for player {}", player.getUsername(), exception);
        }
    }

    private void uninstallPacketTap(Player player) {
        Objects.requireNonNull(player, "player");
        TapRegistration tapRegistration = installedTapNames.remove(player.getUniqueId());
        if (tapRegistration == null) {
            return;
        }

        try {
            Object pipeline = resolvePipeline(player);
            if (pipeline == null) {
                return;
            }
            removePipelineHandler(pipeline, tapRegistration.rawTapName());
            removePipelineHandler(pipeline, tapRegistration.decodedTapName());
        } catch (ReflectiveOperationException | RuntimeException exception) {
            logger.debug("Unable to remove signal packet taps for player {}", player.getUsername(), exception);
        }
    }

    private boolean installRawPacketTap(Player player, Object pipeline, String tapName) throws ReflectiveOperationException {
        if (getPipelineHandler(pipeline, tapName) != null) {
            return true;
        }
        Object handler = createInboundTapHandler(player, this::inspectRawInboundMessage);
        if (addBefore(pipeline, PIPELINE_HANDLER_MINECRAFT_DECODER, tapName, handler)) {
            return true;
        }
        logger.debug("Failed to install raw signal packet tap for player {}", player.getUsername());
        return false;
    }

    private boolean installDecodedPacketTap(Player player, Object pipeline, String tapName) throws ReflectiveOperationException {
        if (getPipelineHandler(pipeline, tapName) != null) {
            return true;
        }
        Object handler = createInboundTapHandler(player, this::inspectDecodedInboundMessage);
        if (addAfter(pipeline, PIPELINE_HANDLER_MINECRAFT_DECODER, tapName, handler)
                || addBefore(pipeline, PIPELINE_HANDLER_MAIN, tapName, handler)
                || addLast(pipeline, tapName, handler)) {
            return true;
        }
        logger.debug("Failed to install decoded signal packet tap for player {}", player.getUsername());
        return false;
    }

    private Object createInboundTapHandler(Player player, InboundInspector inspector) throws ClassNotFoundException {
        ClassLoader classLoader = player.getClass().getClassLoader();
        Class<?> channelInboundHandlerClass = Class.forName("io.netty.channel.ChannelInboundHandler", true, classLoader);

        InvocationHandler invocationHandler = (proxy, method, args) -> {
            String methodName = method.getName();
            if (args == null) {
                return handleObjectMethod(proxy, methodName, method, new Object[0], player);
            }

            if (method.getDeclaringClass() == Object.class) {
                return handleObjectMethod(proxy, methodName, method, args, player);
            }

            try {
                return switch (methodName) {
                    case "channelRead" -> {
                        inspector.inspect(player, args[1]);
                        invokeContext(args[0], "fireChannelRead", new Class<?>[]{Object.class}, new Object[]{args[1]});
                        yield null;
                    }
                    case "channelRegistered" -> forwardInboundEvent(args[0], "fireChannelRegistered");
                    case "channelUnregistered" -> forwardInboundEvent(args[0], "fireChannelUnregistered");
                    case "channelActive" -> forwardInboundEvent(args[0], "fireChannelActive");
                    case "channelInactive" -> forwardInboundEvent(args[0], "fireChannelInactive");
                    case "channelReadComplete" -> forwardInboundEvent(args[0], "fireChannelReadComplete");
                    case "userEventTriggered" -> {
                        invokeContext(args[0], "fireUserEventTriggered", new Class<?>[]{Object.class}, new Object[]{args[1]});
                        yield null;
                    }
                    case "channelWritabilityChanged" -> forwardInboundEvent(args[0], "fireChannelWritabilityChanged");
                    case "exceptionCaught" -> {
                        invokeContext(args[0], "fireExceptionCaught", new Class<?>[]{Throwable.class}, new Object[]{args[1]});
                        yield null;
                    }
                    case "handlerAdded", "handlerRemoved" -> null;
                    default -> defaultReturnValue(method.getReturnType());
                };
            } catch (InvocationTargetException exception) {
                throw exception.getCause();
            }
        };

        return java.lang.reflect.Proxy.newProxyInstance(
                classLoader,
                new Class<?>[]{channelInboundHandlerClass},
                invocationHandler
        );
    }

    private Object handleObjectMethod(
            Object proxy,
            String methodName,
            Method method,
            Object[] args,
            Player player
    ) {
        return switch (methodName) {
            case "toString" -> "ProxyVirtualizerSignalTap[" + player.getUsername() + "]";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> defaultReturnValue(method.getReturnType());
        };
    }

    private Object forwardInboundEvent(Object context, String fireMethodName)
            throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        invokeContext(context, fireMethodName, new Class<?>[0], new Object[0]);
        return null;
    }

    private void inspectInboundMessage(Player player, Object message) {
        inspectDecodedInboundMessage(player, message);
        inspectRawInboundMessage(player, message);
    }

    private void inspectDecodedInboundMessage(Player player, Object message) {
        if (!connectionStorage.isInVirtualServer(player)) {
            return;
        }
        if (message == null) {
            return;
        }

        inspectDecodedChatOrCommandPacket(player, message);
        inspectDecodedMovementPacket(player, message);
        if (!hasRawPacketTap(player) && isByteBuf(message)) {
            inspectRawInboundMessage(player, message);
        }
    }

    private void inspectRawInboundMessage(Player player, Object message) {
        if (!connectionStorage.isInVirtualServer(player)) {
            return;
        }
        if (message == null) {
            return;
        }
        if (!ProtocolVersion.MINECRAFT_1_21_4.equals(player.getProtocolVersion())) {
            return;
        }
        if (!isByteBuf(message)) {
            return;
        }

        try {
            Object copy = duplicateByteBuf(message);
            int packetId = readVarInt(copy);
            switch (packetId) {
                case PACKET_ID_SET_PLAYER_POSITION_1_21_4 -> publishPosition(player, copy, PlayerPacketSignalKind.POSITION);
                case PACKET_ID_SET_PLAYER_POSITION_AND_ROTATION_1_21_4 -> publishPositionAndRotation(player, copy);
                case PACKET_ID_SET_PLAYER_ROTATION_1_21_4 -> publishRotation(player, copy, PlayerPacketSignalKind.ROTATION);
                default -> {
                }
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            logger.debug("Failed to inspect inbound packet for player {}", player.getUsername(), exception);
        }
    }

    private boolean inspectDecodedChatOrCommandPacket(Player player, Object message) {
        String className = message.getClass().getName();

        try {
            if (className.endsWith("SessionPlayerChatPacket") || className.endsWith("KeyedPlayerChatPacket")) {
                String chatMessage = tryInvokeStringGetter(message, "getMessage");
                if (chatMessage != null) {
                    signalBus.publish(new PlayerChatSignal(player, new PlayerChatPayload(chatMessage)));
                    return true;
                }
            }

            if (className.endsWith("LegacyChatPacket")) {
                String legacyMessage = tryInvokeStringGetter(message, "getMessage");
                if (legacyMessage != null) {
                    if (legacyMessage.startsWith("/")) {
                        publishCommandFromRaw(player, legacyMessage.substring(1), "PLAYER", "UNSUPPORTED");
                    } else {
                        signalBus.publish(new PlayerChatSignal(player, new PlayerChatPayload(legacyMessage)));
                    }
                    return true;
                }
            }

            if (className.endsWith("SessionPlayerCommandPacket")
                    || className.endsWith("UnsignedPlayerCommandPacket")
                    || className.endsWith("KeyedPlayerCommandPacket")) {
                String command = tryInvokeStringGetter(message, "getCommand");
                if (command != null) {
                    String signedState = resolveSignedStateName(message);
                    publishCommandFromRaw(player, command, "PLAYER", signedState);
                    return true;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            logger.debug("Failed to inspect decoded chat/command packet for player {}", player.getUsername(), exception);
        }

        return false;
    }

    private boolean inspectDecodedMovementPacket(Player player, Object message) {
        String className = message.getClass().getName();
        if (!className.contains("MovePlayerPacket")) {
            return false;
        }

        try {
            Double x = tryInvokeDoubleGetter(message, "getX");
            Double y = tryInvokeDoubleGetter(message, "getY");
            Double z = tryInvokeDoubleGetter(message, "getZ");
            Float yaw = firstNonNull(
                    tryInvokeFloatGetter(message, "getYaw"),
                    tryInvokeFloatGetter(message, "getYRot")
            );
            Float pitch = firstNonNull(
                    tryInvokeFloatGetter(message, "getPitch"),
                    tryInvokeFloatGetter(message, "getXRot")
            );
            int flags = resolveMovementFlags(message);

            boolean hasPosition = x != null && y != null && z != null;
            boolean hasRotation = yaw != null && pitch != null;
            if (!hasPosition && !hasRotation) {
                return false;
            }

            if (hasPosition && hasRotation) {
                signalBus.publish(new PlayerMoveSignal(player, new PlayerMovePayload(
                        x, y, z,
                        isOnGround(flags),
                        hasHorizontalCollision(flags),
                        PlayerPacketSignalKind.POSITION_AND_ROTATION
                )));
                signalBus.publish(new PlayerLookSignal(player, new PlayerLookPayload(
                        yaw, pitch,
                        isOnGround(flags),
                        hasHorizontalCollision(flags),
                        PlayerPacketSignalKind.POSITION_AND_ROTATION
                )));
                return true;
            }

            if (hasPosition) {
                signalBus.publish(new PlayerMoveSignal(player, new PlayerMovePayload(
                        x, y, z,
                        isOnGround(flags),
                        hasHorizontalCollision(flags),
                        PlayerPacketSignalKind.POSITION
                )));
                return true;
            }

            signalBus.publish(new PlayerLookSignal(player, new PlayerLookPayload(
                    yaw, pitch,
                    isOnGround(flags),
                    hasHorizontalCollision(flags),
                    PlayerPacketSignalKind.ROTATION
            )));
            return true;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            logger.debug("Failed to inspect decoded movement packet for player {}", player.getUsername(), exception);
            return false;
        }
    }

    private void publishCommandFromRaw(Player player, String rawCommand, String invocationSource, String signedState) {
        CommandParts parts = splitCommand(rawCommand);
        signalBus.publish(new PlayerCommandSignal(player, new PlayerCommandPayload(
                rawCommand,
                parts.label(),
                List.copyOf(parts.arguments()),
                invocationSource,
                signedState
        )));
    }

    private void publishPosition(Player player, Object byteBuf, PlayerPacketSignalKind kind)
            throws ReflectiveOperationException {
        double x = readDouble(byteBuf);
        double y = readDouble(byteBuf);
        double z = readDouble(byteBuf);
        int flags = readUnsignedByte(byteBuf);

        signalBus.publish(new PlayerMoveSignal(player, new PlayerMovePayload(
                x,
                y,
                z,
                isOnGround(flags),
                hasHorizontalCollision(flags),
                kind
        )));
    }

    private void publishPositionAndRotation(Player player, Object byteBuf)
            throws ReflectiveOperationException {
        double x = readDouble(byteBuf);
        double y = readDouble(byteBuf);
        double z = readDouble(byteBuf);
        float yaw = readFloat(byteBuf);
        float pitch = readFloat(byteBuf);
        int flags = readUnsignedByte(byteBuf);

        signalBus.publish(new PlayerMoveSignal(player, new PlayerMovePayload(
                x,
                y,
                z,
                isOnGround(flags),
                hasHorizontalCollision(flags),
                PlayerPacketSignalKind.POSITION_AND_ROTATION
        )));
        signalBus.publish(new PlayerLookSignal(player, new PlayerLookPayload(
                yaw,
                pitch,
                isOnGround(flags),
                hasHorizontalCollision(flags),
                PlayerPacketSignalKind.POSITION_AND_ROTATION
        )));
    }

    private void publishRotation(Player player, Object byteBuf, PlayerPacketSignalKind kind)
            throws ReflectiveOperationException {
        float yaw = readFloat(byteBuf);
        float pitch = readFloat(byteBuf);
        int flags = readUnsignedByte(byteBuf);

        signalBus.publish(new PlayerLookSignal(player, new PlayerLookPayload(
                yaw,
                pitch,
                isOnGround(flags),
                hasHorizontalCollision(flags),
                kind
        )));
    }

    private static boolean isOnGround(int flags) {
        return (flags & 0x01) != 0;
    }

    private static boolean hasHorizontalCollision(int flags) {
        return (flags & 0x02) != 0;
    }

    private static boolean isByteBuf(Object value) {
        String className = value.getClass().getName();
        return "io.netty.buffer.ByteBuf".equals(className) || inheritsFrom(value.getClass(), "io.netty.buffer.ByteBuf");
    }

    private static String tryInvokeStringGetter(Object target, String methodName) throws ReflectiveOperationException {
        Object result = target.getClass().getMethod(methodName).invoke(target);
        return result instanceof String value ? value : null;
    }

    private static Double tryInvokeDoubleGetter(Object target, String methodName) throws ReflectiveOperationException {
        try {
            Object result = target.getClass().getMethod(methodName).invoke(target);
            return result instanceof Number number ? number.doubleValue() : null;
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Float tryInvokeFloatGetter(Object target, String methodName) throws ReflectiveOperationException {
        try {
            Object result = target.getClass().getMethod(methodName).invoke(target);
            return result instanceof Number number ? number.floatValue() : null;
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static int resolveMovementFlags(Object packet) throws ReflectiveOperationException {
        try {
            Object flags = packet.getClass().getMethod("getFlags").invoke(packet);
            Integer resolved = coerceFlags(flags);
            if (resolved != null) {
                return resolved;
            }
        } catch (NoSuchMethodException ignored) {
        }

        int flags = 0;
        Boolean onGround = tryInvokeBooleanGetter(packet, "isOnGround");
        Boolean horizontalCollision = tryInvokeBooleanGetter(packet, "hasHorizontalCollision");
        if (horizontalCollision == null) {
            horizontalCollision = tryInvokeBooleanGetter(packet, "isHorizontalCollision");
        }
        if (onGround != null && onGround) {
            flags |= 0x01;
        }
        if (horizontalCollision != null && horizontalCollision) {
            flags |= 0x02;
        }
        return flags;
    }

    private static Boolean tryInvokeBooleanGetter(Object target, String methodName) throws ReflectiveOperationException {
        try {
            Object result = target.getClass().getMethod(methodName).invoke(target);
            return result instanceof Boolean value ? value : null;
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Integer coerceFlags(Object flags) throws ReflectiveOperationException {
        if (flags == null) {
            return null;
        }
        if (flags instanceof Number number) {
            return number.intValue();
        }
        for (String getter : List.of("getBits", "bits", "getMask", "mask", "value", "getValue")) {
            try {
                Object result = flags.getClass().getMethod(getter).invoke(flags);
                if (result instanceof Number number) {
                    return number.intValue();
                }
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    private static String resolveSignedStateName(Object packet) {
        try {
            Object state = packet.getClass().getMethod("getEventSignedState").invoke(packet);
            if (state instanceof Enum<?> enumValue) {
                return enumValue.name();
            }
        } catch (ReflectiveOperationException ignored) {
        }

        try {
            Object signed = packet.getClass().getMethod("isSigned").invoke(packet);
            if (signed instanceof Boolean signedBoolean) {
                return signedBoolean ? "SIGNED_WITHOUT_ARGS" : "UNSIGNED";
            }
        } catch (ReflectiveOperationException ignored) {
        }

        return "UNSUPPORTED";
    }

    private static boolean inheritsFrom(Class<?> type, String fqcn) {
        Class<?> current = type;
        while (current != null) {
            if (fqcn.equals(current.getName())) {
                return true;
            }
            for (Class<?> iface : current.getInterfaces()) {
                if (inheritsFrom(iface, fqcn)) {
                    return true;
                }
            }
            current = current.getSuperclass();
        }
        return false;
    }

    private static int readVarInt(Object byteBuf) throws ReflectiveOperationException {
        int numRead = 0;
        int result = 0;
        byte read;
        do {
            Number readValue = (Number) invokeByteBufMethod(byteBuf, "readByte", new Class<?>[0], new Object[0]);
            read = readValue.byteValue();
            int value = read & 0b0111_1111;
            result |= (value << (7 * numRead));
            numRead++;
            if (numRead > 5) {
                throw new IllegalArgumentException("VarInt is too big");
            }
        } while ((read & 0b1000_0000) != 0);
        return result;
    }

    private static double readDouble(Object byteBuf) throws ReflectiveOperationException {
        return (double) invokeByteBufMethod(byteBuf, "readDouble", new Class<?>[0], new Object[0]);
    }

    private static float readFloat(Object byteBuf) throws ReflectiveOperationException {
        return (float) invokeByteBufMethod(byteBuf, "readFloat", new Class<?>[0], new Object[0]);
    }

    private static int readUnsignedByte(Object byteBuf) throws ReflectiveOperationException {
        Number value = (Number) invokeByteBufMethod(byteBuf, "readUnsignedByte", new Class<?>[0], new Object[0]);
        return value.intValue();
    }

    private static Object duplicateByteBuf(Object byteBuf) throws ReflectiveOperationException {
        return invokeByteBufMethod(byteBuf, "duplicate", new Class<?>[0], new Object[0]);
    }

    private static Object invokeByteBufMethod(Object byteBuf, String methodName, Class<?>[] parameterTypes, Object[] args)
            throws ReflectiveOperationException {
        ClassLoader classLoader = byteBuf.getClass().getClassLoader();
        try {
            Class<?> byteBufClass = Class.forName("io.netty.buffer.ByteBuf", true, classLoader);
            return byteBufClass.getMethod(methodName, parameterTypes).invoke(byteBuf, args);
        } catch (ClassNotFoundException exception) {
            throw new NoSuchMethodException("io.netty.buffer.ByteBuf not found");
        }
    }

    private static Object resolvePipeline(Player player) throws ReflectiveOperationException {
        Object connection = player.getClass().getMethod("getConnection").invoke(player);
        if (connection == null) {
            return null;
        }
        Object channel = connection.getClass().getMethod("getChannel").invoke(connection);
        if (channel == null) {
            return null;
        }
        return channel.getClass().getMethod("pipeline").invoke(channel);
    }

    private static Object getPipelineHandler(Object pipeline, String name) throws ReflectiveOperationException {
        return pipeline.getClass().getMethod("get", String.class).invoke(pipeline, name);
    }

    private static boolean addAfter(Object pipeline, String baseName, String name, Object handler)
            throws ReflectiveOperationException {
        try {
            invokePipelineMutation(pipeline, "addAfter", baseName, name, handler);
            return true;
        } catch (InvocationTargetException exception) {
            return false;
        }
    }

    private static boolean addBefore(Object pipeline, String baseName, String name, Object handler)
            throws ReflectiveOperationException {
        try {
            invokePipelineMutation(pipeline, "addBefore", baseName, name, handler);
            return true;
        } catch (InvocationTargetException exception) {
            return false;
        }
    }

    private static boolean addLast(Object pipeline, String name, Object handler) throws ReflectiveOperationException {
        try {
            Class<?> channelHandlerClass = Class.forName("io.netty.channel.ChannelHandler", true, handler.getClass().getClassLoader());
            pipeline.getClass()
                    .getMethod("addLast", String.class, channelHandlerClass)
                    .invoke(pipeline, name, handler);
            return true;
        } catch (InvocationTargetException exception) {
            return false;
        }
    }

    private static void removePipelineHandler(Object pipeline, String name) throws ReflectiveOperationException {
        try {
            pipeline.getClass().getMethod("remove", String.class).invoke(pipeline, name);
        } catch (InvocationTargetException ignored) {

        }
    }

    private static void invokePipelineMutation(Object pipeline, String methodName, String baseName, String name, Object handler)
            throws ReflectiveOperationException {
        Class<?> channelHandlerClass = Class.forName("io.netty.channel.ChannelHandler", true, handler.getClass().getClassLoader());
        pipeline.getClass()
                .getMethod(methodName, String.class, String.class, channelHandlerClass)
                .invoke(pipeline, baseName, name, handler);
    }

    private static Object invokeContext(Object context, String methodName, Class<?>[] parameterTypes, Object[] args)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ClassLoader classLoader = context.getClass().getClassLoader();
        try {
            Class<?> channelHandlerContextClass = Class.forName("io.netty.channel.ChannelHandlerContext", true, classLoader);
            return channelHandlerContextClass.getMethod(methodName, parameterTypes).invoke(context, args);
        } catch (ClassNotFoundException exception) {
            throw new NoSuchMethodException("io.netty.channel.ChannelHandlerContext not found");
        }
    }

    private static Object defaultReturnValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == float.class) {
            return 0F;
        }
        if (returnType == double.class) {
            return 0D;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return null;
    }

    private static CommandParts splitCommand(String rawCommand) {
        if (rawCommand == null || rawCommand.isBlank()) {
            return new CommandParts("", List.of());
        }

        String normalized = rawCommand.stripLeading();
        if (normalized.isEmpty()) {
            return new CommandParts("", List.of());
        }

        String[] parts = normalized.split("\\s+");
        String label = parts.length == 0 ? "" : parts[0];
        if (parts.length <= 1) {
            return new CommandParts(label, List.of());
        }

        List<String> arguments = new ArrayList<>(parts.length - 1);
        for (int i = 1; i < parts.length; i++) {
            arguments.add(parts[i]);
        }
        return new CommandParts(label, arguments);
    }

    private record CommandParts(String label, List<String> arguments) {
    }

    private static <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }

    private record TapRegistration(String rawTapName, String decodedTapName, boolean rawInstalled, boolean decodedInstalled) {
        private TapRegistration withInstalled(boolean rawInstalled, boolean decodedInstalled) {
            return new TapRegistration(rawTapName, decodedTapName, rawInstalled, decodedInstalled);
        }
    }

    @FunctionalInterface
    private interface InboundInspector {
        void inspect(Player player, Object message);
    }
}
