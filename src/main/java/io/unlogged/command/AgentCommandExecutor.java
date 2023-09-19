package io.unlogged.command;

public interface AgentCommandExecutor {
    AgentCommandResponse executeCommand(AgentCommandRequest agentCommandRequest) throws Exception;

    AgentCommandResponse injectMocks(AgentCommandRequest agentCommandRequest) throws Exception;

    AgentCommandResponse removeMocks(AgentCommandRequest agentCommandRequest) throws Exception;
}
