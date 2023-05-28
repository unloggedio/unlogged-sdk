package io.unlogged.command;

public class AgentCommandErrorResponse {

    private ResponseType responseType;

    public ResponseType getResponseType() {
        return responseType;
    }

    public void setResponseType(ResponseType responseType) {
        this.responseType = responseType;
    }

    public String getMessage() {
        return message;
    }

    private final String message;

    public AgentCommandErrorResponse(String message) {
        this.message = message;
    }
}
