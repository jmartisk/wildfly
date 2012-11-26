package org.jboss.as.test.clustering.extended.ejb2.stateful.passivation;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.clustering.EJBClientContextSelector;
import org.jboss.as.test.clustering.NodeInfoServlet;
import org.jboss.as.test.clustering.NodeNameGetter;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.CreateException;
import javax.ejb.RemoveException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.rmi.NoSuchObjectException;
import java.util.Properties;

import static junit.framework.Assert.assertTrue;
import static org.jboss.as.test.clustering.ClusteringTestConstants.*;

/**
 * The test creates 500 stateful session beans, executes some calls to
 * stress state replication, waits for passivation and exipration to kick
 * in, and then updates the sessions to produce the session removal
 * conflict seen in JBAS-1560 (Clustered stateful session bean removal of
 * expired passivated instances causes deadlock). This is sensative to
 * timing issues so a failure in activation can show up;
 * we catch any NoSuchObjectException to handle this.
 *
 * Migrated StatefulPassivationExpirationUnitTestCase from AS5 testsuite.
 *
 * @author Jan Martiska / jmartisk@redhat.com
 */
@RunWith(Arquillian.class)
@RunAsClient
public class StatefulPassivationExpirationTestCase extends ClusterPassivationTestBase {

    private static Logger log = Logger.getLogger(ClusterPassivationTestCase.class);
    protected static InitialContext context;
    private static final String ARCHIVE_NAME = "stateful-passivation-expiration";


    @BeforeClass
    public static void beforeClass() throws Exception {
        Properties env = new Properties();
        env.setProperty(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        context = new InitialContext(env);
    }

    @ArquillianResource
    private ContainerController controller;
    @ArquillianResource
    private Deployer deployer;

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(CONTAINER_1)
    public static Archive<?> deployment0() {
        Archive<?> archive = createDeployment();
        return archive;
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(CONTAINER_2)
    public static Archive<?> deployment1() {
        Archive<?> archive = createDeployment();
        return archive;
    }

    private static Archive<?> createDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME + ".war");
        war.addClasses(StatefulBeanBase.class, StatefulBean.class, StatefulRemote.class, StatefulRemoteHome.class);
        war.addClasses(NodeNameGetter.class, NodeInfoServlet.class);
//        war.addAsWebInfResource(ClusterPassivationDDTestCase.class.getPackage(), "ejb-jar-with-stateful-timeout.xml", "ejb-jar.xml");
//        war.addAsWebInfResource(ClusterPassivationDDTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
        log.info(war.toString(true));
        return war;
    }

    @Test
    @InSequence(-1)
    public void arquillianStartServers() {
        startServers(null, null);
    }


