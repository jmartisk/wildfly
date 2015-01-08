package org.jboss.as.test.integration.beanvalidation.hibernate.validator.expression;

import java.util.Arrays;
import java.util.Set;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.validation.ConstraintViolation;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.test.integration.weld.alternative.AlternativeBean;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Jan Martiska
 */
@RunWith(Arquillian.class)
@ServerSetup(ValidationWithExpressionsTestCase.AllowPropertyReplacementSetup.class)
public class ValidationWithExpressionsTestCase {

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

    @EJB
    private ValidatingEJB ejb;

    @Inject
    private ObjectWithSomeConstraints object;

    @Deployment
    public static JavaArchive deployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class);
        archive.addPackage(ValidationWithExpressionsTestCase.class.getPackage());
        archive.addAsManifestResource(
                ValidationWithExpressionsTestCase.class.getPackage(), "jboss.properties",
                "jboss.properties");
        archive.addAsManifestResource(new StringAsset("<beans></beans>"), "beans.xml");
        return archive;
    }

    @Before
    public void setup() {
        object.setStringConstrainedByARegex("x");
    }

    @Test
    public void doit() {
//        ObjectWithSomeConstraints object = new ObjectWithSomeConstraints();
//        object.setStringConstrainedByARegex("x");
        System.out.println("EJB NAME SEEN PROGRAMATICALLY::: " + ValidatingEJB.class.getAnnotation(Singleton.class).name());
        Set<ConstraintViolation<ObjectWithSomeConstraints>> violations = ejb.validateObject(object);
        if (!violations.isEmpty()) {
            Assert.fail("Validation failed, violations = " + Arrays.toString(violations.toArray()));
        }
    }

}
