package io.github.zapolyarnydev.proxyvirtualizer.api.exception;

public class PlayerAlreadyConnectedException extends RuntimeException {
    public PlayerAlreadyConnectedException(String message) {
        super(message);
    }
}
