package org.wildfly.test.integration.microprofile.metrics.appname;

import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Verify that metrics from different deployments are differentiated by the _app tag.
 */
@RunWith(Arquillian.class)
public class MicroProfileMetricsAppNameTestCase {

    public static final String WAR_DEPLOYMENT_WITH_APPNAME_OVERRIDE = "war-deployment-with-appname-override";
    //    public static final String WAR_DEPLOYMENT_WITH_APPNAME_OVERRIDE_EMPTY = "war-deployment-with-appname-override-empty";
    public static final String WAR_DEPLOYMENT = "war-deployment";
    public static final String JAR_DEPLOYMENT = "jar-deployment";
    public static final String JAR_IN_EAR_DEPLOYMENT = "jar-in-ear-deployment";
    public static final String EAR_NAME = "ear-deployment";

    @Inject
    MetricRegistry metricRegistry;

    @Deployment(name = WAR_DEPLOYMENT)
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, WAR_DEPLOYMENT + ".war");
        war.addClass(SimpleMetricGenerator.class);
        war.addAsManifestResource(new StringAsset(
                        "gauge.value=32"),
                "microprofile-config.properties");
        war.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return war;
    }

    @Deployment(name = WAR_DEPLOYMENT_WITH_APPNAME_OVERRIDE)
    public static Archive<?> deployWithOverride() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, WAR_DEPLOYMENT_WITH_APPNAME_OVERRIDE + ".war");
        war.addClass(SimpleMetricGenerator.class);
        war.addAsManifestResource(new StringAsset(
                        "mp.metrics.appName=OVERRIDDEN\n" +
                                "gauge.value=12"),
                "microprofile-config.properties");
        war.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return war;
    }

    @Deployment(name = JAR_DEPLOYMENT)
    public static Archive<?> deployJar() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, JAR_DEPLOYMENT + ".jar");
        jar.addClass(SimpleMetricGenerator.class);
        jar.addAsManifestResource(new StringAsset("gauge.value=2"),
                "microprofile-config.properties");
        jar.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        return jar;
    }

    @Deployment(name = JAR_IN_EAR_DEPLOYMENT)
    public static Archive<?> deployJarInEar() {
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_NAME + ".ear");

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, JAR_IN_EAR_DEPLOYMENT + ".jar");
        jar.addClass(SimpleMetricGenerator.class);
        jar.addAsManifestResource(new StringAsset("gauge.value=52"),
                "microprofile-config.properties");
        jar.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");

        ear.addAsModule(jar);
        return ear;
    }

/*
    @Deployment(name = WAR_DEPLOYMENT_WITH_APPNAME_OVERRIDE_EMPTY)
    public static Archive<?> deployWithOverrideEmpty() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, WAR_DEPLOYMENT_WITH_APPNAME_OVERRIDE_EMPTY + ".war");
        war.addClass(SimpleMetricGenerator.class);
        war.addAsManifestResource(new StringAsset(
                        "mp.metrics.appName=\n" +
                                "gauge.value=22"),
                "microprofile-config.properties");
        war.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return war;
    }
*/

    @Test
    // it doesn't really matter which deployment, but Arquillian requires one to be chosen
    @OperateOnDeployment(WAR_DEPLOYMENT)
    public void test() {
        // FIXME the expected app names should be their web context roots, not the module names...
        verifyMetric("OVERRIDDEN", 12L);
        verifyMetric("deployment.war-deployment.war", 32L);
        verifyMetric("deployment.jar-deployment.jar", 2L);
        verifyMetric("deployment." + EAR_NAME +".ear." + JAR_IN_EAR_DEPLOYMENT +".jar", 52L);
//        verifyMetric(null, 22L);
    }

    public void verifyMetric(String expectedAppName, Long expectedGaugeValue) {
        List<Tag> expectedTags = new ArrayList<>();
        expectedTags.add(new Tag("t1", "v1"));
        if (expectedAppName != null) {
            expectedTags.add(new Tag("_app", expectedAppName));
        }
        // avoid using MetricID constructor here, we're in a deployment so this would unexpectedly add a tag
        Optional<Map.Entry<MetricID, Gauge>> foundMetric = metricRegistry.getGauges().entrySet().stream().filter(entry ->
                entry.getKey().getTagsAsList().size() == expectedTags.size() &&
                        entry.getKey().getTagsAsList().containsAll(expectedTags)).findFirst();
        System.out.println("GGGG " + metricRegistry.getGauges()); // TODO: remove this
        Assert.assertTrue("Could not find metrics with tags " + expectedTags, foundMetric.isPresent());
        Assert.assertEquals(expectedGaugeValue, foundMetric.get().getValue().getValue());
    }


}
