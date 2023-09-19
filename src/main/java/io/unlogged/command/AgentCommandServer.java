package io.unlogged.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.iki.elonen.NanoHTTPD;
import io.unlogged.Runtime;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AgentCommandServer extends NanoHTTPD {

    private final ServerMetadata serverMetadata;
    ObjectMapper objectMapper = new ObjectMapper();
    private AgentCommandExecutor agentCommandExecutor;
    private String pingResponseBody;

    public AgentCommandServer(int port, ServerMetadata serverMetadata) {
        super(port);
        this.serverMetadata = serverMetadata;
        init();
    }

    public AgentCommandServer(String hostname, int port, ServerMetadata serverMetadata) {
        super(hostname, port);
        this.serverMetadata = serverMetadata;
        init();
    }

    public void init() {
        AgentCommandResponse pingResponse = new AgentCommandResponse();
        pingResponse.setMessage("ok");
        pingResponse.setResponseType(ResponseType.NORMAL);
        try {
            pingResponse.setMethodReturnValue(serverMetadata);
            pingResponseBody = objectMapper.writeValueAsString(pingResponse);
        } catch (JsonProcessingException e) {
            // should never happen
        }

    }

    @Override
    public Response serve(IHTTPSession session) {
        String requestBodyText = null;
        Map<String, String> bodyParams = new HashMap<>();
        try {
            session.parseBody(bodyParams);
        } catch (IOException | ResponseException e) {
            return newFixedLengthResponse("{\"message\": \"" + e.getMessage() + "\", }");
        }
        requestBodyText = bodyParams.get("postData");
        String postBody = session.getQueryParameterString();

        String requestPath = session.getUri();
        Method requestMethod = session.getMethod();
//        System.err.println("[" + requestMethod + "] " + requestPath + ": " + postBody + " - " + requestBodyText);
        if (requestPath.equals("/ping")) {
            return newFixedLengthResponse(Response.Status.OK, "application/json", pingResponseBody);
        }
        try {
            AgentCommandRequest agentCommandRequest = objectMapper.readValue(
                    postBody != null ? postBody : requestBodyText,
                    AgentCommandRequest.class);
            AgentCommandResponse commandResponse;
            switch (agentCommandRequest.getCommand()) {
                case EXECUTE:
                    commandResponse = this.agentCommandExecutor.executeCommand(agentCommandRequest);
                    break;
                case INJECT_MOCKS:
                    commandResponse = this.agentCommandExecutor.injectMocks(agentCommandRequest);
                    break;
                case REGISTER_CLASS:
//                    System.out.println("RegisterClass over wire");
                    String classWeaveInfoData = agentCommandRequest.getMethodParameters().get(0);
                    String probesToRecord = agentCommandRequest.getMethodParameters().get(1);
                    Runtime.registerClass(classWeaveInfoData, probesToRecord);
                    commandResponse = new AgentCommandResponse();
                    commandResponse.setResponseType(ResponseType.NORMAL);
                    break;
                case REMOVE_MOCKS:
                    commandResponse = this.agentCommandExecutor.removeMocks(agentCommandRequest);
                    break;
                default:
                    System.err.println(
                            "Unknown request [" + requestMethod + "] " + requestPath + " - " + agentCommandRequest);

                    commandResponse = new AgentCommandResponse();
                    commandResponse.setMessage("unknown command: " + agentCommandRequest.getCommand());
                    commandResponse.setResponseType(ResponseType.FAILED);
                    break;
            }
            String responseBody = objectMapper.writeValueAsString(commandResponse);
            return newFixedLengthResponse(Response.Status.OK, "application/json", responseBody);
        } catch (Throwable e) {
            e.printStackTrace();
            AgentCommandErrorResponse agentCommandErrorResponse = new AgentCommandErrorResponse(e.getMessage());
            if (e instanceof NoSuchMethodException) {
                agentCommandErrorResponse.setResponseType(ResponseType.FAILED);
            } else {
                agentCommandErrorResponse.setResponseType(ResponseType.EXCEPTION);
            }
            String errorResponseBody = null;
            try {
                errorResponseBody = objectMapper.writeValueAsString(agentCommandErrorResponse);
            } catch (JsonProcessingException ex) {
                return newFixedLengthResponse("{\"message\": \"" + ex.getMessage() + "\"}");
            }

            return newFixedLengthResponse(errorResponseBody);
        }

    }

    public void setAgentCommandExecutor(AgentCommandExecutor agentCommandExecutor) {
        this.agentCommandExecutor = agentCommandExecutor;
    }
}
