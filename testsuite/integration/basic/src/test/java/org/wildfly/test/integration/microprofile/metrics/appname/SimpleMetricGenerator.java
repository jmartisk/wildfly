package org.wildfly.test.integration.microprofile.metrics.appname;

import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Gauge;

import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Startup
@Singleton
public class SimpleMetricGenerator {

    @Inject
    @ConfigProperty(name = "gauge.value")
    Long prop1;

    @Gauge(name = "g1", absolute = true, tags = {"t1=v1"}, unit = MetricUnits.NONE)
    public Long gauge() {
        return prop1;
    }

}
