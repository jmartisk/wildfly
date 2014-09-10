package org.jboss.as.test.integration.ejb.descriptor.ejbnamewildcard;

import javax.ejb.EJBAccessException;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for wildcard (*) in ejb-name element of jboss-ejb3.xml
 * @author Jan Martiska
 */
@RunWith(Arquillian.class)
public class EJBNameWildcardTestCase {

    @Deployment
    public static Archive<?> deployment() {
        return ShrinkWrap.create(JavaArchive.class, "ejb-name-wildcard-test.jar")
                .addPackage(RestrictedBean.class.getPackage())
                .addAsManifestResource(RestrictedBean.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
    }

    /*
     * Try to invoke a method which requires special privileges,
     * as defined in jboss-ejb3.xml using ejb-name=*,method-name=restrictedMethod.
     * It shouldn't be allowed.
     */
    @Test(expected = EJBAccessException.class)
    public void testRestrictedMethod() throws Exception {
        getRestrictedBean().restrictedMethod();
    }

    /*
     * Try to invoke a method which is excluded
     * by jboss-ejb3.xml using ejb-name=*,method-name=excludedMethod
     * and shouldn't be callable at all.
     */
    @Test(expected = EJBAccessException.class)
    public void testExcludedMethod() throws Exception {
        getRestrictedBean().excludedMethod();
    }

    private RestrictedBean getRestrictedBean() throws NamingException {
        return (RestrictedBean) new InitialContext().lookup("java:global/ejb-name-wildcard-test/" + RestrictedBean.class.getSimpleName());
    }

}
