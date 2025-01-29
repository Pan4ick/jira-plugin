package com.enviogroup.plugins.status.screen;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugin.webfragment.contextproviders.AbstractJiraContextProvider;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.template.soy.SoyTemplateRendererProvider;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.velocity.NumberTool;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AgreementScreen extends AbstractJiraContextProvider {

    private static final NumberTool numberTool = new NumberTool(new Locale("ru", "RU"));

    @Override
    public Map getContextMap(ApplicationUser applicationUser, JiraHelper jiraHelper) {
        Issue issue = (Issue) jiraHelper.getContextParams().get("issue");
        ModelMapper modelMapper = new ModelMapper();
        Map<String, Object> contextMap = new HashMap<>();
        contextMap.put("model", modelMapper.getAgreementModel(issue));
        contextMap.put("number", numberTool);
        contextMap.put("soyRenderer", ComponentAccessor.getComponent(SoyTemplateRendererProvider.class).getRenderer());
        contextMap.put("baseURL", ComponentAccessor.getApplicationProperties().getString("jira.baseurl"));
        return contextMap;
    }
}