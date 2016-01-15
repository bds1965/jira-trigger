package com.ceilfors.jenkins.plugins.jirabuilder
import jenkins.model.GlobalConfiguration
import org.junit.Rule
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import spock.lang.Specification

import java.util.logging.Level

import static com.ceilfors.jenkins.plugins.jirabuilder.JiraCommentBuilderTrigger.DEFAULT_COMMENT
/**
 * @author ceilfors
 */
class JiraBuilderAcceptanceTest extends Specification {

    String jiraRootUrl = "http://localhost:2990/jira"
    String jiraUsername = "admin"
    String jiraPassword = "admin"

    JenkinsRunner jenkins = new JenkinsRunner()

    @Rule
    RuleChain ruleChain = RuleChain
            .outerRule(new JulLogLevelRule(Level.FINEST))
            .around(jenkins)
            .around(
            new ExternalResource() {
                @Override
                protected void before() throws Throwable {
                    JiraBuilderGlobalConfiguration configuration = GlobalConfiguration.all().get(JiraBuilderGlobalConfiguration)
                    configuration.jiraRootUrl = jiraRootUrl
                    configuration.jiraUsername = jiraUsername
                    configuration.jiraPassword = jiraPassword
                    jira = new RealJiraRunner(jenkins.instance, configuration)
                    jira.deleteAllWebhooks()
                    jira.registerWebhook(jenkins.webhookUrl.replace("localhost", "10.0.2.2"))
                }
            })

    JiraRunner jira

    def 'Trigger simple job when a comment is created'() {
        given:
        def issueKey = jira.createIssue()
        jenkins.createJiraTriggeredProject("job")

        when:
        jira.addComment(issueKey, DEFAULT_COMMENT)

        then:
        jenkins.buildShouldBeScheduled("job")
        jira.shouldBeNotifiedWithComment(issueKey, "job")
    }

    def 'Trigger job with built-in field when a comment is created'() {
        given:
        def issueKey = jira.createIssue("Dummy issue description")
        jenkins.createJiraTriggeredProject("simpleJob", "jenkins_description", "jenkins_key")
        jenkins.addParameterMapping("simpleJob", "jenkins_description", "fields.description")
        jenkins.addParameterMapping("simpleJob", "jenkins_key", "key")

        when:
        jira.addComment(issueKey, DEFAULT_COMMENT)

        then:
        jenkins.buildShouldBeScheduledWithParameter("simpleJob", [
                "jenkins_description": "Dummy issue description",
                "jenkins_key"        : issueKey
        ])
    }

    def 'Job is triggered when a comment matches the comment pattern'() {
        given:
        def issueKey = jira.createIssue()
        jenkins.createJiraTriggeredProject("job")
        jenkins.setJiraBuilderCommentPattern("job", ".*jira trigger.*")

        when:
        jira.addComment(issueKey, "bla jira trigger bla")

        then:
        jenkins.buildShouldBeScheduled("job")
    }

    def 'Job is not triggered when a comment does not match the comment pattern'() {
        given:
        def issueKey = jira.createIssue()
        jenkins.createJiraTriggeredProject("job")
        jenkins.setJiraBuilderCommentPattern("job", ".*jira trigger.*")

        when:
        jira.addComment(issueKey, DEFAULT_COMMENT)

        then:
        jenkins.noBuildShouldBeScheduled()
    }

    def 'Job is triggered when the issue matches JQL filter'() {
        given:
        def issueKey = jira.createIssue("dummy description")
        jenkins.createJiraTriggeredProject("job")
        jenkins.setJiraBuilderJqlFilter("job", 'type=task and description~"dummy description" and status="To Do"')

        when:
        jira.addComment(issueKey, DEFAULT_COMMENT)

        then:
        jenkins.buildShouldBeScheduled("job")
    }

    def 'Job is not triggered when the issue does not match JQL filter'() {
        given:
        def issueKey = jira.createIssue("dummy description")
        jenkins.createJiraTriggeredProject("job")
        jenkins.setJiraBuilderJqlFilter("job", 'type=task and status="Done"')

        when:
        jira.addComment(issueKey, DEFAULT_COMMENT)

        then:
        jenkins.noBuildShouldBeScheduled()
    }

    def 'Jobs is triggered when JIRA configuration is set from the UI'() {
        given:
        def issueKey = jira.createIssue("Dummy issue description")
        jenkins.createJiraTriggeredProject("simpleJob", "jenkins_description")
        jenkins.setJiraBuilderJqlFilter("simpleJob", 'type=task and description~"dummy description" and status="To Do"')

        when:
        jenkins.setJiraBuilderGlobalConfig(jiraRootUrl, jiraUsername, jiraPassword)
        jira.addComment(issueKey, DEFAULT_COMMENT)

        then:
        jenkins.buildShouldBeScheduled("simpleJob")
    }

    def 'Comment pattern by default must not be empty'() {
        when:
        jenkins.createJiraTriggeredProject("job")

        then:
        jenkins.triggerCommentPatternShouldNotBeEmpty("job")
    }

    // ** Incremental features: **
    // Add comment - to notify that a build is scheduled
    // Add comment - Visibility must be the requester and jira-administrators
    // Add comment - Visibility must be configured in global configuration
    // Add comment - when there is a comment pattern that matches, but no jobs have been triggered
    // Trigger job when issue is updated - all
    // Trigger job when issue is updated - filter by field
    // Trigger job when issue is updated - filter by from and to value
    // --- 0.1.0 ---

    // Classes javadoc
    // --- 0.2.0 ---

    // Register webhook from Jenkins configuration page
    // Document log names in wiki
    // Make AcceptanceTest independent of JIRA
    // Run CI in CloudBees Jenkins
    // --- 1.0.0 ---

    // How to enable JenkinsRule as ClassRule to make the build faster
    // JiraTriggerCause should contain issue key and link
    // Override UncaughtExceptionHandler in Acceptance Test to catch Exception, especially when webhook is configured wrongly and Acceptance test don't see any error
    // Form Validation in Global Config by hitting JIRA
    // Check SequentialExecutionQueue that is used by GitHubWebHook
    // Should JiraWebhook be RootAction rather than UnprotectedRootAction? Check out RequirePostWithGHHookPayload
    // Translate JiraBuilderException to error messages
}
