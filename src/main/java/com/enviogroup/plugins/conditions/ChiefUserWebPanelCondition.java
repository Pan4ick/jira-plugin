package com.enviogroup.plugins.conditions;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.plugin.webfragment.conditions.AbstractWebCondition;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.user.ApplicationUser;

public class ChiefUserWebPanelCondition extends AbstractWebCondition {

    public static final String JIRA_CHIEF_USERS_GROUP = "jira-chief-users";

    @Override
    public boolean shouldDisplay(ApplicationUser user, JiraHelper helper) {
        return ComponentAccessor.getGroupManager().isUserInGroup(user, JIRA_CHIEF_USERS_GROUP);
    }
}
