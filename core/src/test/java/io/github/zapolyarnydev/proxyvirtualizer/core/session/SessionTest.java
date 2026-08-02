package io.github.zapolyarnydev.proxyvirtualizer.core.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.zapolyarnydev.proxyvirtualizer.core.session.exception.SessionStateException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class SessionTest {

  private static final Clock CLOCK = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);

  @Test
  void activatesAnInitializingSession() {
    Session session = createSession();

    Session activeSession = session.activate();

    assertThat(activeSession.state()).isEqualTo(SessionState.ACTIVE);
    assertThat(activeSession.createdAt()).isEqualTo(Instant.EPOCH);
    assertThat(activeSession.id()).isEqualTo(session.id());
  }

  @Test
  void rejectsAnInvalidStateTransition() {
    Session activeSession = createSession().activate();

    assertThatThrownBy(activeSession::activate).isInstanceOf(SessionStateException.class);
  }

  @Test
  void closesAnActiveSession() {
    Session closedSession = createSession().activate().close();

    assertThat(closedSession.state()).isEqualTo(SessionState.CLOSED);
    assertThatThrownBy(closedSession::close).isInstanceOf(SessionStateException.class);
  }

  private static Session createSession() {
    return Session.create(
        new PlayerId(UUID.randomUUID()), new ConnectionId(UUID.randomUUID()), CLOCK);
  }
}
