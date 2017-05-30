package org.jboss.as.test.integration.ejb.client.config;

import javax.ejb.Remote;

@Remote
public interface EchoRemote {
    String whoAmI();
}
