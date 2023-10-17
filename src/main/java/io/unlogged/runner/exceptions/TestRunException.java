package io.unlogged.runner.exceptions;

public class TestRunException extends Throwable {
    public TestRunException() {
    }

    public TestRunException(String message) {
        super(message);
    }

    public TestRunException(String message, Throwable cause) {
        super(message, cause);
    }

    public TestRunException(Throwable cause) {
        super(cause);
    }
}
