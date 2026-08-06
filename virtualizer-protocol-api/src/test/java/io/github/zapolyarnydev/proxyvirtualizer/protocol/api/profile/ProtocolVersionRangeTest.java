package io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

final class ProtocolVersionRangeTest {

  @Test
  void containsVersionsWithinInclusiveBounds() {
    ProtocolVersionRange range =
        new ProtocolVersionRange(new ProtocolVersion(769), new ProtocolVersion(771));

    assertThat(range.contains(new ProtocolVersion(769))).isTrue();
    assertThat(range.contains(new ProtocolVersion(770))).isTrue();
    assertThat(range.contains(new ProtocolVersion(771))).isTrue();
    assertThat(range.contains(new ProtocolVersion(772))).isFalse();
  }

  @Test
  void rejectsInvertedBounds() {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () -> new ProtocolVersionRange(new ProtocolVersion(770), new ProtocolVersion(769)));
  }

  @Test
  void detectsOverlappingRanges() {
    ProtocolVersionRange range =
        new ProtocolVersionRange(new ProtocolVersion(769), new ProtocolVersion(771));

    assertThat(
            range.overlaps(
                new ProtocolVersionRange(new ProtocolVersion(771), new ProtocolVersion(772))))
        .isTrue();
    assertThat(
            range.overlaps(
                new ProtocolVersionRange(new ProtocolVersion(772), new ProtocolVersion(773))))
        .isFalse();
  }
}
