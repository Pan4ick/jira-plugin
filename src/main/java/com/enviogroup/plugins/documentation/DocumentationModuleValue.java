package com.enviogroup.plugins.documentation;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugin.webfragment.contextproviders.AbstractJiraContextProvider;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.template.soy.SoyTemplateRendererProvider;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.velocity.NumberTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.enviogroup.plugins.status.screen.CustomField.CUSTOM_FIELD_11411;

public class DocumentationModuleValue extends AbstractJiraContextProvider {

    private static final Logger log = LoggerFactory.getLogger(DocumentationModuleValue.class);
    private static final NumberTool numberTool = new NumberTool(new Locale("ru", "RU"));
    private static final IssueWorker issueWorker = new IssueWorker();

    @Override
    public Map<String, Object> getContextMap(ApplicationUser applicationUser, JiraHelper jiraHelper) {
        Map<String, Object> contextMap = new HashMap<>();
        Issue issue = (Issue) jiraHelper.getContextParams().get("issue");
        contextMap.put("issue", issue);
        contextMap.put("number", numberTool);
        contextMap.put("baseURL", getJiraURL());
        contextMap.put("supplierOffers", issueWorker.getMutableIssuesList(issue, CUSTOM_FIELD_11411));
        contextMap.put("soyRenderer", ComponentAccessor.getComponent(SoyTemplateRendererProvider.class).getRenderer());
        return contextMap;
    }

    private String getJiraURL() {
        return ComponentAccessor.getApplicationProperties().getString("jira.baseurl");
    }

}
