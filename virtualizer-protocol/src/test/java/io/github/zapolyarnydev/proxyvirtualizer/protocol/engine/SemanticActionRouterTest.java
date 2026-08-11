package io.github.zapolyarnydev.proxyvirtualizer.protocol.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.zapolyarnydev.proxyvirtualizer.protocol.api.action.SemanticAction;
import io.github.zapolyarnydev.proxyvirtualizer.protocol.engine.exception.UnknownSemanticActionException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class SemanticActionRouterTest {

  @Test
  void routesActionsByTheirExactSemanticType() {
    List<TestAction> received = new ArrayList<>();
    SemanticActionRouter router =
        SemanticActionRouter.builder().route(TestAction.class, received::add).build();

    router.accept(new TestAction("value"));

    assertThat(received).containsExactly(new TestAction("value"));
  }

  @Test
  void rejectsUnknownSemanticAction() {
    SemanticActionRouter router = SemanticActionRouter.builder().build();

    assertThatThrownBy(() -> router.accept(new TestAction("unknown")))
        .isInstanceOf(UnknownSemanticActionException.class)
        .hasMessageContaining(TestAction.class.getName());
  }

  private record TestAction(String value) implements SemanticAction {}
}
