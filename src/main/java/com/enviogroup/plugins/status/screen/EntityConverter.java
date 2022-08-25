package com.enviogroup.plugins.status.screen;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.enviogroup.plugins.documentation.IssueWorker;

import java.util.HashMap;
import java.util.Map;

public class EntityConverter {
    @ComponentImport
    private static final IssueManager issueManager = ComponentAccessor.getIssueManager();
    private static final ApplicationUser currentUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
    private static final IssueService issueService = ComponentAccessor.getIssueService();
    private static final CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
    private static final IssueWorker issueWorker = new IssueWorker(currentUser, issueService, customFieldManager, issueManager);

    public Map getEntityConverterMap(JiraHelper jiraHelper) {
        Map<String, Object> entityConverterMap = new HashMap<>();
        Issue issue = (Issue) jiraHelper.getContextParams().get("issue");
        entityConverterMap.put("currentUser", currentUser);
        entityConverterMap.put("issue", issue);
        entityConverterMap.put("documents", issueMap(issue, "Договоры"));
        entityConverterMap.put("issueWorker", issueWorker);
        return entityConverterMap;
    }

    private Map<Integer, Object> issueMap(Issue issueId, String cfName) {
        Map<Integer, Object> issueMap = new HashMap<>();
        int j = 0;
        for (MutableIssue i : issueWorker.getMutableIssuesList(issueId, cfName)) {
            issueMap.put(j++, i);
        }
        return issueMap;
    }
}