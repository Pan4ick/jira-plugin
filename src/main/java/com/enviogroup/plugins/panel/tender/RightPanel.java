package com.enviogroup.plugins.panel.tender;


import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.label.Label;
import com.atlassian.jira.plugin.webfragment.contextproviders.AbstractJiraContextProvider;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.template.soy.SoyTemplateRendererProvider;
import com.atlassian.jira.user.ApplicationUser;
import com.enviogroup.plugins.customfield.Constants;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

public class RightPanel extends AbstractJiraContextProvider {

    @Override
    public Map getContextMap(ApplicationUser applicationUser, JiraHelper jiraHelper) {
        Map<String, Object> contextMap = new HashMap<>();
        Issue issue = (Issue) jiraHelper.getContextParams().get("issue");

        contextMap.put("issue", issue);
        contextMap.put("baseURL", getJiraURL());
        contextMap.put("soyRenderer", ComponentAccessor.getComponent(SoyTemplateRendererProvider.class).getRenderer());

        try {
            CustomField customField = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(Constants.SYSTEM_LABLES_FIELD);
            TreeSet<Label> treeSet = (TreeSet<Label>) issue.getCustomFieldValue(customField);
            for (Label label : treeSet) {
                if ("latenottogo".equalsIgnoreCase(label.getLabel())) {
                    contextMap.put("labelnottogo", true);
                }
            }
        } catch (ClassCastException e) {
            //ignore
        } catch (NullPointerException e) {
            //ignore
        }
        return contextMap;
    }

    private String getJiraURL() {
        return ComponentAccessor.getApplicationProperties().getString("jira.baseurl");
    }
}
