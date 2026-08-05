package io.github.zapolyarnydev.proxyvirtualizer.protocol.registry;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.ProtocolCapability;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.ProtocolProfile;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.ProtocolVersion;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.exception.DuplicateProtocolVersionException;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.exception.UnsupportedProtocolVersionException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class ProtocolProfileRegistry {

  private final Map<ProtocolVersion, ProtocolProfile> profiles = new HashMap<>();

  ProtocolProfileRegistry() {}

  public void register(ProtocolProfile profile) {
    Objects.requireNonNull(profile, "profile");
    ProtocolVersion version = profile.version();
    if (profiles.putIfAbsent(version, profile) != null) {
      throw new DuplicateProtocolVersionException(
          "Protocol version is already registered: " + version);
    }
  }

  public ProtocolProfile require(ProtocolVersion version) {
    Objects.requireNonNull(version, "version");
    ProtocolProfile profile = profiles.get(version);
    if (profile == null) {
      throw new UnsupportedProtocolVersionException("Unsupported protocol version: " + version);
    }
    return profile;
  }

  public boolean supports(ProtocolVersion version) {
    Objects.requireNonNull(version, "version");
    return profiles.containsKey(version);
  }

  public boolean supports(ProtocolVersion version, ProtocolCapability capability) {
    Objects.requireNonNull(capability, "capability");
    return require(version).supports(capability);
  }
}
