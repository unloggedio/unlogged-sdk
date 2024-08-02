package io.unlogged.autoexecutor.testutils.entity;

import java.net.URL;
import java.util.TreeMap;

public class TestConfig {

    private String projectId;
    private TreeMap<String, URL> modeToResourceMap;

    public TestConfig(String projectId, TreeMap<String, URL> modeToResourceMap) {
        this.projectId = projectId;
        this.modeToResourceMap = modeToResourceMap;
    }

    public String getProjectId() {
        return projectId;
    }

    public TreeMap<String, URL> getModeToResourceMap() {
        return modeToResourceMap;
    }
}
