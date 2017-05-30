/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.integration.ejb.client.config;

import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.security.auth.client.AuthenticationConfiguration;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.auth.client.MatchRule;

/**
 * Test case for EJB client connection information (especially the URL in this case)
 * configured using an {@link AuthenticationConfiguration}
 *
 * @author Jan Martiska
 */
@RunWith(Arquillian.class)
@RunAsClient
public class EJBClientConnectionConfiguredByAuthenticationConfigurationTestCase {

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "ejb-app.jar");
        archive.addClasses(EchoBean.class, EchoRemote.class);
        return archive;
    }

//    private static String previousWildflyConfigUrlProp;

   /* @BeforeClass
    public static void setWildflyConfig() {
        System.setProperty("jboss.ejb.client.properties.file.path", null);
        final URL wildflyConfigUrl = ClassLoader
                .getSystemResource("org/jboss/as/test/integration/ejb/client/config/wildfly-config.xml");
        previousWildflyConfigUrlProp = System.setProperty("wildfly.config.url", wildflyConfigUrl.toString());


    }*/

    @Test
    public void invokeAsJoe() throws NamingException {
        AuthenticationConfiguration userConf = AuthenticationConfiguration.empty()
                .useName("joe")
                .useHost(System.getProperty("node0", "127.0.0.1"))
                .usePort(8080);
        AuthenticationContext ctx = AuthenticationContext.empty().with(MatchRule.ALL, userConf);
        AuthenticationContext.getContextManager().setThreadDefault(ctx);
        try {
//            String username = ctx.runCallable(() -> {
            final InitialContext iniCtx = new InitialContext(getEjbClientProperties());
            final EchoRemote bean = (EchoRemote)iniCtx
                    .lookup("ejb:/ejb-app/EchoBean!org.jboss.as.test.integration.ejb.client.config.EchoRemote");
            String username = bean.whoAmI();
//            });
            Assert.assertEquals("joe", username);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

   /* @AfterClass
    public static void restoreWildflyConfig() {
        if (previousWildflyConfigUrlProp != null) {
            System.setProperty("wildfly.config.url", previousWildflyConfigUrlProp);
        }
    }*/

    public Properties getEjbClientProperties() {
        final Properties props = new Properties();
        props.put(Context.INITIAL_CONTEXT_FACTORY, "org.wildfly.naming.client.WildFlyInitialContextFactory");
        return props;
    }

}
