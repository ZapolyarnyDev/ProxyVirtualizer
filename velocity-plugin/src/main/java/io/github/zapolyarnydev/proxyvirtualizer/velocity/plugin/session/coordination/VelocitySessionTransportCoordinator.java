package io.github.zapolyarnydev.proxyvirtualizer.velocity.plugin.session.coordination;

import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.signal.PlayerDisconnectedSignal;
import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.signal.RuntimeSignal;
import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.signal.SessionClosedSignal;
import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.signal.SessionOpenedSignal;
import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.signal.dispatch.RuntimeSignalListener;
import io.github.zapolyarnydev.proxyvirtualizer.core.runtime.snapshot.SessionSnapshot;
import io.github.zapolyarnydev.proxyvirtualizer.core.session.ConnectionId;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolPhase;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolProfile;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.profile.ProtocolVersion;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.SessionTransport;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.TransportCloseReason;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.transport.TransportState;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.registry.ProtocolRegistry;
import io.github.zapolyarnydev.proxyvirtualizer.velocity.adapter.connection.VelocityConnection;
import io.github.zapolyarnydev.proxyvirtualizer.velocity.adapter.connection.VelocityConnectionRegistry;
import io.github.zapolyarnydev.proxyvirtualizer.velocity.plugin.session.port.CoreSessionCloser;
import io.github.zapolyarnydev.proxyvirtualizer.velocity.plugin.session.port.SessionTransportFailureHandler;
import io.github.zapolyarnydev.proxyvirtualizer.velocity.plugin.session.port.VelocitySessionTransportFactory;
import io.github.zapolyarnydev.proxyvirtualizer.velocity.plugin.session.protocol.VelocitySessionProtocol;
import io.github.zapolyarnydev.proxyvirtualizer.velocity.plugin.session.protocol.VelocitySessionProtocolFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongSupplier;
import org.jetbrains.annotations.NotNull;

