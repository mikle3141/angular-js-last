package org.antlr.jetbrains.sample;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.antlr.jetbrains.sample.psi.IdentifierPSINode;
import org.antlr.jetbrains.sample.psi.TypeScriptPSIFileRoot;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TypeScriptPsiUtilLightTest extends TypeScriptLightFixtureTestCase {

    @Test
    public void testFindsTemplateUrlAndStyleUrlsLiterals() {
        myFixture.configureByFile("angular_external.ts");

        final PsiElement[] templateUrl = new PsiElement[1];
        final int[] styleUrlCount = new int[1];
        TypeScriptPsiUtil.collectComponentResourceLiterals(myFixture.getFile(), (literal, property) -> {
            if (TypeScriptPsiUtil.METADATA_TEMPLATE_URL.equals(property)) {
                templateUrl[0] = literal;
            }
            if (TypeScriptPsiUtil.METADATA_STYLE_URLS.equals(property)) {
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
        TypeScriptPSIFileRoot file = (TypeScriptPSIFileRoot) myFixture.getFile();

        assertTrue(TypeScriptPsiUtil.hasComponentDecorators(file));

        final PsiElement[] selectorLiteral = new PsiElement[1];
        TypeScriptPsiUtil.collectComponentSelectors(file, (selector, literal) -> {
            if ("app-simple".equals(selector)) {
                selectorLiteral[0] = literal;
            }
        });
        assertNotNull(selectorLiteral[0]);
    }

    @Test
    public void testExportedFunctionDetectedInModule() {
        var moduleVf = myFixture.copyFileToProject("module.ts");
        var moduleFile = (TypeScriptPSIFileRoot) myFixture.getPsiManager().findFile(moduleVf);
        assertNotNull(moduleFile);

        PsiElement foo = TypeScriptPsiUtil.findExportedIdentifier(moduleFile, "foo");
        assertNotNull(foo);
        assertEquals("foo", foo.getText());
    }

    @Test
    public void testImportModulePathViaPsi() {
        myFixture.configureByFile("import_export.ts");

        IdentifierPSINode foo = null;
        for (IdentifierPSINode id : PsiTreeUtil.findChildrenOfType(myFixture.getFile(), IdentifierPSINode.class)) {
            if ("foo".equals(id.getText()) && TypeScriptPsiUtil.isInsideImportStatement(id)) {
                foo = id;
                break;
            }
        }
        assertNotNull(foo);
        assertTrue(TypeScriptPsiUtil.isInsideImportStatement(foo));

        PsiElement importStatement = TypeScriptPsiUtil.findImportStatementAncestor(foo);
        assertNotNull(importStatement);
        assertEquals("./module", TypeScriptPsiUtil.getImportModulePath(importStatement));
    }
}
