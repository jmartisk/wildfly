package org.jboss.as.test.integration.beanvalidation.hibernate.validator.expression;

import java.util.Set;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

/**
 * @author Jan Martiska
 */
@Stateless
public class ValidatingEJB {

    @Resource
    private Validator validator;

    public Set<ConstraintViolation<ObjectWithSomeConstraints>> validateObject(ObjectWithSomeConstraints object) {
        return validator.validate(object);
    }
}