public final class VelocitySessionTransportCoordinator
    implements RuntimeSignalListener, AutoCloseable {

  private final VelocityConnectionRegistry connections;
  private final VelocitySessionTransportFactory transportFactory;
  private final CoreSessionCloser sessionCloser;
  private final SessionTransportFailureHandler failureHandler;
  private final ProtocolRegistry protocols;
  private final VelocitySessionProtocolFactory protocolFactory;
  private final VelocityBackendConnectionController backendConnections;
  private final ScheduledExecutorService heartbeatScheduler;
  private final Map<ConnectionId, VelocitySessionTransportBinding> transportsByConnectionId =
      new HashMap<>();
  private boolean closed;

  public VelocitySessionTransportCoordinator(
      @NotNull VelocityConnectionRegistry connections,
      @NotNull VelocitySessionTransportFactory transportFactory,
      @NotNull CoreSessionCloser sessionCloser,
      @NotNull SessionTransportFailureHandler failureHandler,
      @NotNull ProtocolRegistry protocols) {
    this(
        connections,
        transportFactory,
        sessionCloser,
        failureHandler,
        protocols,
        () -> ThreadLocalRandom.current().nextLong(),
        Executors.newSingleThreadScheduledExecutor(
            runnable -> {
              Thread thread = new Thread(runnable, "proxyvirtualizer-heartbeat");
              thread.setDaemon(true);
              return thread;
            }),
        Duration.ofSeconds(30));
  }

  VelocitySessionTransportCoordinator(
      @NotNull VelocityConnectionRegistry connections,
      @NotNull VelocitySessionTransportFactory transportFactory,
      @NotNull CoreSessionCloser sessionCloser,
      @NotNull SessionTransportFailureHandler failureHandler,
      @NotNull ProtocolRegistry protocols,
      @NotNull LongSupplier keepAliveIds,
      @NotNull ScheduledExecutorService heartbeatScheduler,
      @NotNull Duration heartbeatTimeout) {
    this(
        connections,
        transportFactory,
        sessionCloser,
        failureHandler,
        protocols,
        VelocityBackendConnections::suspend,
        keepAliveIds,
        heartbeatScheduler,
        heartbeatTimeout);
  }

  VelocitySessionTransportCoordinator(
      @NotNull VelocityConnectionRegistry connections,
      @NotNull VelocitySessionTransportFactory transportFactory,
      @NotNull CoreSessionCloser sessionCloser,
      @NotNull SessionTransportFailureHandler failureHandler,
      @NotNull ProtocolRegistry protocols,
      @NotNull VelocityBackendConnectionController backendConnections,
      @NotNull LongSupplier keepAliveIds,
      @NotNull ScheduledExecutorService heartbeatScheduler,
      @NotNull Duration heartbeatTimeout) {
    this.connections = Objects.requireNonNull(connections, "connections");
    this.transportFactory = Objects.requireNonNull(transportFactory, "transportFactory");
    this.sessionCloser = Objects.requireNonNull(sessionCloser, "sessionCloser");
    this.failureHandler = Objects.requireNonNull(failureHandler, "failureHandler");
    this.protocols = Objects.requireNonNull(protocols, "protocols");
    this.backendConnections = Objects.requireNonNull(backendConnections, "backendConnections");
    this.heartbeatScheduler = Objects.requireNonNull(heartbeatScheduler, "heartbeatScheduler");
    protocolFactory =
        new VelocitySessionProtocolFactory(
            protocols, keepAliveIds, heartbeatScheduler, heartbeatTimeout);
  }

  @Override
  public void onSignal(@NotNull RuntimeSignal signal) {
    Objects.requireNonNull(signal, "signal");
    switch (signal) {
      case SessionOpenedSignal opened -> open(opened.session());
      case SessionClosedSignal closed -> close(closed.session());
      case PlayerDisconnectedSignal disconnected ->
          close(disconnected.connection().connectionId(), TransportCloseReason.REMOTE_CLOSED);
      default -> {}
    }
  }

  @Override
  public void close() {
    drainTransports().forEach(active -> terminate(active, TransportCloseReason.REQUESTED));
    heartbeatScheduler.shutdownNow();
  }

  private void open(SessionSnapshot session) {
    VelocityConnection connection = connections.findConnection(session.connectionId()).orElse(null);
    if (connection == null) {
      reconcileCoreSession(
          session,
          new IllegalStateException("Velocity connection is missing for session " + session.id()));
      return;
    }

    ProtocolVersion version;
    ProtocolProfile profile;
    try {
      version =
          ProtocolVersion.of(
              Objects.requireNonNull(
                      connection.player().getProtocolVersion(), "player protocol version")
                  .getProtocol());
      profile = protocols.profiles().require(version, ProtocolPhase.PLAY);
    } catch (RuntimeException cause) {
      reconcileCoreSession(session, cause);
      return;
    }

    SessionTransport transport;
    try {
      transport =
          Objects.requireNonNull(
              transportFactory.create(connection.player()), "transportFactory result");
    } catch (RuntimeException cause) {
      reconcileCoreSession(session, cause);
      return;
    }

    VelocitySessionProtocol protocol =
        protocolFactory.create(
            version,
            profile,
            ProtocolPhase.PLAY,
            transport,
            cause -> protocolFailed(session, cause));
    VelocityBackendConnection backend;
    try {
      backend = backendConnections.suspend(connection.player());
    } catch (RuntimeException cause) {
      reconcileCoreSession(session, cause);
      return;
    }
    VelocitySessionTransportBinding active =
        new VelocitySessionTransportBinding(session, transport, protocol, backend);
    if (!install(active)) {
      terminate(active, TransportCloseReason.REQUESTED);
      return;
    }
    active.beginSwitching();

    try {
      transport.start(
          new VelocitySessionTransportListener(
              () -> transportOpened(active),
              active.protocol()::onInboundFrame,
              cause -> sessionFailed(active, cause, TransportCloseReason.PROTOCOL_ERROR),
              reason -> transportClosed(active),
              cause -> transportFailed(active, cause)));
    } catch (RuntimeException cause) {
      transportFailed(active, cause);
      return;
    }

    TransportState state = transport.state();
    if (!isActive(active) && state != TransportState.CLOSING && !state.isTerminal())
      closeTransport(active, TransportCloseReason.REQUESTED);
  }

  private void close(SessionSnapshot session) {
    VelocitySessionTransportBinding active = remove(session);
    if (active != null) restore(active);
  }

  private void close(ConnectionId connectionId, TransportCloseReason closeReason) {
    VelocitySessionTransportBinding active = remove(connectionId);
    if (active != null) terminate(active, closeReason);
  }

  private void transportOpened(VelocitySessionTransportBinding active) {
    if (!isActive(active) || !active.isSwitching()) return;

    active.protocol().onOpened();
    active.completeSwitching();
  }

  private void transportClosed(VelocitySessionTransportBinding active) {
    if (remove(active)) {
      if (!active.beginClosing()) return;
      active.protocol().close();
      active.backend().terminate();
      closeCoreSession(active.session());
    }
  }

  private void transportFailed(VelocitySessionTransportBinding active, Throwable cause) {
    sessionFailed(active, cause, TransportCloseReason.TRANSPORT_FAILURE);
  }

  private void protocolFailed(SessionSnapshot session, Throwable cause) {
    VelocitySessionTransportBinding active = find(session);
    if (active == null) {
      reportFailure(session, cause);
      return;
    }
    sessionFailed(active, cause, TransportCloseReason.PROTOCOL_ERROR);
  }

  private void sessionFailed(
      VelocitySessionTransportBinding active, Throwable cause, TransportCloseReason closeReason) {
    boolean owned = remove(active);
    if (!owned) {
      if (!active.transport().state().isTerminal()) terminate(active, closeReason);
      return;
    }

    try {
      terminate(active, closeReason);
    } finally {
      reconcileCoreSession(active.session(), cause);
    }
  }

  private void reconcileCoreSession(SessionSnapshot session, Throwable cause) {
    closeCoreSession(session);
    reportFailure(session, cause);
  }

  private void restore(VelocitySessionTransportBinding active) {
    closeTransport(active, TransportCloseReason.REQUESTED)
        .whenComplete(
            (ignored, cause) -> {
              if (cause != null) {
                active.backend().terminate();
                return;
              }
              try {
                active
                    .backend()
                    .restore()
                    .whenComplete(
                        (restored, restoreCause) -> {
                          if (restoreCause != null) {
                            active.backend().terminate();
                            reportFailure(active.session(), restoreCause);
                          }
                        });
              } catch (RuntimeException restoreException) {
                active.backend().terminate();
                reportFailure(active.session(), restoreException);
              }
            });
  }

  private void terminate(VelocitySessionTransportBinding active, TransportCloseReason closeReason) {
    if (!active.beginClosing()) return;

    closeOwnedTransport(active, closeReason);
    active.backend().terminate();
  }

  private CompletionStage<Void> closeTransport(
      VelocitySessionTransportBinding active, TransportCloseReason closeReason) {
    if (!active.beginClosing()) return CompletableFuture.completedFuture(null);

    return closeOwnedTransport(active, closeReason);
  }

  private CompletionStage<Void> closeOwnedTransport(
      VelocitySessionTransportBinding active, TransportCloseReason closeReason) {
    active.protocol().close();
    try {
      CompletionStage<Void> closing = active.transport().close(closeReason);
      closing.whenComplete(
          (ignored, cause) -> {
            if (cause != null) reportFailure(active.session(), cause);
          });
      return closing;
    } catch (RuntimeException cause) {
      reportFailure(active.session(), cause);
      return CompletableFuture.failedFuture(cause);
    }
  }

  private void closeCoreSession(SessionSnapshot session) {
    try {
      Objects.requireNonNull(sessionCloser.closeSession(session.playerId()), "sessionCloser result")
          .whenComplete(
              (ignored, cause) -> {
                if (cause != null) reportFailure(session, cause);
              });
    } catch (RuntimeException cause) {
      reportFailure(session, cause);
    }
  }

  private void reportFailure(SessionSnapshot session, Throwable cause) {
    try {
      failureHandler.onFailure(session, cause);
    } catch (RuntimeException ignored) {
    }
  }

  private synchronized boolean install(VelocitySessionTransportBinding active) {
    if (closed) return false;

    return transportsByConnectionId.putIfAbsent(active.session().connectionId(), active) == null;
  }

  private synchronized boolean remove(VelocitySessionTransportBinding active) {
    return transportsByConnectionId.remove(active.session().connectionId(), active);
  }

  private synchronized VelocitySessionTransportBinding remove(SessionSnapshot session) {
    VelocitySessionTransportBinding active = transportsByConnectionId.get(session.connectionId());
    if (active == null || !active.session().id().equals(session.id())) return null;

    return transportsByConnectionId.remove(session.connectionId());
  }

  private synchronized VelocitySessionTransportBinding remove(ConnectionId connectionId) {
    return transportsByConnectionId.remove(connectionId);
  }

  private synchronized boolean isActive(VelocitySessionTransportBinding active) {
    return transportsByConnectionId.get(active.session().connectionId()) == active;
  }

  private synchronized VelocitySessionTransportBinding find(SessionSnapshot session) {
    VelocitySessionTransportBinding active = transportsByConnectionId.get(session.connectionId());
    if (active == null || !active.session().id().equals(session.id())) return null;
    return active;
  }

  private synchronized List<VelocitySessionTransportBinding> drainTransports() {
    if (closed) return List.of();

    closed = true;
    List<VelocitySessionTransportBinding> activeTransports =
        new ArrayList<>(transportsByConnectionId.values());
    transportsByConnectionId.clear();
    return activeTransports;
  }
}
