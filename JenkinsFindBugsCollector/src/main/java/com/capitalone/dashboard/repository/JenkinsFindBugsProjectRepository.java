package com.capitalone.dashboard.repository;

import com.capitalone.dashboard.model.JenkinsFindBugsProject;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface JenkinsFindBugsProjectRepository extends BaseCollectorItemRepository<JenkinsFindBugsProject> {

    @Query(value="{ 'collectorId' : ?0, options.instanceUrl : ?1, options.projectId : ?2}")
    JenkinsFindBugsProject findJenkinsFindBugsProject(ObjectId collectorId, String instanceUrl, String projectId);

    @Query(value="{ 'collectorId' : ?0, options.instanceUrl : ?1, enabled: true}")
    List<JenkinsFindBugsProject> findEnabledProjects(ObjectId collectorId, String instanceUrl);
}
