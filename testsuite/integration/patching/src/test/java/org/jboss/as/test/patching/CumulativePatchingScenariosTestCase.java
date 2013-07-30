package org.jboss.as.test.patching;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchBuilder;
import org.jboss.as.version.ProductConfig;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;

import static org.jboss.as.patching.Constants.BASE;
import static org.jboss.as.patching.IoUtils.mkdir;
import static org.jboss.as.patching.IoUtils.recursiveDelete;
import static org.jboss.as.test.patching.PatchingTestUtil.AS_VERSION;
import static org.jboss.as.test.patching.PatchingTestUtil.CONTAINER;
import static org.jboss.as.test.patching.PatchingTestUtil.PRODUCT;
import static org.jboss.as.test.patching.PatchingTestUtil.assertPatchElements;
import static org.jboss.as.test.patching.PatchingTestUtil.baseModuleDir;
import static org.jboss.as.test.patching.PatchingTestUtil.createPatchXMLFile;
import static org.jboss.as.test.patching.PatchingTestUtil.createZippedPatchFile;
import static org.jboss.as.test.patching.PatchingTestUtil.randomString;

/**
 * @author Martin Simka
 */
@RunWith(Arquillian.class)
@RunAsClient
public class CumulativePatchingScenariosTestCase {

    @ArquillianResource
    private ContainerController controller;

    private File tempDir;

