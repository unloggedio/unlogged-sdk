package io.unlogged.command;

import java.net.InetAddress;
import java.net.UnknownHostException;
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

    public ServerMetadata(String includePackageName, String agentVersion, int agentServerPort) {
        this.includePackageName = includePackageName;
        this.agentVersion = agentVersion;
		this.createdAt = System.currentTimeMillis() / 1000L;
        this.agentServerPort = agentServerPort;
        this.agentServerUrl = "http://localhost:" + String.valueOf(agentServerPort);

		InetAddress inetAddress;
		try {
			inetAddress = InetAddress.getLocalHost();
			this.hostname = inetAddress.getHostName();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			this.hostname = null;
		}
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
