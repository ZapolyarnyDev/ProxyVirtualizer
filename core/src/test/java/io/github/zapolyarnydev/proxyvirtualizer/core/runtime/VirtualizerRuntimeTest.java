package io.github.zapolyarnydev.proxyvirtualizer.core.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.zapolyarnydev.proxyvirtualizer.core.room.ProxyRoom;
import io.github.zapolyarnydev.proxyvirtualizer.core.room.RoomId;
import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.exception.PlayerConnectionNotFoundException;
import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.exception.ProxyRoomAlreadyExistsException;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.PlayerId;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.Session;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.SessionState;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class VirtualizerRuntimeTest {

  private static final Clock CLOCK = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);

  @Test
  void connectingPlayerDoesNotOpenSession() {
    VirtualizerRuntime runtime = new VirtualizerRuntime();
    PlayerId playerId = playerId();

    runtime.playerConnected(playerId);

    assertThat(runtime.findConnection(playerId)).isPresent();
    assertThat(runtime.findSession(playerId)).isEmpty();
  }

  @Test
  void opensPlayerSessionInExplicitlySelectedRoom() {
    VirtualizerRuntime runtime = new VirtualizerRuntime();
    ProxyRoom firstRoom = new ProxyRoom(new RoomId(1));
    ProxyRoom secondRoom = new ProxyRoom(new RoomId(2));
    PlayerId playerId = playerId();
    runtime.registerRoom(firstRoom);
    runtime.registerRoom(secondRoom);
    runtime.playerConnected(playerId);

    Session session = runtime.openSession(playerId, secondRoom.id(), CLOCK);

    assertThat(session.state()).isEqualTo(SessionState.ACTIVE);
    assertThat(firstRoom.findSession(playerId)).isEmpty();
    assertThat(secondRoom.findSession(playerId)).contains(session);
  }

  @Test
  void disconnectingPlayerClosesSessionInItsRoom() {
    VirtualizerRuntime runtime = new VirtualizerRuntime();
    ProxyRoom room = new ProxyRoom(new RoomId(1));
    PlayerId playerId = playerId();
    runtime.registerRoom(room);
    runtime.playerConnected(playerId);
    runtime.openSession(playerId, room.id(), CLOCK);

    runtime.playerDisconnected(playerId);

    assertThat(runtime.findConnection(playerId)).isEmpty();
    assertThat(runtime.findSession(playerId)).isEmpty();
    assertThat(room.findSession(playerId)).isEmpty();
  }

  @Test
  void rejectsOpeningSessionForDisconnectedPlayer() {
    VirtualizerRuntime runtime = new VirtualizerRuntime();
    runtime.registerRoom(new ProxyRoom(new RoomId(1)));

    assertThatThrownBy(() -> runtime.openSession(playerId(), new RoomId(1), CLOCK))
        .isInstanceOf(PlayerConnectionNotFoundException.class);
  }

  @Test
  void rejectsDuplicateRoomRegistration() {
    VirtualizerRuntime runtime = new VirtualizerRuntime();
    runtime.registerRoom(new ProxyRoom(new RoomId(1)));

    assertThatThrownBy(() -> runtime.registerRoom(new ProxyRoom(new RoomId(1))))
        .isInstanceOf(ProxyRoomAlreadyExistsException.class);
  }

  private static PlayerId playerId() {
    return new PlayerId(UUID.randomUUID());
  }
}
