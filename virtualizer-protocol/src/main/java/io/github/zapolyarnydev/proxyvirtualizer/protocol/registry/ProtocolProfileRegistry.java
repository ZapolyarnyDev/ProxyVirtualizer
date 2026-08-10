package io.github.zapolyarnydev.proxyvirtualizer.protocol.registry;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolCapability;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolPhase;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolProfile;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolProfileId;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolVersion;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.registry.exception.DuplicateProtocolProfileException;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.registry.exception.DuplicateProtocolVersionException;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.registry.exception.UnknownProtocolProfileException;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.registry.exception.UnsupportedProtocolPhaseException;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.registry.exception.UnsupportedProtocolVersionException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class ProtocolProfileRegistry {

  private final Map<ProtocolProfileId, ProtocolProfile> profilesById = new HashMap<>();

  ProtocolProfileRegistry() {}

  public void register(ProtocolProfile profile) {
    Objects.requireNonNull(profile, "profile");
    if (profilesById.containsKey(profile.id()))
      throw new DuplicateProtocolProfileException(profile.id());

    profilesById.values().stream()
        .filter(existing -> existing.versions().stream().anyMatch(profile.versions()::contains))
        .findFirst()
        .ifPresent(
            existing -> {
              throw new DuplicateProtocolVersionException(existing, profile);
            });
    profilesById.put(profile.id(), profile);
  }

  public ProtocolProfile require(ProtocolProfileId profileId) {
    Objects.requireNonNull(profileId, "profileId");
    ProtocolProfile profile = profilesById.get(profileId);
    if (profile == null) throw new UnknownProtocolProfileException(profileId);

    return profile;
  }

  public ProtocolProfile require(ProtocolProfileId profileId, ProtocolPhase phase) {
    Objects.requireNonNull(phase, "phase");
    ProtocolProfile profile = require(profileId);
    requirePhase(profile, phase);
    return profile;
  }

  public ProtocolProfile require(ProtocolVersion version) {
    Objects.requireNonNull(version, "version");
    return profilesById.values().stream()
        .filter(profile -> profile.supports(version))
        .findFirst()
        .orElseThrow(
            () ->
                new UnsupportedProtocolVersionException(
                    "Unsupported protocol version: " + version));
  }

  public ProtocolProfile require(ProtocolVersion version, ProtocolPhase phase) {
    Objects.requireNonNull(phase, "phase");
    ProtocolProfile profile = require(version);
    requirePhase(profile, phase);
    return profile;
  }

  public boolean supports(ProtocolVersion version) {
    Objects.requireNonNull(version, "version");
    return profilesById.values().stream().anyMatch(profile -> profile.supports(version));
  }

  public boolean supports(ProtocolVersion version, ProtocolCapability capability) {
    Objects.requireNonNull(capability, "capability");
    return require(version).supports(capability);
  }

  public boolean supports(ProtocolVersion version, ProtocolPhase phase) {
    Objects.requireNonNull(version, "version");
    Objects.requireNonNull(phase, "phase");
    return profilesById.values().stream()
        .anyMatch(profile -> profile.supports(version) && profile.supports(phase));
  }

  private static void requirePhase(ProtocolProfile profile, ProtocolPhase phase) {
    if (!profile.supports(phase)) throw new UnsupportedProtocolPhaseException(profile, phase);
  }
}
