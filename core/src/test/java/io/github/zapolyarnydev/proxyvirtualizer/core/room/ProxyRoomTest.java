package io.github.zapolyarnydev.proxyvirtualizer.core.room;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.zapolyarnydev.proxyvirtualizer.core.room.exception.ProxyRoomFullException;
import io.github.zapolyarnydev.proxyvirtualizer.core.room.exception.SessionAlreadyExistsException;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.ConnectionId;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.PlayerId;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.Session;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.SessionState;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class ProxyRoomTest {

  private static final Clock CLOCK = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);

  @Test
  void opensAndActivatesSession() {
    ProxyRoom room = new ProxyRoom(new RoomId(1));
    PlayerId playerId = playerId();

    Session active = room.openSession(playerId, connectionId(), CLOCK);

    assertThat(active.state()).isEqualTo(SessionState.ACTIVE);
    assertThat(room.findSession(playerId)).contains(active);
  }

  @Test
  void rejectsAnotherSessionForTheSamePlayer() {
    ProxyRoom room = new ProxyRoom(new RoomId(1));
    PlayerId playerId = playerId();
    room.openSession(playerId, connectionId(), CLOCK);

    assertThatThrownBy(() -> room.openSession(playerId, connectionId(), CLOCK))
        .isInstanceOf(SessionAlreadyExistsException.class);
  }

  @Test
  void closesSessionAndAllowsReconnect() {
    ProxyRoom room = new ProxyRoom(new RoomId(1));
    PlayerId playerId = playerId();
    ConnectionId connectionId = connectionId();
    room.openSession(playerId, connectionId, CLOCK);

    assertThat(room.closeSession(playerId))
        .hasValueSatisfying(session -> assertThat(session.state()).isEqualTo(SessionState.CLOSED));
    assertThat(room.findSession(playerId)).isEmpty();
    assertThat(room.openSession(playerId, connectionId, CLOCK)).isNotNull();
  }

  @Test
  void rejectsNewSessionWhenAtCapacity() {
    ProxyRoom room = new ProxyRoom(new RoomId(1));
    room.limitRoomSessions(1);
    room.openSession(playerId(), connectionId(), CLOCK);

    assertThatThrownBy(() -> room.openSession(playerId(), connectionId(), CLOCK))
        .isInstanceOf(ProxyRoomFullException.class);
  }

  private static PlayerId playerId() {
    return new PlayerId(UUID.randomUUID());
  }

  private static ConnectionId connectionId() {
    return new ConnectionId(UUID.randomUUID());
  }
}
