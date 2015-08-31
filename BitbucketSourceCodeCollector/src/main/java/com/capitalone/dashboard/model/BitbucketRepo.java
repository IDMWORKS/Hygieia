package com.capitalone.dashboard.model;

import org.joda.time.DateTime;

/**
 * CollectorItem extension to store the Bitbucket repo url and branch.
 */
public class BitbucketRepo extends CollectorItem {
    private static final String REPO_URL = "repoUrl";
    private static final String BRANCH = "branch"; // master, development etc.
    private static final String USER_ID = "userID";
    private static final String PASSWORD = "password";
    private static final String LAST_UPDATE_TIME = "lastUpdate";

    public String getUserId() {
        return (String) getOptions().get(USER_ID);
    }

    public void setUserId(String userId) {
        getOptions().put(USER_ID, userId);
    }

    public String getPassword() {
        return (String) getOptions().get(PASSWORD);
    }

    public void setPassword(String password) {
        getOptions().put(PASSWORD, password);
    }


    public String getRepoUrl() {
        return (String) getOptions().get(REPO_URL);
    }

    public void setRepoUrl(String instanceUrl) {
        getOptions().put(REPO_URL, instanceUrl);
    }

    public String getBranch() {
        return (String) getOptions().get(BRANCH);
    }

    public void setBranch(String branch) {
        getOptions().put(BRANCH, branch);
    }

    public DateTime getLastUpdateTime() {
        Object latest = getOptions().get(LAST_UPDATE_TIME);
        return (DateTime) latest;
    }

    public void setLastUpdateTime(DateTime date) {
        getOptions().put(LAST_UPDATE_TIME, date);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BitbucketRepo bitbucketRepo = (BitbucketRepo) o;

        return getRepoUrl().equals(bitbucketRepo.getRepoUrl()) & getBranch().equals(bitbucketRepo.getBranch());
    }

    @Override
    public int hashCode() {
        return getRepoUrl().hashCode();
    }

}