    @Before
    public void prepare() throws IOException {
        tempDir = mkdir(new File(System.getProperty("java.io.tmpdir")), randomString());
        assertPatchElements(baseModuleDir, null);
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

    private File createOneOffPatchAddingMiscFile(String patchID, String asVersion) throws Exception {
        File oneOffPatchDir = mkdir(tempDir, patchID);
        ContentModification miscFileAdded = ContentModificationUtils.addMisc(oneOffPatchDir, patchID,
                "test content", "awesomeDirectory", "awesomeFile");
        ProductConfig productConfig = new ProductConfig(PRODUCT, asVersion, "main");
        Patch oneOffPatch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription("A one-off patch adding a misc file.")
                .oneOffPatchIdentity(productConfig.getProductName(), productConfig.getProductVersion())
                .getParent()
                .addContentModification(miscFileAdded)
                .build();
        createPatchXMLFile(oneOffPatchDir, oneOffPatch);
        return createZippedPatchFile(oneOffPatchDir, patchID);
    }

    private File createOneOffPatchAddingAModule(String patchID, String asVersion) throws Exception {
        String layerPatchID = randomString();
        File oneOffPatchDir = mkdir(tempDir, patchID);

        final String moduleName = "org.wildfly.awesomemodule";

        final ResourceItem resourceItem1 = new ResourceItem("testFile1", "content1".getBytes());
        final ResourceItem resourceItem2 = new ResourceItem("testFile2", "content2".getBytes());

        ContentModification moduleAdded = ContentModificationUtils.addModule(oneOffPatchDir, layerPatchID, moduleName, resourceItem1, resourceItem2);
        ProductConfig productConfig = new ProductConfig(PRODUCT, asVersion, "main");
        Patch oneOffPatch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription("A one-off patch adding a new module.")
                .oneOffPatchIdentity(productConfig.getProductName(), productConfig.getProductVersion())
                .getParent()
                .oneOffPatchElement(layerPatchID, "base", false)
                .setDescription("New module for the base layer")
                .addContentModification(moduleAdded)
                .getParent()
                .build();
        createPatchXMLFile(oneOffPatchDir, oneOffPatch);
        return createZippedPatchFile(oneOffPatchDir, patchID);
    }

    private File createCumulativePatch(String patchID, String asVersion) throws Exception {
        String layerPatchID = randomString();
        File oneOffPatchDir = mkdir(tempDir, patchID);

        final String moduleName = "patch.cumulative.awesomemodule";

        final ResourceItem resourceItem1 = new ResourceItem("testFile1", "content1".getBytes());
        final ResourceItem resourceItem2 = new ResourceItem("testFile2", "content2".getBytes());

        ContentModification moduleAdded = ContentModificationUtils.addModule(oneOffPatchDir, layerPatchID, moduleName, resourceItem1, resourceItem2);
        ProductConfig productConfig = new ProductConfig(PRODUCT, asVersion, "main");
        Patch oneOffPatch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription("A cp patch.")
                .upgradeIdentity(productConfig.getProductName(), productConfig.getProductVersion(), "EAP with cp patch")
                .getParent()
                .upgradeElement(layerPatchID, "base", false)
                .addContentModification(moduleAdded)
                .getParent()
                .build();
        createPatchXMLFile(oneOffPatchDir, oneOffPatch);
        return createZippedPatchFile(oneOffPatchDir, patchID);
    }

    /**
     * Applies one-off that adds a misc file
     * Applies one-off that adds a module
     * Applies CP that adds a module, should invalidate all previously installed one-off
     * does rollback of CP
     * does rollback of all one-offs
     *
     * @throws Exception
     */
    @Test
    public void testTwoOneOffsInvalidatedByCumulativePatch() throws Exception {
        String oneOffPatchID1 = randomString();
        String oneOffPatchID2 = randomString();
        String cpPatchID = randomString();
        File oneOffZip1 = createOneOffPatchAddingMiscFile(oneOffPatchID1, AS_VERSION);
        File oneOffZip2 = createOneOffPatchAddingAModule(oneOffPatchID2, AS_VERSION);
        File cpZip = createCumulativePatch(cpPatchID, AS_VERSION);

        // apply oneoffs
        controller.start(CONTAINER);
        Assert.assertTrue("Patch should be accepted", CliUtilsForPatching.applyPatch(oneOffZip1.getAbsolutePath()));
        Assert.assertTrue("server should be in restart-required mode",
                CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + oneOffPatchID1 + " should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(oneOffPatchID1));
        Assert.assertTrue("Patch should be accepted", CliUtilsForPatching.applyPatch(oneOffZip2.getAbsolutePath()));
        Assert.assertTrue("server should be in restart-required mode",
                CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + oneOffPatchID2 + " should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(oneOffPatchID2));

        // apply cumulative patch
        Assert.assertTrue("Patch should be accepted", CliUtilsForPatching.applyPatch(cpZip.getAbsolutePath()));
        Assert.assertTrue("server should be in restart-required mode",
                CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        controller.start(CONTAINER);
        Assert.assertTrue("The patch " + cpPatchID + " should be listed as installed",
                CliUtilsForPatching.getCumulativePatchId().equalsIgnoreCase(cpPatchID));
        Assert.assertFalse("The patch " + oneOffPatchID1 + " should NOT be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(oneOffPatchID1));
        Assert.assertFalse("The patch " + oneOffPatchID2 + " should NOT be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(oneOffPatchID2));

        // rollback cumulative patch
        Assert.assertTrue("Rollback should be accepted", CliUtilsForPatching.rollbackCumulativePatch(true));
        Assert.assertTrue("server should be in restart-required mode",
                CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        controller.start(CONTAINER);
        Assert.assertFalse("The patch " + cpPatchID + " should NOT be listed as installed",
                CliUtilsForPatching.getCumulativePatchId().equalsIgnoreCase(cpPatchID));
        Assert.assertTrue("The cumulative patch id should be " + BASE,
                CliUtilsForPatching.getCumulativePatchId().equalsIgnoreCase(BASE));
        Assert.assertTrue("The patch " + oneOffPatchID1 + " should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(oneOffPatchID1));
        Assert.assertTrue("The patch " + oneOffPatchID2 + " should be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(oneOffPatchID2));

        //rollback oneoffs
        Assert.assertTrue("Rollback should be accepted", CliUtilsForPatching.rollbackPatch(oneOffPatchID2));
        Assert.assertTrue("server should be in restart-required mode",
                CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        controller.start(CONTAINER);
        Assert.assertFalse("The patch " + oneOffPatchID2 + " should NOT be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(oneOffPatchID2));
        Assert.assertTrue("Rollback should be accepted", CliUtilsForPatching.rollbackPatch(oneOffPatchID1));
        Assert.assertTrue("server should be in restart-required mode",
                CliUtilsForPatching.doesServerRequireRestart());
        controller.stop(CONTAINER);

        controller.start(CONTAINER);
        Assert.assertFalse("The patch " + oneOffPatchID1 + " should NOT be listed as installed",
                CliUtilsForPatching.getInstalledPatches().contains(oneOffPatchID1));
        controller.stop(CONTAINER);
    }


}
