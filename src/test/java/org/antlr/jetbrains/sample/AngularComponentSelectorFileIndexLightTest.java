package org.antlr.jetbrains.sample;

import com.intellij.util.indexing.FileBasedIndex;
import org.antlr.jetbrains.sample.index.AngularComponentSelectorFileIndex;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AngularComponentSelectorFileIndexLightTest extends AngularLightFixtureTestCase {

    @Test
    public void testFileIndexContainsSelectorFromComponent() throws Exception {
        myFixture.configureByFile("angular_simple.ts");
        FileBasedIndex.getInstance().ensureUpToDate(
                AngularComponentSelectorFileIndex.NAME,
                getProject(),
                null
        );

        assertNotNull(AngularSelectorIndex.resolveSelector(getProject(), "app-simple"));
    }
}
