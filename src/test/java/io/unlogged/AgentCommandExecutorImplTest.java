package io.unlogged;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.unlogged.logging.DiscardEventLogger;
import io.unlogged.mocking.*;
import io.unlogged.test.SimplePojoA;
import io.unlogged.test.SimplePojoB;
import io.unlogged.test.SimpleServiceA;
import io.unlogged.test.SimpleServiceB;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class AgentCommandExecutorImplTest {

    @Test
    void arrangeMocks() throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        AgentCommandExecutorImpl agentCommandExecutor = new AgentCommandExecutorImpl(objectMapper,
                new DiscardEventLogger());

        SimpleServiceA simpleServiceA = new SimpleServiceA(new SimpleServiceB());


        List<DeclaredMock> declaredMocks = new ArrayList<>();
        declaredMocks.add(new DeclaredMock(SimplePojoB.class.getCanonicalName(), "simpleServiceB",
                "makeAndReturn",
                Arrays.asList(new ParameterMatcher("any", SimplePojoB.class.getCanonicalName())),
                new ReturnValue("", SimplePojoA.class.getCanonicalName(), ReturnValueType.REAL),
                MethodExitType.NORMAL));

        SimplePojoA expectedValue = simpleServiceA.callToTest("a val", new SimplePojoB("1", 1));
        System.out.println("Expected value: " + objectMapper.writeValueAsString(expectedValue));

        SimpleServiceA simpleServiceAWithMocks = (SimpleServiceA) agentCommandExecutor.arrangeMocks(
                simpleServiceA.getClass(),
                simpleServiceA.getClass().getClassLoader(),
                simpleServiceA, declaredMocks);


        SimplePojoA returnedValue = simpleServiceAWithMocks.callToTest("a val", new SimplePojoB("1", 1));

        System.out.println("Mocked value: " + objectMapper.writeValueAsString(returnedValue));
    }
}