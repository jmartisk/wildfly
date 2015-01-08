package org.jboss.as.test.integration.beanvalidation.hibernate.validator.expression;

import java.util.Map;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;
import javax.enterprise.inject.Any;
import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import org.jboss.ejb3.annotation.TransactionTimeout;

/**
 * @author Jan Martiska
 */
@Singleton(name = "${ffs:xxx}")
public class ValidatingEJB {

//    @Inject
//    @Any
//    ObjectWithSomeConstraints x;

    @Resource
    private Validator validator;

    public Set<ConstraintViolation<ObjectWithSomeConstraints>> validateObject(
            ObjectWithSomeConstraints object) {
//        x.setStringConstrainedByARegex("x");
//        return validator.validate(x);
        return validator.validate(object);
    }

    @Resource
    SessionContext ctx;

    @PostConstruct
    public void testejb() {
        System.out.println("CONTEXT DATA:");
        for (Map.Entry<String, Object> stringObjectEntry : ctx.getContextData().entrySet()) {
            System.out.println(stringObjectEntry.getKey() +" :: " + stringObjectEntry.getValue());
        }

    }

}
