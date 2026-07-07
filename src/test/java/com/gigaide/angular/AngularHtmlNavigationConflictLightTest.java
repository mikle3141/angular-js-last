package com.gigaide.angular;

import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AngularHtmlNavigationConflictLightTest extends AngularLightFixtureTestCase {

    @Test
    public void testComponentTagInTemplateUrlFileUsesSelectorNavigation() {
        myFixture.copyFileToProject("angular_external.ts");
        myFixture.copyFileToProject("angular_simple.ts");
        myFixture.configureByFile("external.component.html");

        int offset = myFixture.getFile().getText().indexOf("app-simple") + 2;
        assertTrue(offset > 0);

        PsiReference reference = myFixture.getFile().findReferenceAt(offset);
        assertNotNull(reference);
        PsiElement resolved = reference.resolve();
        assertNotNull(resolved);
        assertEquals("app-simple", selectorLiteralValue(resolved));

        AngularResourceGotoDeclarationHandler resourceHandler = new AngularResourceGotoDeclarationHandler();
        assertNull(resourceHandler.getGotoDeclarationTargets(
                myFixture.getFile().findElementAt(offset), offset, myFixture.getEditor()));

        AngularHtmlGotoDeclarationHandler tagHandler = new AngularHtmlGotoDeclarationHandler();
        PsiElement[] tagTargets = tagHandler.getGotoDeclarationTargets(
                myFixture.getFile().findElementAt(offset), offset, myFixture.getEditor());
        assertNotNull(tagTargets);
        assertEquals("app-simple", selectorLiteralValue(tagTargets[0]));
    }

    @Test
    public void testNonTagClickInTemplateUrlFileUsesResourceNavigation() {
        myFixture.copyFileToProject("angular_external.ts");
        myFixture.configureByFile("external.component.html");

        int offset = myFixture.getFile().getText().indexOf("external");
        assertTrue(offset > 0);

        AngularResourceGotoDeclarationHandler resourceHandler = new AngularResourceGotoDeclarationHandler();
        PsiElement[] resourceTargets = resourceHandler.getGotoDeclarationTargets(
                myFixture.getFile().findElementAt(offset), offset, myFixture.getEditor());
        assertNotNull(resourceTargets);
        assertEquals("./external.component.html", selectorLiteralValue(resourceTargets[0]));
    }
}
