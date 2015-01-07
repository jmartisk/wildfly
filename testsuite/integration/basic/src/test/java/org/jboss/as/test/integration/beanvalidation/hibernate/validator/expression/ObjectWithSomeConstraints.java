package org.jboss.as.test.integration.beanvalidation.hibernate.validator.expression;

import javax.validation.constraints.Pattern;

/**
 * @author Jan Martiska
 */
public class ObjectWithSomeConstraints {

    public ObjectWithSomeConstraints() {
    }

    @Pattern(regexp = "${regex:ggz}")
    private String stringConstrainedByARegex;

    public String getStringConstrainedByARegex() {
        return stringConstrainedByARegex;
    }

    public void setStringConstrainedByARegex(String stringConstrainedByARegex) {
        this.stringConstrainedByARegex = stringConstrainedByARegex;
    }
}
