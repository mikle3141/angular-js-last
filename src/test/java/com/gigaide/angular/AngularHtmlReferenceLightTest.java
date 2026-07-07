package com.gigaide.angular;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AngularHtmlReferenceLightTest extends AngularLightFixtureTestCase {

    @Test
    public void testHtmlTagReferenceResolvesToComponent() {
        myFixture.copyFileToProject("angular_simple.ts");
        myFixture.configureByFile("template.html");

        PsiFile htmlFile = myFixture.getFile();
        int offset = htmlFile.getText().indexOf("app-simple") + 2;
        assertTrue("offset inside tag name", offset > 0);

        PsiReference reference = htmlFile.findReferenceAt(offset);
        assertNotNull("reference at tag name", reference);

        PsiElement resolved = reference.resolve();
        assertNotNull("selector resolves to TS source", resolved);
        assertEquals("app-simple", selectorLiteralValue(resolved));
        assertTrue(resolved.getContainingFile().getName().endsWith(".ts"));
    }
}
