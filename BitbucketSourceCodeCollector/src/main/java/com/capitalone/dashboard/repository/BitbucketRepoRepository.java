package com.capitalone.dashboard.repository;

import com.capitalone.dashboard.model.BitbucketRepo;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface BitbucketRepoRepository extends BaseCollectorItemRepository<BitbucketRepo> {

    @Query(value="{ 'collectorId' : ?0, options.repoUrl : ?1, options.branch : ?2}")
    BitbucketRepo findBitbucketRepo(ObjectId collectorId, String url, String branch);

    @Query(value="{ 'collectorId' : ?0, enabled: true}")
    List<BitbucketRepo> findEnabledBitbucketRepos(ObjectId collectorId);
}
