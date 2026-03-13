package org.antlr.jetbrains.sample;

import com.intellij.lang.cacheBuilder.WordsScanner;
import com.intellij.lang.findUsages.FindUsagesProvider;
import com.intellij.psi.PsiElement;
import org.antlr.intellij.adaptor.lexer.RuleIElementType;
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode;
import org.antlr.jetbrains.sample.psi.IdentifierPSINode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.antlr.jetbrains.sample.parser.TypeScriptParser.RULE_arguments;
import static org.antlr.jetbrains.sample.parser.TypeScriptParser.RULE_functionDeclaration;
import static org.antlr.jetbrains.sample.parser.TypeScriptParser.RULE_singleExpression;
import static org.antlr.jetbrains.sample.parser.TypeScriptParser.RULE_variableDeclaration;

public class TypeScriptFindUsagesProvider implements FindUsagesProvider {
    @Override
    public boolean canFindUsagesFor(PsiElement psiElement) {
        return psiElement instanceof IdentifierPSINode;
    }

    @Nullable
    @Override
    public WordsScanner getWordsScanner() {
        return null;
    }

    @Nullable
    @Override
    public String getHelpId(PsiElement psiElement) {
        return null;
    }

    @NotNull
    @Override
    public String getType(PsiElement element) {
        PsiElement parent = element.getParent();
        if (parent instanceof ANTLRPsiNode && parent.getNode().getElementType() instanceof RuleIElementType) {
            int ruleIndex = ((RuleIElementType) parent.getNode().getElementType()).getRuleIndex();
            if (ruleIndex == RULE_functionDeclaration) return "function";
            if (ruleIndex == RULE_variableDeclaration) return "variable";
            if (ruleIndex == RULE_arguments) return "function";
            if (ruleIndex == RULE_singleExpression) return "variable";
        }
        return "identifier";
    }

    @NotNull
    @Override
    public String getDescriptiveName(PsiElement element) {
        return element.getText();
    }

    @NotNull
    @Override
    public String getNodeText(PsiElement element, boolean useFullName) {
        return element.getText();
    }
}
