package com.capitalone.dashboard.collector;


import com.capitalone.dashboard.model.*;
import com.capitalone.dashboard.repository.BaseCollectorRepository;
import com.capitalone.dashboard.repository.BitbucketRepoRepository;
import com.capitalone.dashboard.repository.CommitRepository;
import com.capitalone.dashboard.repository.ComponentRepository;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * CollectorTask that fetches Commit information from Bitbucket
 */
@Component
public class BitbucketCollectorTask extends CollectorTask<Collector> {
    private static final Log LOG = LogFactory.getLog(BitbucketCollectorTask.class);

    private final BaseCollectorRepository<Collector> collectorRepository;
    private final BitbucketRepoRepository bitbucketRepoRepository;
    private final CommitRepository commitRepository;
    private final BitbucketClient bitbucketClient;
    private final BitbucketSettings bitbucketSettings;
    private final ComponentRepository dbComponentRepository;

    @Autowired
    public BitbucketCollectorTask(TaskScheduler taskScheduler,
                               BaseCollectorRepository<Collector> collectorRepository,
                               BitbucketRepoRepository bitbucketRepoRepository,
                               CommitRepository commitRepository,
                               BitbucketClient bitbucketClient,
                               BitbucketSettings bitbucketSettings,
                               ComponentRepository dbComponentRepository) {
        super(taskScheduler, "Bitbucket");
        this.collectorRepository = collectorRepository;
        this.bitbucketRepoRepository = bitbucketRepoRepository;
        this.commitRepository = commitRepository;
        this.bitbucketClient = bitbucketClient;
        this.bitbucketSettings = bitbucketSettings;
        this.dbComponentRepository = dbComponentRepository;
    }

    @Override
    public Collector getCollector() {
        Collector protoType = new Collector();
        protoType.setName("Bitbucket");
        protoType.setCollectorType(CollectorType.SCM);
        protoType.setOnline(true);
        protoType.setEnabled(true);
        return protoType;
    }

    @Override
    public BaseCollectorRepository<Collector> getCollectorRepository() {
        return collectorRepository;
    }

    @Override
    public String getCron() {
        return bitbucketSettings.getCron();
    }

    /**
     * Clean up unused deployment collector items
     *
     * @param collector
     *            the {@link UDeployCollector}
     */

    private void clean(Collector collector) {
        Set<ObjectId> uniqueIDs = new HashSet<ObjectId>();
        /**
         * Logic: For each component, retrieve the collector item list of the type SCM.
         * Store their IDs in a unique set ONLY if their collector IDs match with Bitbucket collectors ID.
         */
        for (com.capitalone.dashboard.model.Component comp : dbComponentRepository
                .findAll()) {
            if ((comp.getCollectorItems() != null)
                    && !comp.getCollectorItems().isEmpty()) {
                List<CollectorItem> itemList = comp.getCollectorItems().get(
                        CollectorType.SCM);
                if (itemList != null) {
                    for (CollectorItem ci : itemList) {
                        if ((ci != null) && (ci.getCollectorId().equals(collector.getId()))){
                            uniqueIDs.add(ci.getId());
                        }
                    }
                }
            }
        }

        /**
         * Logic: Get all the collector items from the collector_item collection for this collector.
         * If their id is in the unique set (above), keep them enabled; else, disable them.
         */
        List<BitbucketRepo> repoList = new ArrayList<BitbucketRepo>();
        Set<ObjectId> bitbucketID = new HashSet<ObjectId>();
        bitbucketID.add(collector.getId());
        for (BitbucketRepo repo : bitbucketRepoRepository.findByCollectorIdIn(bitbucketID)) {
            if (repo != null) {
                repo.setEnabled(uniqueIDs.contains(repo.getId()));
                repoList.add(repo);
            }
        }
        bitbucketRepoRepository.save(repoList);
    }


    @Override
    public void collect(Collector collector) {

        logBanner("Starting...");
        long start = System.currentTimeMillis();
        int repoCount = 0;
        int commitCount = 0;

        clean(collector);
        for (BitbucketRepo repo : enabledRepos(collector)) {
            repo.setLastUpdateTime(new DateTime());
            bitbucketRepoRepository.save(repo);
            for (Commit commit : bitbucketClient.getCommits(repo)) {
                if (isNewCommit(repo, commit)) {
                    commit.setCollectorItemId(repo.getId());
                    commitRepository.save(commit);
                    commitCount++;
                }
            }
            repoCount++;
        }
        log("Repo Count", start, repoCount);
        log("New Commits", start, commitCount);
        log("Finished", start);
    }

    private DateTime lastUpdated(BitbucketRepo repo) {
        return repo.getLastUpdateTime();
    }

    private List<BitbucketRepo> enabledRepos(Collector collector) {
        return bitbucketRepoRepository.findEnabledBitbucketRepos(collector.getId());
    }

    private boolean isNewCommit(BitbucketRepo repo, Commit commit) {
        return commitRepository.findByCollectorItemIdAndScmRevisionNumber(
                repo.getId(), commit.getScmRevisionNumber()) == null;
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
            token3 = StringUtils.leftPad(elapsed, 30 - text.length() );
        } else {
            String countStr = count.toString();
            token2 = StringUtils.leftPad(countStr, 20 - text.length() );
            token3 = StringUtils.leftPad(elapsed, 10 );
        }
        LOG.info(text + token2 + token3);
    }

    private void logBanner(String instanceUrl) {
        LOG.info("------------------------------");
        LOG.info(instanceUrl);
        LOG.info("------------------------------");
    }
}
