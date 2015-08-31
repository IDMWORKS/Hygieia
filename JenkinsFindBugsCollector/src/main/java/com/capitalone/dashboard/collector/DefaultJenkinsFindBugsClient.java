package com.capitalone.dashboard.collector;

import com.capitalone.dashboard.model.*;
import com.capitalone.dashboard.util.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class DefaultJenkinsFindBugsClient implements JenkinsFindBugsClient {
    private static final Log LOG = LogFactory.getLog(DefaultJenkinsFindBugsClient.class);

    private static final String URL_RESOURCES = "/api/resources?format=json";
    private static final String URL_RESOURCE_DETAILS = "/api/resources?format=json&resource=%s&metrics=%s&includealerts=true";

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String KEY = "key";
    private static final String VERSION = "version";
    private static final String MSR = "msr";
    private static final String ALERT = "alert";
    private static final String ALERT_TEXT = "alert_text";
    private static final String VALUE = "val";
    private static final String FORMATTED_VALUE = "frmt_val";
    private static final String STATUS_WARN = "WARN";
    private static final String STATUS_ALERT = "ALERT";
    private static final String DATE = "date";

    private final RestOperations rest;
    private final JenkinsFindBugsSettings jenkinsFindBugsSettings;

    @Autowired
    public DefaultJenkinsFindBugsClient(Supplier<RestOperations> restOperationsSupplier, JenkinsFindBugsSettings settings) {
        this.rest = restOperationsSupplier.get();
        this.jenkinsFindBugsSettings = settings;
    }

    @Override
    public List<JenkinsFindBugsProject> getProjects(String instanceUrl) {
        List<JenkinsFindBugsProject> projects = new ArrayList<>();
        String url = instanceUrl + URL_RESOURCES;

        JenkinsFindBugsProject project = new JenkinsFindBugsProject();
        project.setInstanceUrl(instanceUrl);
        project.setProjectId("testid");
        project.setProjectName("testprojectname");
        projects.add((project));

//        try {
//
//            for (Object obj : parseAsArray(url)) {
//                JSONObject prjData = (JSONObject) obj;
//
//                JenkinsFindBugsProject project = new JenkinsFindBugsProject();
//                project.setInstanceUrl(instanceUrl);
//                project.setProjectId(str(prjData, ID));
//                project.setProjectName(str(prjData, NAME));
//                projects.add((project));
//            }
//
//        } catch (ParseException e) {
//            LOG.error("Could not parse response from: " + url, e);
//        } catch (RestClientException rce) {
//            LOG.error(rce);
//        }

        return projects;
    }

    @Override
    public CodeQuality currentCodeQuality(JenkinsFindBugsProject project) {
        LOG.info("Checking current code quality for project id=" + project.getProjectId() + " name=" + project.getProjectName());
//        String url = String.format(
//                project.getInstanceUrl() + URL_RESOURCE_DETAILS, project.getProjectId());

        CodeQuality cq = new CodeQuality();
        cq.setName(project.getProjectName());
        cq.setUrl(project.getInstanceUrl() + "/dashboard/index/" + project.getProjectId());
        cq.setType(CodeQualityType.StaticAnalysis);
        cq.setTimestamp(System.currentTimeMillis());
        cq.setVersion("0.0.2");

        CodeQualityMetric m1 = new CodeQualityMetric("blocker_violations");
        m1.setValue("test-bad-raw");
        m1.setFormattedValue("test-bad-formatted");
        m1.setStatus(CodeQualityMetricStatus.Alert);
        m1.setStatusMessage("This seems bad");
        cq.getMetrics().add(m1);

        CodeQualityMetric m2 = new CodeQualityMetric("major_violations");
        m2.setValue("test-meh-raw");
        m2.setFormattedValue("test-meh-formatted");
        m2.setStatus(CodeQualityMetricStatus.Warning);
        m2.setStatusMessage("Keep an eye on this I guess");
        cq.getMetrics().add(m2);

        return cq;

//        try {
//            JSONArray jsonArray = parseAsArray(url);
//
//            if (!jsonArray.isEmpty()) {
//                JSONObject prjData = (JSONObject) jsonArray.get(0);
//
//                CodeQuality codeQuality = new CodeQuality();
//                codeQuality.setName(str(prjData, NAME));
//                codeQuality.setUrl(project.getInstanceUrl() + "/dashboard/index/" + project.getProjectId());
//                codeQuality.setType(CodeQualityType.StaticAnalysis);
//                codeQuality.setTimestamp(timestamp(prjData, DATE));
//                codeQuality.setVersion(str(prjData, VERSION));
//
//                for (Object metricObj : (JSONArray) prjData.get(MSR)) {
//                    JSONObject metricJson = (JSONObject) metricObj;
//
//                    CodeQualityMetric metric = new CodeQualityMetric(str(metricJson, KEY));
//                    metric.setValue(metricJson.get(VALUE));
//                    metric.setFormattedValue(str(metricJson, FORMATTED_VALUE));
//                    metric.setStatus(metricStatus(str(metricJson, ALERT)));
//                    metric.setStatusMessage(str(metricJson, ALERT_TEXT));
//                    codeQuality.getMetrics().add(metric);
//                }
//
//                return codeQuality;
//            }
//
//        } catch (ParseException e) {
//            LOG.error("Could not parse response from: " + url, e);
//        } catch (RestClientException rce) {
//            LOG.error(rce);
//        }
//
//        return null;
    }

    private JSONArray parseAsArray(String url) throws ParseException {
        return (JSONArray) new JSONParser().parse(rest.getForObject(url, String.class));
    }

    private long timestamp(JSONObject json, String key) {
        Object obj = json.get(key);
        if (obj != null) {
            try {
                return new SimpleDateFormat(DATE_FORMAT).parse(obj.toString()).getTime();
            } catch (java.text.ParseException e) {
                LOG.error(obj + " is not in expected format " + DATE_FORMAT, e);
            }
        }
        return 0;
    }

    private String str(JSONObject json, String key) {
        Object obj = json.get(key);
        return obj == null ? null : obj.toString();
    }

    private Integer integer(JSONObject json, String key) {
        Object obj = json.get(key);
        return obj == null ? null : (Integer) obj;
    }

    private BigDecimal decimal(JSONObject json, String key) {
        Object obj = json.get(key);
        return obj == null ? null : new BigDecimal(obj.toString());
    }

    private Boolean bool(JSONObject json, String key) {
        Object obj = json.get(key);
        return obj == null ? null : Boolean.valueOf(obj.toString());
    }

    private CodeQualityMetricStatus metricStatus(String status) {
        if (StringUtils.isBlank(status)) {
            return CodeQualityMetricStatus.Ok;
        }

        switch(status) {
            case STATUS_WARN:  return CodeQualityMetricStatus.Warning;
            case STATUS_ALERT: return CodeQualityMetricStatus.Alert;
            default:           return CodeQualityMetricStatus.Ok;
        }
    }
}
