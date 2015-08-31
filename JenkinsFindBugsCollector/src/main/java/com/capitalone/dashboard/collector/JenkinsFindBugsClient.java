package com.capitalone.dashboard.collector;

import com.capitalone.dashboard.model.CodeQuality;
import com.capitalone.dashboard.model.JenkinsFindBugsProject;

import java.util.List;

public interface JenkinsFindBugsClient {

    List<JenkinsFindBugsProject> getProjects(String instanceUrl);

    CodeQuality currentCodeQuality(JenkinsFindBugsProject project);

}
