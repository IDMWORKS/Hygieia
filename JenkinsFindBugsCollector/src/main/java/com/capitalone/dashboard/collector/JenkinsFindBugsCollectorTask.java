package com.capitalone.dashboard.collector;

import com.capitalone.dashboard.model.*;
import com.capitalone.dashboard.repository.BaseCollectorRepository;
import com.capitalone.dashboard.repository.CodeQualityRepository;
import com.capitalone.dashboard.repository.ComponentRepository;
import com.capitalone.dashboard.repository.JenkinsFindBugsCollectorRepository;
import com.capitalone.dashboard.repository.JenkinsFindBugsProjectRepository;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class JenkinsFindBugsCollectorTask extends CollectorTask<JenkinsFindBugsCollector> {
    private static final Log LOG = LogFactory.getLog(JenkinsFindBugsCollectorTask.class);

    private final JenkinsFindBugsCollectorRepository jenkinsFindBugsCollectorRepository;
    private final JenkinsFindBugsProjectRepository jenkinsFindBugsProjectRepository;
    private final CodeQualityRepository codeQualityRepository;
    private final JenkinsFindBugsClient jenkinsFindBugsClient;
    private final JenkinsFindBugsSettings jenkinsFindBugsSettings;
    private final ComponentRepository dbComponentRepository;
    private final int CLEANUP_INTERVAL = 3600000;

    @Autowired
    public JenkinsFindBugsCollectorTask(TaskScheduler taskScheduler,
                              JenkinsFindBugsCollectorRepository jenkinsFindBugsCollectorRepository,
                              JenkinsFindBugsProjectRepository jenkinsFindBugsProjectRepository,
                              CodeQualityRepository codeQualityRepository,
                              JenkinsFindBugsSettings jenkinsFindBugsSettings,
                              JenkinsFindBugsClient jenkinsFindBugsClient,
                              ComponentRepository dbComponentRepository) {
        super(taskScheduler, JenkinsFindBugsCollector.COLLECTOR_TYPE_NAME);
        this.jenkinsFindBugsCollectorRepository = jenkinsFindBugsCollectorRepository;
        this.jenkinsFindBugsProjectRepository = jenkinsFindBugsProjectRepository;
        this.codeQualityRepository = codeQualityRepository;
        this.jenkinsFindBugsSettings = jenkinsFindBugsSettings;
        this.jenkinsFindBugsClient = jenkinsFindBugsClient;
        this.dbComponentRepository = dbComponentRepository;
    }

    @Override
    public JenkinsFindBugsCollector getCollector() {
        return JenkinsFindBugsCollector.prototype(jenkinsFindBugsSettings.getServers());
    }

    @Override
    public BaseCollectorRepository<JenkinsFindBugsCollector> getCollectorRepository() {
        return jenkinsFindBugsCollectorRepository;
    }

    @Override
    public String getCron() {
        return jenkinsFindBugsSettings.getCron();
    }

    @Override
    public void collect(JenkinsFindBugsCollector collector) {
        long start = System.currentTimeMillis();

        // Clean up every hour
        if ((start - collector.getLastExecuted()) > CLEANUP_INTERVAL) {
            clean(collector);
        }
        for (String instanceUrl : collector.getJenkinsFindBugsServers()) {
            logInstanceBanner(instanceUrl);



            List<JenkinsFindBugsProject> projects = jenkinsFindBugsClient.getProjects(instanceUrl);
            int projSize = ((projects != null) ? projects.size() : 0);
            log("Fetched projects   " + projSize , start);

            addNewProjects(projects, collector);

            refreshData(enabledProjects(collector, instanceUrl));

            log("Finished", start);
        }
    }


    /**
     * Clean up unused Jenkins FindBugs collector items
     *
     * @param collector
     *            the {@link com.capitalone.dashboard.model.JenkinsFindBugsCollector}
     */

    private void clean(JenkinsFindBugsCollector collector) {
        Set<ObjectId> uniqueIDs = new HashSet<ObjectId>();
        for (com.capitalone.dashboard.model.Component comp : dbComponentRepository
                .findAll()) {
            if ((comp.getCollectorItems() != null)
                    && !comp.getCollectorItems().isEmpty()) {
                List<CollectorItem> itemList = comp.getCollectorItems().get(
                        CollectorType.CodeQuality);
                if (itemList != null) {
                    for (CollectorItem ci : itemList) {
                        if ((ci != null) && (ci.getCollectorId().equals(collector.getId()))){
                            uniqueIDs.add(ci.getId());
                        }
                    }
                }
            }
        }
        List<JenkinsFindBugsProject> jobList = new ArrayList<JenkinsFindBugsProject>();
        Set<ObjectId> udId = new HashSet<ObjectId>();
        udId.add(collector.getId());
        for (JenkinsFindBugsProject job : jenkinsFindBugsProjectRepository.findByCollectorIdIn(udId)) {
            if (job != null) {
                job.setEnabled(uniqueIDs.contains(job.getId()));
                jobList.add(job);
            }
        }
        jenkinsFindBugsProjectRepository.save(jobList);
    }

    private void refreshData(List<JenkinsFindBugsProject> jenkinsFindBugsProjects) {
        long start = System.currentTimeMillis();
        int count = 0;

        for (JenkinsFindBugsProject project : jenkinsFindBugsProjects) {
            CodeQuality codeQuality = jenkinsFindBugsClient.currentCodeQuality(project);
            if ((codeQuality != null) && isNewQualityData(project, codeQuality)) {
                codeQuality.setCollectorItemId(project.getId());
                codeQualityRepository.save(codeQuality);
                count++;
            }
        }

        log("Updated", start, count);
    }

    private List<JenkinsFindBugsProject> enabledProjects(JenkinsFindBugsCollector collector, String instanceUrl) {
        LOG.info("findEnabledProjects(collectorID=" + collector.getId() + " instanceURL=" + instanceUrl);
        return jenkinsFindBugsProjectRepository.findEnabledProjects(collector.getId(), instanceUrl);
    }

    private void addNewProjects(List<JenkinsFindBugsProject> projects, JenkinsFindBugsCollector collector) {
        long start = System.currentTimeMillis();
        int count = 0;

        for (JenkinsFindBugsProject project : projects) {

            if (isNewProject(collector, project)) {
                project.setCollectorId(collector.getId());
                project.setEnabled(false);
                project.setDescription(project.getProjectName());
                jenkinsFindBugsProjectRepository.save(project);
                count++;
            }
        }
        log("New projects", start, count);
    }

    private boolean isNewProject(JenkinsFindBugsCollector collector, JenkinsFindBugsProject application) {
        LOG.info("findJenkinsFindBugsProject(collectorID=" + collector.getId() + " instanceURL=" + application.getInstanceUrl() + " projectID=" + application.getProjectId());
        return jenkinsFindBugsProjectRepository.findJenkinsFindBugsProject(
                collector.getId(), application.getInstanceUrl(), application.getProjectId()) == null;
    }

    private boolean isNewQualityData(JenkinsFindBugsProject project, CodeQuality codeQuality) {
        return codeQualityRepository.findByCollectorItemIdAndTimestamp(
                project.getId(), codeQuality.getTimestamp()) == null;
    }

    private void log(String marker, long start) {
        log(marker, start, null);
    }

    private void log(String text, long start, Integer count) {
        long end = System.currentTimeMillis();
        String elapsed = ((end - start) / 1000) + "s";
        String token2 = "";
        String token3;
        if (count == null) {
            token3 = StringUtils.leftPad(elapsed, 30 - text.length());
        } else {
            String countStr = count.toString();
            token2 = StringUtils.leftPad(countStr, 20 - text.length() );
            token3 = StringUtils.leftPad(elapsed, 10 );
        }
        LOG.info(text + token2 + token3);
    }

    private void logInstanceBanner(String instanceUrl) {
        LOG.info("------------------------------");
        LOG.info(instanceUrl);
        LOG.info("------------------------------");
    }
}
