package org.antlr.jetbrains.sample;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AngularComponentMetadataReferenceLightTest extends TypeScriptLightFixtureTestCase {

    @Test
    public void testTemplateUrlResolvesToHtmlFile() {
        myFixture.copyFileToProject("external.component.html");
        myFixture.configureByFile("angular_external.ts");

        PsiReference reference = findResourceReference("./external.component.html");
        assertNotNull(reference);

        PsiElement resolved = reference.resolve();
        assertNotNull(resolved);
        assertTrue(resolved instanceof PsiFile);
        assertEquals("external.component.html", ((PsiFile) resolved).getName());
    }

    @Test
    public void testStyleUrlsResolvesToCssFile() {
        myFixture.copyFileToProject("external.component.css");
        myFixture.configureByFile("angular_external.ts");

        PsiReference reference = findResourceReference("./external.component.css");
        assertNotNull(reference);

        PsiElement resolved = reference.resolve();
        assertNotNull(resolved);
        assertTrue(resolved instanceof PsiFile);
        assertEquals("external.component.css", ((PsiFile) resolved).getName());
    }

    private PsiReference findResourceReference(String pathFragment) {
        PsiFile file = myFixture.getFile();
        int offset = file.getText().indexOf(pathFragment);
        assertTrue("path fragment in file: " + pathFragment, offset >= 0);
        offset += 2;
        return file.findReferenceAt(offset);
    }
}
