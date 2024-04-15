package io.unlogged.command;

public class ServerMetadata {
    public void setAgentServerPort(int agentServerPort) {
        this.agentServerPort = agentServerPort;
    }

    private int agentServerPort;
    String includePackageName;
    String agentVersion;
    String agentServerUrl;
    public ServerMetadata(String includePackageName, String agentVersion, int agentServerPort) {
        this.includePackageName = includePackageName;
        this.agentVersion = agentVersion;
        this.agentServerPort = agentServerPort;
        this.agentServerUrl = "http://localhost:" + String.valueOf(agentServerPort);
    }

    @Override
    public String toString() {
        return "{" +
                "\"agentServerPort\": \"" + agentServerPort + "\"," +
                "\"includePackageName\": \"" + includePackageName + "\"," +
                "\"agentServerUrl\": \"" + agentServerUrl + "\"," +
                "\"agentVersion\": \"" + agentVersion + '\"' +
                '}';
    }

    public int getAgentServerPort() {
        return agentServerPort;
    }

    public String getAgentServerUrl() {
        return agentServerUrl;
    }

    public void setAgentServerUrl(String agentServerUrl) {
        this.agentServerUrl = agentServerUrl;
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
