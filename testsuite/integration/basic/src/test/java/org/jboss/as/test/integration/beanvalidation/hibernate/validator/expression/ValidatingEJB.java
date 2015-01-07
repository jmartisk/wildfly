package org.jboss.as.test.integration.beanvalidation.hibernate.validator.expression;

import java.util.Set;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.enterprise.inject.Any;
import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

/**
 * @author Jan Martiska
 */
@Singleton
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
}
