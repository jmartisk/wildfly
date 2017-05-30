package org.jboss.as.test.integration.ejb.client.config;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

@Stateless
public class EchoBean implements EchoRemote {

    @Resource
    private SessionContext ctx;

    @Override
    @PermitAll
    public String whoAmI() {
        return ctx.getCallerPrincipal().getName();
    }

}
