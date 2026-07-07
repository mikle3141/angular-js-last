package com.gigaide.angular;

import com.intellij.psi.PsiElement;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AngularPsiUtilLightTest extends AngularLightFixtureTestCase {

    @Test
    public void testCreature3AppTsFindsAppRootSelector() {
        myFixture.configureByFile("creature3_app.ts");
        assertTrue(AngularPsiUtil.hasComponentDecorators(myFixture.getFile()));

        final String[] selector = new String[1];
        AngularPsiUtil.collectComponentSelectors(myFixture.getFile(), (s, literal) -> {
            if ("app-root".equals(s)) {
                selector[0] = s;
            }
        });
        assertNotNull("app-root selector should be indexed from creature3 app.ts", selector[0]);
    }

    @Test
    public void testFindsTemplateUrlAndStyleUrlsLiterals() {
        myFixture.configureByFile("angular_external.ts");

        final PsiElement[] templateUrl = new PsiElement[1];
        final int[] styleUrlCount = new int[1];
        AngularPsiUtil.collectComponentResourceLiterals(myFixture.getFile(), (literal, property) -> {
            if (AngularPsiUtil.METADATA_TEMPLATE_URL.equals(property)) {
                templateUrl[0] = literal;
            }
            if (AngularPsiUtil.METADATA_STYLE_URLS.equals(property)) {
                styleUrlCount[0]++;
            }
        });
        assertNotNull(templateUrl[0]);
        assertEquals("./external.component.html", selectorLiteralValue(templateUrl[0]));
        assertEquals(2, styleUrlCount[0]);
    }

    @Test
    public void testFindsComponentDecoratorInPsiTree() {
        myFixture.configureByFile("angular_simple.ts");

        assertTrue(AngularPsiUtil.hasComponentDecorators(myFixture.getFile()));

        final PsiElement[] selectorLiteral = new PsiElement[1];
        AngularPsiUtil.collectComponentSelectors(myFixture.getFile(), (selector, literal) -> {
            if ("app-simple".equals(selector)) {
                selectorLiteral[0] = literal;
            }
        });
        assertNotNull(selectorLiteral[0]);
    }
}
