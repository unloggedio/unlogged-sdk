package io.unlogged.autoexecutor.testutils.autoCIUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.unlogged.command.AgentCommandRequest;
import io.unlogged.command.AgentCommandRequestType;
import io.unlogged.command.AgentCommandResponse;
import io.unlogged.command.ResponseType;
import okhttp3.*;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class AgentClientLite {
    private final OkHttpClient client = new OkHttpClient.Builder()
            .callTimeout(Duration.of(30, ChronoUnit.SECONDS))
            .readTimeout(Duration.of(5, ChronoUnit.MINUTES))
            .connectTimeout(Duration.of(500, ChronoUnit.MILLIS)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final String agentUrl = "http://localhost:12100";
    public static final String NO_SERVER_CONNECT_ERROR_MESSAGE = "Failed to invoke call to agent server: \n" +
            "Make sure the process is running with java unlogged-sdk\n\n";


    public AgentCommandResponse executeCommand(AgentCommandRequest agentCommandRequest) throws IOException {
        agentCommandRequest.setRequestType(AgentCommandRequestType.DIRECT_INVOKE);

        RequestBody body = RequestBody.create(objectMapper.writeValueAsString(agentCommandRequest), JSON);
        Request request = new Request.Builder()
                .url(agentUrl + "/command")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            return objectMapper.readValue(responseBody,
                    new TypeReference<AgentCommandResponse>() {
                    });
        } catch (Throwable e) {
            AgentCommandResponse agentCommandResponse = new AgentCommandResponse();
            agentCommandResponse.setResponseType(ResponseType.FAILED);
            agentCommandResponse.setMessage(NO_SERVER_CONNECT_ERROR_MESSAGE + e.getMessage());
            return agentCommandResponse;
        }
    }

    public boolean isConnected() {
        Request pingRequest = new Request.Builder()
                .url(agentUrl + "/ping")
                .build();
        try {
            Response response = client.newCall(pingRequest).execute();
            response.close();
            return true;
        } catch (Throwable e) {
            return false;
        }
    }
}
