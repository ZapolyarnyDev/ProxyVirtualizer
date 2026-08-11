package io.github.zapolyarnydev.proxyvirtualizer.protocol.engine;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.action.PacketActionMapper;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.action.SemanticAction;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.action.SemanticActionSink;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.ProtocolContext;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.packet.ServerboundPacket;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolProfile;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.InboundFrame;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.engine.exception.InvalidProtocolContextException;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.engine.exception.PacketActionMappingException;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.engine.exception.SemanticActionHandlingException;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.engine.exception.UnknownPacketActionMapperException;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.registry.ProtocolRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public final class ProtocolSessionProcessor {

  private final ProtocolRegistry registry;
  private final InboundPacketDecoder decoder;

  public ProtocolSessionProcessor(@NotNull ProtocolRegistry registry) {
    this.registry = Objects.requireNonNull(registry, "registry");
    decoder = new InboundPacketDecoder(registry);
  }

  public void process(
      @NotNull ProtocolContext context,
      @NotNull InboundFrame frame,
      @NotNull SemanticActionSink actionSink) {
    Objects.requireNonNull(context, "context");
    Objects.requireNonNull(frame, "frame");
    Objects.requireNonNull(actionSink, "actionSink");
    validateContext(context);

    ServerboundPacket packet = decoder.decode(context.version(), context.phase(), frame);
    PacketActionMapper<ServerboundPacket> mapper = requireMapper(context, packet);
    List<SemanticAction> actions = map(context, packet, mapper);
    dispatch(context, packet, actions, actionSink);
  }

  private void validateContext(ProtocolContext context) {
    ProtocolProfile registered = registry.profiles().require(context.version(), context.phase());
    if (!registered.id().equals(context.profile().id())) {
      throw new InvalidProtocolContextException(
          "Protocol context profile "
              + context.profile().id()
              + " does not match registered profile "
              + registered.id()
              + " for protocol "
              + context.version().number());
    }
  }

  private PacketActionMapper<ServerboundPacket> requireMapper(
      ProtocolContext context, ServerboundPacket packet) {
    return registry
        .actionMappers()
        .find(context.version(), context.phase(), serverboundPacketType(packet))
        .orElseThrow(
            () ->
                new UnknownPacketActionMapperException(
                    "No packet action mapper for protocol "
                        + context.version().number()
                        + ", phase "
                        + context.phase()
                        + ", packet "
                        + packet.getClass().getName()));
  }

  @SuppressWarnings("unchecked")
  private static Class<ServerboundPacket> serverboundPacketType(ServerboundPacket packet) {
    return (Class<ServerboundPacket>) packet.getClass();
  }

  private static List<SemanticAction> map(
      ProtocolContext context,
      ServerboundPacket packet,
      PacketActionMapper<ServerboundPacket> mapper) {
    List<SemanticAction> actions = new ArrayList<>();
    try {
      mapper.map(
          context,
          packet,
          action -> actions.add(Objects.requireNonNull(action, "mapper published action")));
    } catch (RuntimeException cause) {
      throw new PacketActionMappingException(
          "Could not map packet " + packet.getClass().getName() + " to semantic actions", cause);
    }
    return List.copyOf(actions);
  }

  private static void dispatch(
      ProtocolContext context,
      ServerboundPacket packet,
      List<SemanticAction> actions,
      SemanticActionSink actionSink) {
    for (SemanticAction action : actions) {
      try {
        actionSink.accept(action);
      } catch (RuntimeException cause) {
        throw new SemanticActionHandlingException(
            "Could not handle semantic action "
                + action.getClass().getName()
                + " produced by packet "
                + packet.getClass().getName()
                + " for protocol "
                + context.version().number(),
            cause);
      }
    }
  }
}
