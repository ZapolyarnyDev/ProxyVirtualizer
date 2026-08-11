package io.github.zapolyarnydev.proxyvirtualizer.protocol.registry;

public final class ProtocolRegistry {

  private final ProtocolProfileRegistry profiles = new ProtocolProfileRegistry();
  private final PacketCodecRegistry codecs = new PacketCodecRegistry(profiles);
  private final PacketActionMapperRegistry actionMappers = new PacketActionMapperRegistry(profiles);

  public ProtocolProfileRegistry profiles() {
    return profiles;
  }

  public PacketCodecRegistry codecs() {
    return codecs;
  }

  public PacketActionMapperRegistry actionMappers() {
    return actionMappers;
  }
}
