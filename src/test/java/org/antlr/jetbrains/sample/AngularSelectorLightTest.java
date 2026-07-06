package org.antlr.jetbrains.sample;

import com.intellij.psi.PsiElement;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AngularSelectorLightTest extends AngularLightFixtureTestCase {

    @Test
    public void testResolveKnownSelector() {
        myFixture.configureByFile("angular_simple.ts");

        PsiElement target = AngularSelectorIndex.resolveSelector(getProject(), "app-simple");
        assertNotNull(target);
        assertEquals("app-simple", selectorLiteralValue(target));
        assertTrue(target.getContainingFile().getName().endsWith(".ts"));
    }

    @Test
    public void testSkipsCommentedSelectors() {
        myFixture.configureByFile("angular_with_comment.ts");

        assertNull(AngularSelectorIndex.resolveSelector(getProject(), "commented-out"));
        assertNull(AngularSelectorIndex.resolveSelector(getProject(), "also-commented"));

        PsiElement target = AngularSelectorIndex.resolveSelector(getProject(), "app-with-comment");
        assertNotNull(target);
        assertEquals("app-with-comment", selectorLiteralValue(target));
    }

    @Test
    public void testGetAllSelectorsIncludesComponent() {
        myFixture.configureByFile("angular_simple.ts");

        List<String> selectors = AngularSelectorIndex.getAllSelectors(getProject());
        assertTrue(selectors.contains("app-simple"));
    }
}
