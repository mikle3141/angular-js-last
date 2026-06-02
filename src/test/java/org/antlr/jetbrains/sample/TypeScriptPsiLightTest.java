package org.antlr.jetbrains.sample;

import com.intellij.psi.util.PsiTreeUtil;
import org.antlr.jetbrains.sample.psi.FunctionSubtree;
import org.antlr.jetbrains.sample.psi.VardefSubtree;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TypeScriptPsiLightTest extends TypeScriptLightFixtureTestCase {

    @Test
    public void testFunctionDeclarationCreatesFunctionSubtree() {
        myFixture.configureByFile("function_decl.ts");

        Collection<FunctionSubtree> functions = PsiTreeUtil.findChildrenOfType(myFixture.getFile(), FunctionSubtree.class);
        assertEquals(1, functions.size());
        assertEquals("foo", functions.iterator().next().getName());
    }

    @Test
    public void testVariableDeclarationCreatesVardefSubtree() {
        myFixture.configureByFile("simple_var.ts");

        Collection<VardefSubtree> variables = PsiTreeUtil.findChildrenOfType(myFixture.getFile(), VardefSubtree.class);
        assertEquals(1, variables.size());
        assertEquals("x", variables.iterator().next().getName());
    }
}
