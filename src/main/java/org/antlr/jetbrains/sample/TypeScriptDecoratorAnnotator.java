package org.antlr.jetbrains.sample;

import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.antlr.jetbrains.sample.psi.IdentifierPSINode;
import org.jetbrains.annotations.NotNull;

/**
 * Подсвечивает декораторы (@Identifier) синим цветом.
 * Идентификатор сразу после @ считается именем декоратора.
 */
public class TypeScriptDecoratorAnnotator implements Annotator {

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (!(element instanceof PsiFile)) return;
        PsiFile file = (PsiFile) element;
        if (!file.getLanguage().is(TypeScriptLanguage.INSTANCE)) return;

        for (IdentifierPSINode id : PsiTreeUtil.findChildrenOfType(file, IdentifierPSINode.class)) {
            PsiElement prev = PsiTreeUtil.prevLeaf(id, true);
            if (prev != null && "@".equals(prev.getText())) {
                Annotation ann = holder.createInfoAnnotation(id.getTextRange(), null);
                ann.setTextAttributes(TypeScriptSyntaxHighlighter.DECORATOR);
            }
        }
    }
}
