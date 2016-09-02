package com.sonyericsson.jenkins.plugins.bfa;

import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseBuildAction;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseDisplayData;
import com.sonyericsson.jenkins.plugins.bfa.model.FoundFailureCause;
import hudson.model.Job;
import hudson.model.Run;
import jenkins.model.Jenkins;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for the GerritMessageProviderExtensionTest.
 * @author Alexander Akbashev mr.akbashev@gmail.com
 * @throws Exception if so.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({PluginImpl.class, Jenkins.class})
public class GerritMessageProviderExtensionTest {
    private static final String JENKINS_URL =  "http://some.jenkins.com";
    private static final String BUILD_URL = "jobs/build/123";

    /**
     * Initialize basic stuff: Jenkins, PluginImpl, etc.
     */
    @Before
    public void setUp() {
        PowerMockito.mockStatic(Jenkins.class);
        Jenkins jenkins = PowerMockito.mock(Jenkins.class);
        PowerMockito.when(Jenkins.getInstance()).thenReturn(jenkins);
        PowerMockito.when(jenkins.getRootUrl()).thenReturn(JENKINS_URL);


        PowerMockito.mockStatic(PluginImpl.class);
        PluginImpl plugin = PowerMockito.mock(PluginImpl.class);
        PowerMockito.when(plugin.isGerritTriggerEnabled()).thenReturn(true);
        PowerMockito.when(PluginImpl.getInstance()).thenReturn(plugin);
    }

    /**
     *
     * Creates run with desired failure cause
     *
     * @param cause failure cause that would be displayed.
     * @return Run with desired failure cause.
     */
    private Run getRunWithTopCause(String cause) {
        Run run = PowerMockito.mock(Run.class);
        Job parent = PowerMockito.mock(Job.class);
        FailureCauseBuildAction action = PowerMockito.mock(FailureCauseBuildAction.class);

        List<FoundFailureCause> failureCauses = new ArrayList<FoundFailureCause>();
        failureCauses.add(new FoundFailureCause(new FailureCause("testName", cause)));
        FailureCauseDisplayData displayData = new FailureCauseDisplayData("parentURL", "parentName",
                "/jobs/build/123", "buildName");
        displayData.setFoundFailureCauses(failureCauses);

        PowerMockito.when(run.getAction(FailureCauseBuildAction.class)).thenReturn(action);
        PowerMockito.when(run.getUrl()).thenReturn(BUILD_URL);
        PowerMockito.when(run.getParent()).thenReturn(parent);
        PowerMockito.when(action.getBuild()).thenReturn(run);
        PowerMockito.when(action.getFailureCauseDisplayData()).thenReturn(displayData);

        return run;
    }

    /**
     *
     * Creates run with desired failure cause on third level
     *
     * @param cause failure cause that would be displayed.
     * @return Run with desired failure cause in third level.
     */
    private Run getRunWithDeepCause(String cause) {
        Run run = PowerMockito.mock(Run.class);
        Job parent = PowerMockito.mock(Job.class);
        FailureCauseBuildAction action = PowerMockito.mock(FailureCauseBuildAction.class);

        List<FoundFailureCause> emptyFailureCauses = new ArrayList<FoundFailureCause>();
        List<FoundFailureCause> failureCauses = new ArrayList<FoundFailureCause>();
        failureCauses.add(new FoundFailureCause(new FailureCause("testName", cause)));

        FailureCauseDisplayData bottomDisplayData = new FailureCauseDisplayData("parentURL", "bottomBuildName",
                "/jobs/build/789", "bottomBuildName");
        bottomDisplayData.setFoundFailureCauses(failureCauses);

        FailureCauseDisplayData middleDisplayData = new FailureCauseDisplayData("parentURL", "middleBuildName",
                "/jobs/build/456", "middleBuildName");
        middleDisplayData.setFoundFailureCauses(emptyFailureCauses);
        middleDisplayData.addDownstreamFailureCause(bottomDisplayData);

        FailureCauseDisplayData topLevelDisplayData = new FailureCauseDisplayData("topParentURL", "topParentName",
                "/jobs/build/123", "topBuildName");
        topLevelDisplayData.setFoundFailureCauses(emptyFailureCauses);
        topLevelDisplayData.addDownstreamFailureCause(middleDisplayData);

        PowerMockito.when(run.getAction(FailureCauseBuildAction.class)).thenReturn(action);
        PowerMockito.when(run.getUrl()).thenReturn(BUILD_URL);
        PowerMockito.when(run.getParent()).thenReturn(parent);
        PowerMockito.when(action.getBuild()).thenReturn(run);
        PowerMockito.when(action.getFailureCauseDisplayData()).thenReturn(topLevelDisplayData);

        return run;
    }

    /**
     * Test that single quote would be escaped.
     */
    @Test
    public void testSingleQuote() {
        Run run = getRunWithTopCause("it's a single quote");

        GerritMessageProviderExtension extension = new GerritMessageProviderExtension();

        Assert.assertEquals("it&#39s a single quote ( http://some.jenkins.com/jobs/build/123 )",
                extension.getBuildCompletedMessage(run));
    }

    /**
     * Test that multiple quotes would be escaped.
     */
    @Test
    public void testMultipleQuotes() {
        Run run = getRunWithTopCause("q'u'o't'e's");

        GerritMessageProviderExtension extension = new GerritMessageProviderExtension();

        Assert.assertEquals("q&#39u&#39o&#39t&#39e&#39s ( http://some.jenkins.com/jobs/build/123 )",
                extension.getBuildCompletedMessage(run));
    }

    /**
     * Test that message from top build would be displayed.
     */
    @Test
    public void testTopCause() {
        Run run = getRunWithTopCause("some cause");

        GerritMessageProviderExtension extension = new GerritMessageProviderExtension();

        Assert.assertEquals("some cause ( http://some.jenkins.com/jobs/build/123 )",
                extension.getBuildCompletedMessage(run));
    }

    /**
     * Test that message from nested build would be displayed.
     */
    @Test
    public void testDeepCause() {
        Run run = getRunWithDeepCause("deep cause");

        GerritMessageProviderExtension extension = new GerritMessageProviderExtension();

        Assert.assertEquals("deep cause ( http://some.jenkins.com/jobs/build/789 )",
                extension.getBuildCompletedMessage(run));
    }
}
