package io.unlogged.runner.exceptions;

public class ExecutionFailedException extends TestRunException {
    public ExecutionFailedException(Exception e) {
        super(e);
    }
}
