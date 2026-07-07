package com.gigaide.angular;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AngularCreature3NavigationLightTest extends AngularLightFixtureTestCase {

    @Test
    public void testIndexHtmlNavigatesToAppRootSelector() {
        myFixture.copyFileToProject("creature3_app.ts");
        myFixture.configureByFile("creature3_index.html");

        int offset = myFixture.getFile().getText().indexOf("app-root") + 2;
        assertTrue(offset > 0);

        PsiReference reference = myFixture.getFile().findReferenceAt(offset);
        assertNotNull(reference);

        PsiElement resolved = reference.resolve();
        assertNotNull(resolved);
        assertEquals("'app-root'", resolved.getText());
        assertTrue(resolved.getContainingFile().getName().endsWith("creature3_app.ts"));
    }

    @Test
    public void testSelectorIndexFindsAppRootFromCreature3App() {
        myFixture.copyFileToProject("creature3_app.ts");

        PsiElement target = AngularSelectorIndex.resolveSelector(getProject(), "app-root");
        assertNotNull(target);
        assertTrue(target.getText().contains("app-root"));
        assertTrue(target.getContainingFile().getName().endsWith("creature3_app.ts"));
    }
}
