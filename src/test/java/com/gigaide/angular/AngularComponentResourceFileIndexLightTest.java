package com.gigaide.angular;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.indexing.FileBasedIndex;
import com.gigaide.angular.index.AngularComponentResourceFileIndex;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AngularComponentResourceFileIndexLightTest extends AngularLightFixtureTestCase {

    @Test
    public void testFileIndexResolvesTemplateUrlFromHtml() throws Exception {
        myFixture.copyFileToProject("angular_external.ts");
        VirtualFile htmlVf = myFixture.copyFileToProject("external.component.html");
        FileBasedIndex.getInstance().ensureUpToDate(
                AngularComponentResourceFileIndex.NAME,
                getProject(),
                null
        );

        var targets = AngularComponentResourceIndex.resolveAllResourceReferences(getProject(), htmlVf);
        assertFalse(targets.isEmpty());
        assertEquals("./external.component.html", selectorLiteralValue(targets.get(0)));
    }

    @Test
    public void testFileIndexResolvesStyleUrlsFromCss() throws Exception {
        myFixture.copyFileToProject("angular_external.ts");
        VirtualFile cssVf = myFixture.copyFileToProject("external.component.css");
        FileBasedIndex.getInstance().ensureUpToDate(
                AngularComponentResourceFileIndex.NAME,
                getProject(),
                null
        );

        var targets = AngularComponentResourceIndex.resolveAllResourceReferences(getProject(), cssVf);
        assertFalse(targets.isEmpty());
        assertEquals("./external.component.css", selectorLiteralValue(targets.get(0)));
    }
}
