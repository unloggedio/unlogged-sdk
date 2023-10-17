package io.unlogged;

import io.unlogged.command.AgentCommandResponse;

public class AgentCommandRawResponse {
    AgentCommandResponse agentCommandResponse;
    Object responseObject;

    public AgentCommandRawResponse(AgentCommandResponse agentCommandResponse, Object responseObject) {
        this.agentCommandResponse = agentCommandResponse;
        this.responseObject = responseObject;
    }

    public AgentCommandRawResponse(AgentCommandResponse agentCommandResponse) {
        this.agentCommandResponse = agentCommandResponse;
    }

    public AgentCommandResponse getAgentCommandResponse() {
        return agentCommandResponse;
    }

    public Object getResponseObject() {
        return responseObject;
    }

    public void setAgentCommandResponse(AgentCommandResponse agentCommandResponse) {
        this.agentCommandResponse = agentCommandResponse;
    }

    public void setResponseObject(Object responseObject) {
        this.responseObject = responseObject;
    }
}
