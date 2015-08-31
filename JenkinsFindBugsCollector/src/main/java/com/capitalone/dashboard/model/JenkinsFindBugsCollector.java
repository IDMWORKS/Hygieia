package com.capitalone.dashboard.model;

import java.util.ArrayList;
import java.util.List;

public class JenkinsFindBugsCollector extends Collector {
    private List<String> jenkinsFindBugsServers = new ArrayList<>();

    public static final String COLLECTOR_TYPE_NAME = "JenkinsFindBugs";

    public List<String> getJenkinsFindBugsServers() {
        return jenkinsFindBugsServers;
    }

    public static JenkinsFindBugsCollector prototype(List<String> servers) {
        JenkinsFindBugsCollector protoType = new JenkinsFindBugsCollector();
        protoType.setName(COLLECTOR_TYPE_NAME);
        protoType.setCollectorType(CollectorType.CodeQuality);
        protoType.setOnline(true);
        protoType.setEnabled(true);
        protoType.getJenkinsFindBugsServers().addAll(servers);
        return protoType;
    }
}
