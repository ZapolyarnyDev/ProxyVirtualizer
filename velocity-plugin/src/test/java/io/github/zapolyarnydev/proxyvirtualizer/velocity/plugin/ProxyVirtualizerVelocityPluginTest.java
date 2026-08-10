package io.github.zapolyarnydev.proxyvirtualizer.velocity.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.zapolyarnydev.proxyvirtualizer.velocity.adapter.connection.VelocityPlayerConnectionListener;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

class ProxyVirtualizerVelocityPluginTest {

  @Test
  void registersPlayerConnectionListenerDuringInitialization() {
    RecordingEventManager recordingEventManager = new RecordingEventManager();
    ProxyVirtualizerVelocityPlugin plugin =
        new ProxyVirtualizerVelocityPlugin(proxyServer(recordingEventManager), logger());

    plugin.onProxyInitialize(null);

    assertThat(recordingEventManager.plugin).isSameAs(plugin);
    assertThat(recordingEventManager.listener).isInstanceOf(VelocityPlayerConnectionListener.class);
  }

  private static ProxyServer proxyServer(RecordingEventManager recordingEventManager) {
    EventManager eventManager =
        (EventManager)
            Proxy.newProxyInstance(
                EventManager.class.getClassLoader(),
                new Class<?>[] {EventManager.class},
                (proxy, method, arguments) -> {
                  if (method.getName().equals("register")) {
                    recordingEventManager.plugin = arguments[0];
                    recordingEventManager.listener = arguments[1];
                    return null;
                  }

                  throw new UnsupportedOperationException(method.getName());
                });

    return (ProxyServer)
        Proxy.newProxyInstance(
            ProxyServer.class.getClassLoader(),
            new Class<?>[] {ProxyServer.class},
            (proxy, method, arguments) -> {
              if (method.getName().equals("getEventManager")) return eventManager;

              throw new UnsupportedOperationException(method.getName());
            });
  }

  private static Logger logger() {
    return (Logger)
        Proxy.newProxyInstance(
            Logger.class.getClassLoader(),
            new Class<?>[] {Logger.class},
            (proxy, method, arguments) -> {
              if (method.getName().equals("info")) return null;

              throw new UnsupportedOperationException(method.getName());
            });
  }

  private static final class RecordingEventManager {

    private Object plugin;
    private Object listener;
  }
}
