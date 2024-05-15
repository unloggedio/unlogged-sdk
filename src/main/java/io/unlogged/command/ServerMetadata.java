package io.unlogged.command;

import java.util.TimeZone;

public class ServerMetadata {
    public void setAgentServerPort(int agentServerPort) {
        this.agentServerPort = agentServerPort;
    }

    private int agentServerPort;
    String includePackageName;
    String agentVersion;
    String agentServerUrl;
	private String hostname;
	private long createdAt;

    public ServerMetadata(String includePackageName, String agentVersion, String hostname, int agentServerPort) {
        this.includePackageName = includePackageName;
        this.agentVersion = agentVersion;
		this.createdAt = System.currentTimeMillis();
        this.agentServerPort = agentServerPort;
        this.agentServerUrl = "http://localhost:" + String.valueOf(agentServerPort);
		this.hostname = hostname;
    }

    @Override
    public String toString() {
        return "{" +
                "\"agentServerPort\": \"" + agentServerPort + "\"," +
                "\"includePackageName\": \"" + includePackageName + "\"," +
                "\"agentServerUrl\": \"" + agentServerUrl + "\"," +
                "\"agentVersion\": \"" + agentVersion + "\"," +
                "\"hostname\": \"" + hostname + "\"," +
                "\"createdAt\": \"" + createdAt + "\"," +
                "\"timezone\": \"" + TimeZone.getDefault().getID() + '\"' +
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