    @Test
    public void test1(@ArquillianResource @OperateOnDeployment(DEPLOYMENT_1) ManagementClient client1,
                      @ArquillianResource @OperateOnDeployment(DEPLOYMENT_2) ManagementClient client2) throws NamingException, CreateException, IOException {
        log.info("+++ testStatefulPassivationExpiration");

        EJBClientContextSelector.setup("cluster/ejb3/stateful/failover/sfsb-failover-jboss-ejb-client.properties");

        int beanCount = 500;
        StatefulRemoteHome home = (StatefulRemoteHome) context.lookup("ejb:/" + ARCHIVE_NAME + "/" + StatefulBean.class.getSimpleName() + "!"
                + StatefulRemoteHome.class.getName());

        long start = System.currentTimeMillis();
        log.info("Start bean creation");
        StatefulRemote[] beans = new StatefulRemote[beanCount];
        long[] accessStamp = new long[beanCount];
        for(int n = 0; n < beans.length; n ++)
        {
            beans[n] = home.create();
            accessStamp[n] = System.currentTimeMillis();
        }
        long end = System.currentTimeMillis();
        log.info("End bean creation, elapsed="+(end - start));
/*

        try {
        Thread.sleep(180000);
        } catch(Exception e) {}
*/

        int N = 5000;
        long min = 99999, max = 0, maxInactive = 0;
        for(int n = 0; n < N; n ++)
        {
            int id = n % beans.length;
            StatefulRemote bean = beans[id];
            if (bean == null)
                continue;  // bean timed out and removed
            long callStart = System.currentTimeMillis();
            long inactive = callStart - accessStamp[id];
            try
            {
                log.info("Setting number for id=" + id);
                bean.setNumber(id);
                log.info("Retrieving number for id=" + id);
                int number = bean.getNumber();

                long now = System.currentTimeMillis();
                long elapsed = now - callStart;
                accessStamp[id] = now;
                assertTrue("Id == "+id, number == id);
                min = Math.min(min, elapsed);
                max = Math.max(max, elapsed);
                maxInactive = Math.max(maxInactive, inactive);
                log.info(n+", elapsed="+elapsed+", inactive="+inactive);
            }
            catch (NoSuchObjectException nso)
            {
                log.info("Caught NoSuchObjectException on bean id=" + id + " -- inactive time = " + inactive);
                // Remove the bean as it will never succeed again
                beans[id] = null;
            }
        }
        log.info(N+" calls complete, max="+max+", min="+min+", maxInactive="+maxInactive);

        log.info("WAITING now");
        try {
            Thread.sleep(15000);
        } catch (Exception e) {

        }
        start = System.currentTimeMillis();
        for(int n = 0; n < beans.length; n ++)
        {
            beans[n] = home.create();
            accessStamp[n] = System.currentTimeMillis();
        }
        end = System.currentTimeMillis();
        log.info("End second round bean creation, elapsed="+(end - start));
        for(int n = 0; n < N; n ++)
        {
            int id = n % beans.length;
            StatefulRemote bean = beans[id];
            if (bean == null)
                continue;  // bean timed out and removed
            long callStart = System.currentTimeMillis();
            long inactive = callStart - accessStamp[id];
            try
            {
                bean.setNumber(id);
                int number = bean.getNumber();

                long now = System.currentTimeMillis();
                long elapsed = now - callStart;
                accessStamp[id] = now;
                assertTrue("Id == "+id, number == id);
                min = Math.min(min, elapsed);
                max = Math.max(max, elapsed);
                maxInactive = Math.max(maxInactive, inactive);
                log.info(n+", elapsed="+elapsed+", inactive="+inactive);
            }
            catch (NoSuchObjectException nso)
            {
                log.info(n+" Caught NoSuchObjectException on bean " + id + " -- inactive time = " + (callStart - accessStamp[id]));
                // Remove the bean as it will never succeed again
                beans[id] = null;
            }
        }
        log.info(N+" calls complete, max="+max+", min="+min+", maxInactive="+maxInactive);

        for(int n = 0; n < beans.length; n ++)
        {
            try
            {
                if (beans[n] != null)
                    beans[n].remove();
            }
            catch (java.rmi.NoSuchObjectException nso)
            {
                log.info("Caught NoSuchObjectException removing bean " + n);
            } catch (RemoveException e) {
                log.info("Caught RemoveException removing bean " + n);
            }
        }

        Assert.fail("passed");
    }

    @Test
    @InSequence(100)
    public void arquillianStopServers() {
        stopServers();
    }


    @Override
    protected void startServers(ManagementClient client1, ManagementClient client2) {
        if (client1 == null || !client1.isServerInRunningState()) {
            log.info("Starting server: " + CONTAINER_1);
            if(!controller.isStarted(CONTAINER_1))
                controller.start(CONTAINER_1);
            deployer.deploy(DEPLOYMENT_1);
        }
        if (client2 == null || !client2.isServerInRunningState()) {
            log.info("Starting server: " + CONTAINER_2);
            if(!controller.isStarted(CONTAINER_2))
                controller.start(CONTAINER_2);
            deployer.deploy(DEPLOYMENT_2);
        }
    }

    protected void stopServers() {
        if(controller.isStarted(CONTAINER_1))
            controller.stop(CONTAINER_1);
        if(controller.isStarted(CONTAINER_2))
            controller.stop(CONTAINER_2);
    }
}
