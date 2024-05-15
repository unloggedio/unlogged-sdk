package io.unlogged.logging.util;


import io.unlogged.logging.IErrorLogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NetworkClient {

    private static String hostname = null;
    private final String serverUrl;
    private final String sessionId;
    private final String token;
    private final IErrorLogger errorLogger;

    public NetworkClient(String serverUrl, String sessionId, String token, IErrorLogger errorLogger) {
        this.serverUrl = serverUrl;
        this.token = token;
        this.sessionId = sessionId;
        this.errorLogger = errorLogger;
    }

    public String getToken() {
        return token;
    }

    public static String getHostname() {
        if (hostname != null) {
            return hostname;
        }


        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            try {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(Runtime.getRuntime().exec("hostname").getInputStream())
                );
                hostname = reader.readLine();
                reader.close();
            } catch (IOException ex) {
                String userName = System.getProperty("user.name");
                if (userName == null) {
                    userName = "n/a";
                }
                hostname = userName + "-" + UUID.randomUUID();
            }
        }
//        System.out.println("[unlogged] session hostname is [" + hostname + "]");

        return hostname;
    }

    public void sendPOSTRequest(String url, String attachmentFilePath, String loggerPath) throws IOException {

        String charset = "UTF-8";
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "insidious/1.0.0");
        headers.put("Authorization", "Bearer " + this.token);

        MultipartUtility form = null;
        try {
            form = new MultipartUtility(url, charset, headers);
            form.addFilePart("file", new File(attachmentFilePath));
            form.addFilePart("file", new File(loggerPath));
			
			String response = form.finish();
        } catch (IOException e) {
            errorLogger.log("failed to upload - " + e.getMessage());
            throw e;
        }
    }

    public void uploadFile(String filePath, String loggerPath) throws IOException {
//        System.out.println("[unlogged] File to upload to [" + serverUrl + "]: " + filePath);
        long start = System.currentTimeMillis();
        sendPOSTRequest(this.serverUrl + "/session/uploadArchive?sessionId=" + sessionId, filePath, loggerPath);
        long end = System.currentTimeMillis();
        long seconds = (end - start) / 1000;
        if (seconds > 2) {
            System.out.println("[unlogged] Upload took " + seconds + " seconds: " + filePath);
        }
    }

	public String getServerUrl() {
		return serverUrl;
	}

}
