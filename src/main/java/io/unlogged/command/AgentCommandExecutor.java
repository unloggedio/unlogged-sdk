package io.unlogged.command;

public interface AgentCommandExecutor {
    AgentCommandResponse executeCommand(AgentCommandRequest agentCommandRequest) throws Exception;
}
