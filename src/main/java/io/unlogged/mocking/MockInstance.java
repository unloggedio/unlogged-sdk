package io.unlogged.mocking;

import net.bytebuddy.dynamic.DynamicType;

public class MockInstance {
    private final Object mockedFieldInstance;
    private final MockHandler mockHandler;
    private DynamicType.Loaded<?> loadedMockedField;

    public MockInstance(Object mockedFieldInstance, MockHandler mockHandler, DynamicType.Loaded<?> loadedMockedField) {

        this.mockedFieldInstance = mockedFieldInstance;
        this.mockHandler = mockHandler;
        this.loadedMockedField = loadedMockedField;
    }

    public Object getMockedFieldInstance() {
        return mockedFieldInstance;
    }

    public MockHandler getMockHandler() {
        return mockHandler;
    }
}
