package com.capitalone.dashboard.collector;

import com.capitalone.dashboard.model.Commit;
import com.capitalone.dashboard.model.BitbucketRepo;

import java.util.Date;
import java.util.List;

/**
 * Client for fetching commit history from Bitbucket
 */
public interface BitbucketClient {

    /**
     * Fetch all of the commits for the provided BitbucketRepo.
     *
     * @param repo BitbucketRepo
     * @return all commits in repo
     */

    List<Commit> getCommits(BitbucketRepo repo);

}
