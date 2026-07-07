package com.gigaide.angular;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AngularHtmlGotoDeclarationHandler implements GotoDeclarationHandler {
    private static final Logger LOG = Logger.getInstance(AngularHtmlGotoDeclarationHandler.class);

    @Override
    public PsiElement @Nullable [] getGotoDeclarationTargets(@Nullable PsiElement sourceElement,
                                                             int offset,
                                                             Editor editor) {
        if (sourceElement == null) {
            return null;
        }
        String selector = AngularHtmlNavigationUtil.findComponentTagNameAt(sourceElement, offset, editor);
        if (selector == null) {
            return null;
        }
        PsiFile htmlFile = sourceElement.getContainingFile();
        if (htmlFile == null || !htmlFile.getName().endsWith(".html")) {
            return null;
        }

        PsiElement target = AngularSelectorIndex.resolveSelector(sourceElement.getProject(), selector);
        if (target == null) {
            LOG.debug("[AngularGoto] selector not resolved: " + selector);
            return null;
        }
        return new PsiElement[]{target};
    }
}
