package com.gigaide.angular;

import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AngularResourceGotoLightTest extends AngularLightFixtureTestCase {

    @Test
    public void testHtmlFileGotoTemplateUrlInComponent() {
        myFixture.copyFileToProject("angular_external.ts");
        myFixture.configureByFile("external.component.html");

        PsiElement[] targets = gotoTargetsAt(0);
        assertNotNull(targets);
        assertTrue(targets.length > 0);
        assertEquals("./external.component.html", selectorLiteralValue(targets[0]));
        assertTrue(targets[0].getContainingFile().getName().endsWith("angular_external.ts"));
    }

    @Test
    public void testCssFileGotoStyleUrlsInComponent() {
        myFixture.copyFileToProject("angular_external.ts");
        myFixture.configureByFile("external.component.css");

        PsiElement[] targets = gotoTargetsAt(0);
        assertNotNull(targets);
        assertTrue(targets.length > 0);
        assertEquals("./external.component.css", selectorLiteralValue(targets[0]));
        assertTrue(targets[0].getContainingFile().getName().endsWith("angular_external.ts"));
    }

    private PsiElement[] gotoTargetsAt(int offset) {
        PsiFile file = myFixture.getFile();
        Editor editor = myFixture.getEditor();
        PsiElement element = file.findElementAt(offset);
        AngularResourceGotoDeclarationHandler handler = new AngularResourceGotoDeclarationHandler();
        return handler.getGotoDeclarationTargets(element, offset, editor);
    }
}
