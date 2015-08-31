package com.capitalone.dashboard.collector;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

import com.capitalone.dashboard.model.Commit;
import com.capitalone.dashboard.model.BitbucketRepo;
import com.capitalone.dashboard.util.Encryption;
import com.capitalone.dashboard.util.EncryptionException;
import com.capitalone.dashboard.util.Supplier;

/**
 * GitHubClient implementation that uses SVNKit to fetch information about
 * Subversion repositories.
 */

@Component
public class DefaultBitbucketClient implements BitbucketClient {
    private static final Log LOG = LogFactory.getLog(DefaultBitbucketClient.class);

    private final BitbucketSettings settings;

    private final RestOperations restOperations;
//    private final String SEGMENT_API = "/api/v3/repos/";
    private final String PUBLIC_BITBUCKET_REPO_HOST = "api.bitbucket.org/2.0/repositories/";
    private final String BITBUCKET_QUERY_PARAMS = "?pagelen=100";
//    private final String PUBLIC_GITHUB_HOST_NAME = "bitbucket.org";

    private final DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.dateTimeNoMillis();

    @Autowired
    public DefaultBitbucketClient(BitbucketSettings settings,
                               Supplier<RestOperations> restOperationsSupplier) {
        this.settings = settings;
        this.restOperations = restOperationsSupplier.get();
    }

    @Override
    public List<Commit> getCommits(BitbucketRepo repo) {

        repo.setUserId("bitbucket-username-goes-here");
        repo.setPassword("bitbucket-password-goes-here");
        List<Commit> commits = new ArrayList<>();

        // format URL
        String repoUrl = (String) repo.getOptions().get("url");

        URL url = null;
        String hostName = "";
        String protocol = "";
        try {
            url = new URL(repoUrl);
            hostName = url.getHost();
            protocol = url.getProtocol();
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            LOG.error(e.getMessage());
        }
        String hostUrl = protocol + "://" + hostName + "/";
        String repoName = repoUrl.substring(hostUrl.length(), repoUrl.length());
        String apiUrl = protocol + "://" + PUBLIC_BITBUCKET_REPO_HOST + repoName;
//
//        DateTime dt = repo.getLastUpdateTime().minusMinutes(10000); // randomly
//        // chosen 10
//        // minutes.
//        // Need to
//        // refactor
//        DateTimeFormatter fmt = ISODateTimeFormat.dateHourMinuteSecond();
//        String strDt = fmt.print(dt);
        String queryUrl = apiUrl.concat("/commits/" + repo.getBranch() + BITBUCKET_QUERY_PARAMS);

        // decrypt password
        String decryptedPassword = "";
        // TODO sort out encryption
//        if ((repo.getPassword() != null) && !"".equals(repo.getPassword())) {
//            try {
//                decryptedPassword = Encryption.decryptString(
//                        repo.getPassword(), settings.getKey());
//            } catch (EncryptionException e) {
//                LOG.error(e.getMessage());
//            }
//        }
        decryptedPassword = repo.getPassword();

        try {
            JSONObject rootObject = parseAsObject(makeRestCall(queryUrl,
                    repo.getUserId(), decryptedPassword));
            LOG.info(rootObject.toJSONString());

            JSONArray commitArray = (JSONArray) rootObject.get("values");


            for (Object item : commitArray.toArray()) {
                JSONObject jsonObject = (JSONObject) item;
                String sha = str(jsonObject, "hash");
                JSONObject authorObject = (JSONObject) jsonObject.get("author");
                String message = str(jsonObject, "message");
                String author = str(authorObject, "raw");
//                dateTimeFormatter.parseDateTime(str(jsonObject, "date"));
//                long timestamp = new DateTime(str(jsonObject, "date"))
//                        .getMillis();
//                System.out.println("Converted " + str(jsonObject, "date") + " to " + timestamp);
                long timestamp = dateTimeFormatter.parseDateTime(str(jsonObject, "date"))
                        .getMillis();
                Commit commit = new Commit();

                commit.setTimestamp(System.currentTimeMillis());
                commit.setScmUrl(repo.getRepoUrl());
                commit.setScmRevisionNumber(sha);
                commit.setScmAuthor(author);
                commit.setScmCommitLog(message);
                commit.setScmCommitTimestamp(timestamp);
                commit.setNumberOfChanges(1);
                commits.add(commit);
            }
        } catch (RestClientException re) {
            LOG.error(re.getMessage() + ":" + queryUrl);
        }
        return commits;
    }

    private ResponseEntity<String> makeRestCall(String url, String userId,
                                                String password) {
        // Basic Auth only.
        if (!"".equals(userId) && !"".equals(password)) {
            return restOperations.exchange(url, HttpMethod.GET,
                    new HttpEntity<>(createHeaders(userId, password)),
                    String.class);

        } else {
            return restOperations.exchange(url, HttpMethod.GET, null,
                    String.class);
        }

    }

    private HttpHeaders createHeaders(final String userId, final String password) {
        return new HttpHeaders() {
            {
                String auth = userId + ":" + password;
                byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset
                        .forName("US-ASCII")));
                String authHeader = "Basic " + new String(encodedAuth);
                set("Authorization", authHeader);
            }
        };
    }

    private JSONObject parseAsObject(ResponseEntity<String> response) {
        try {
            return (JSONObject) new JSONParser().parse(response.getBody());
        } catch (ParseException pe) {
            LOG.error(pe.getMessage());
        }
        return new JSONObject();
    }

    private String str(JSONObject json, String key) {
        Object value = json.get(key);
        return value == null ? null : value.toString();
    }

}