package io.unlogged.command;

public class ServerMetadata {
    String includePackageName;
    String agentVersion;

    public ServerMetadata(String includePackageName, String agentVersion) {
        this.includePackageName = includePackageName;
        this.agentVersion = agentVersion;
    }

    public String getIncludePackageName() {
        return includePackageName;
    }

    public void setIncludePackageName(String includePackageName) {
        this.includePackageName = includePackageName;
    }

    public String getAgentVersion() {
        return agentVersion;
    }

    public void setAgentVersion(String agentVersion) {
        this.agentVersion = agentVersion;
    }
}
