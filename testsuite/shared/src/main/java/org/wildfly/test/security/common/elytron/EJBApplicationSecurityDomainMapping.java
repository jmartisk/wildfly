package org.wildfly.test.security.common.elytron;

import org.jboss.as.test.integration.management.util.CLIWrapper;

/**
 * @author Jan Martiska
 */
public class EJBApplicationSecurityDomainMapping implements ConfigurableElement {

    private final String legacyDomain;
    private final String elytronDomain;


    public EJBApplicationSecurityDomainMapping(String legacyDomain, String elytronDomain) {
        this.legacyDomain = legacyDomain;
        this.elytronDomain = elytronDomain;
    }

    @Override
    public String getName() {
        return legacyDomain;
    }

    @Override
    public void create(CLIWrapper cli) throws Exception {
        cli.sendLine("/subsystem=ejb3/application-security-domain="+legacyDomain+":add(security-domain="+elytronDomain+")");
    }

    @Override
    public void remove(CLIWrapper cli) throws Exception {
        cli.sendLine("/subsystem=ejb3/application-security-domain="+legacyDomain+":remove");
    }
}
