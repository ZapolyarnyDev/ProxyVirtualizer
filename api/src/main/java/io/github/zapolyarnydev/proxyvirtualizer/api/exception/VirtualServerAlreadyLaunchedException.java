package io.github.zapolyarnydev.proxyvirtualizer.api.exception;

public class VirtualServerAlreadyLaunchedException extends RuntimeException {
    public VirtualServerAlreadyLaunchedException(String message) {
        super(message);
    }
}
