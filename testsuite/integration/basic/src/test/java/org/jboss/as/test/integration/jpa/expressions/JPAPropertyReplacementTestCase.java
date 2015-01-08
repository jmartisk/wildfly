package org.jboss.as.test.integration.jpa.expressions;

import java.sql.Connection;
import java.sql.SQLException;
import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * A testcase for verifying property substitution in JPA annotations.
 * @author Jan Martiska
 */
@RunWith(Arquillian.class)
@ServerSetup(JPAPropertyReplacementTestCase.AllowPropertyReplacementSetup.class)
public class JPAPropertyReplacementTestCase {

    @Deployment
    public static JavaArchive deployment() {
        JavaArchive deployment = ShrinkWrap.create(JavaArchive.class);
        deployment.addPackage(JPAPropertyReplacementTestCase.class.getPackage());
        deployment.addAsManifestResource(JPAPropertyReplacementTestCase.class.getPackage(),
                "jboss.properties",
                "jboss.properties");
        deployment.addAsManifestResource(JPAPropertyReplacementTestCase.class.getPackage(), "persistence.xml",
                "persistence.xml");
        System.out.println(deployment.toString(false));
        return deployment;
    }

    static class AllowPropertyReplacementSetup implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String s) throws Exception {
            final ModelNode enableSubstitutionOp = new ModelNode();
            enableSubstitutionOp.get(ClientConstants.OP_ADDR).set(ClientConstants.SUBSYSTEM, "ee");
            enableSubstitutionOp.get(ClientConstants.OP).set(ClientConstants.WRITE_ATTRIBUTE_OPERATION);
            enableSubstitutionOp.get(ClientConstants.NAME).set("annotation-property-replacement");
            enableSubstitutionOp.get(ClientConstants.VALUE).set(true);
            try {
                applyUpdate(managementClient, enableSubstitutionOp);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            System.out.println("*-************************************************************** CONFIGURED!");
        }

        @Override
        public void tearDown(ManagementClient managementClient, String s) throws Exception {
            final ModelNode disableSubstitution = new ModelNode();
            disableSubstitution.get(ClientConstants.OP_ADDR).set(ClientConstants.SUBSYSTEM, "ee");
            disableSubstitution.get(ClientConstants.OP).set(ClientConstants.WRITE_ATTRIBUTE_OPERATION);
            disableSubstitution.get(ClientConstants.NAME).set("annotation-property-replacement");
            disableSubstitution.get(ClientConstants.VALUE).set(false);
            try {
                applyUpdate(managementClient, disableSubstitution);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private void applyUpdate(final ManagementClient managementClient, final ModelNode update)
                throws Exception {
            ModelNode result = managementClient.getControllerClient().execute(update);
            if (result.hasDefined(ClientConstants.OUTCOME)
                    && ClientConstants.SUCCESS.equals(result.get(ClientConstants.OUTCOME).asString())) {
            } else if (result.hasDefined(ClientConstants.FAILURE_DESCRIPTION)) {
                final String failureDesc = result.get(ClientConstants.FAILURE_DESCRIPTION).toString();
                throw new RuntimeException(failureDesc);
            } else {
                throw new RuntimeException("Operation not successful; outcome = " + result.get("outcome"));
            }
        }
    }

    @ArquillianResource
    private InitialContext ctx;

    private DataSource ds;

    @Before
    public void before() throws NamingException {
        ds = (DataSource)ctx.lookup("java:jboss/datasources/ExampleDS");
    }

    /**
     * Deploy an entity with an expression denoting the table name. Verify that the table gets the correct name.
     */
    @Test
    public void testTableName() throws SQLException {
        Connection connection = ds.getConnection();
        Assert.fail(connection.toString());
    }
}
