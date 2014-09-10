package org.jboss.as.test.integration.ejb.descriptor.ejbnamewildcard;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;

/**
 * @author Jan Martiska
 */
@Stateless
@LocalBean
public class RestrictedBean {

    public void excludedMethod() {

    }

    public void restrictedMethod() {

    }

}
