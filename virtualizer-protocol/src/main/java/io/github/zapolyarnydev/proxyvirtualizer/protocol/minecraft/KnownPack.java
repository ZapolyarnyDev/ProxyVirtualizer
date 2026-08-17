package io.github.zapolyarnydev.proxyvirtualizer.protocol.minecraft;

import java.util.Objects;

public record KnownPack(String namespace, String id, String version) {
  public KnownPack {
    namespace = Objects.requireNonNull(namespace, "namespace");
    id = Objects.requireNonNull(id, "id");
    version = Objects.requireNonNull(version, "version");
  }
}
