package org.jboss.as.test.integration.jpa.expressions;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author Jan Martiska
 */
@Entity
@Table(name = "${tablename}")
public class EntityWithParametrizedTable {

    public EntityWithParametrizedTable() {
    }

    @Id
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

}
