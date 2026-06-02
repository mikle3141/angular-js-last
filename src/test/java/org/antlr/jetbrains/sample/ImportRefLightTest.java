package org.antlr.jetbrains.sample;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import org.antlr.jetbrains.sample.psi.IdentifierPSINode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ImportRefLightTest extends TypeScriptLightFixtureTestCase {

    @Test
    public void testImportResolvesToExportedFunction() {
        myFixture.copyFileToProject("module.ts");
        myFixture.configureByFile("import_export.ts");

        IdentifierPSINode fooImport = findImportedIdentifier("foo");
        assertNotNull(fooImport);

        PsiReference reference = fooImport.getReference();
        assertNotNull(reference);

        PsiElement resolved = reference.resolve();
        assertNotNull(resolved);
        assertEquals("foo", resolved.getText());
        assertTrue(resolved.getContainingFile().getName().endsWith("module.ts"));
    }

    private IdentifierPSINode findImportedIdentifier(String name) {
        for (IdentifierPSINode id : PsiTreeUtil.findChildrenOfType(myFixture.getFile(), IdentifierPSINode.class)) {
            if (name.equals(id.getText()) && TypeScriptPsiUtil.isInsideImportStatement(id)) {
                return id;
            }
        }
        return null;
    }
}
