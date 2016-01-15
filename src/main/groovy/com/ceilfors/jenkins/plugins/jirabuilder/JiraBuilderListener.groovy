package com.ceilfors.jenkins.plugins.jirabuilder
import com.atlassian.jira.rest.client.api.domain.Comment
import hudson.model.AbstractProject
/**
 * @author ceilfors
 */
interface JiraBuilderListener {
    void buildScheduled(Comment comment, Collection<? extends AbstractProject> projects)
    void buildNotScheduled(Comment comment)
}
