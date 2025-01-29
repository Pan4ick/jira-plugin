package com.enviogroup.plugins.conditions;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugin.webfragment.conditions.AbstractWebCondition;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.user.ApplicationUser;

import java.util.Objects;

public class AgreementWebPanelCondition extends AbstractWebCondition {

    public static final String AGREEMENT = "Договоры";

    @Override
    public boolean shouldDisplay(ApplicationUser user, JiraHelper helper) {
        Issue currentIssue = (Issue) helper.getContextParams().get("issue");
        String issueTypeName = Objects.requireNonNull(currentIssue.getIssueType()).getName();
        return issueTypeName.equals(AGREEMENT);
    }
}