package org.jboss.as.test.patching;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.io.IOException;

import static org.jboss.as.patching.IoUtils.mkdir;
import static org.jboss.as.patching.IoUtils.recursiveDelete;
import static org.jboss.as.test.patching.PatchingTestUtil.CONTAINER;
import static org.jboss.as.test.patching.PatchingTestUtil.randomString;

/**
 * @author Martin Simka
 */
public class AbstractPatchingTestCase {

    protected File tempDir;

    @ArquillianResource
    protected ContainerController controller;

    @Before
    public void prepare() throws IOException {
        tempDir = mkdir(new File(System.getProperty("java.io.tmpdir")), randomString());
//        assertPatchElements(baseModuleDir, null);
    }

    @After
    public void cleanup() throws Exception {
        if (controller.isStarted(CONTAINER))
            controller.stop(CONTAINER);
        CliUtilsForPatching.rollbackAll();

        if (recursiveDelete(tempDir)) {
            tempDir.deleteOnExit();
        }
    }
}
