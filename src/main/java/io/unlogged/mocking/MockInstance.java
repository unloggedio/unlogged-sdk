package io.unlogged.mocking;

import net.bytebuddy.dynamic.DynamicType;

public class MockInstance {
    private final Object mockedFieldInstance;
    private final MockHandler mockHandler;

    public MockInstance(Object mockedFieldInstance, MockHandler mockHandler) {

        this.mockedFieldInstance = mockedFieldInstance;
        this.mockHandler = mockHandler;
    }

    public Object getMockedFieldInstance() {
        return mockedFieldInstance;
    }

    public MockHandler getMockHandler() {
        return mockHandler;
    }
}
