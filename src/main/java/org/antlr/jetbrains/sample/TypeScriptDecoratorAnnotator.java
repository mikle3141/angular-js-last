package org.antlr.jetbrains.sample;

import com.gigaide.javascript.TsLanguage;
import com.gigaide.javascript.psi.js.JsCallExpression;
import com.gigaide.javascript.psi.js.JsDecorator;
import com.gigaide.javascript.psi.js.JsExpression;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Подсвечивает имена декораторов (@Component и т.п.) синим цветом.
 */
public class TypeScriptDecoratorAnnotator implements Annotator {

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (!(element instanceof PsiFile file)) {
            return;
        }
        if (!file.getLanguage().is(TsLanguage.INSTANCE)) {
            return;
        }

        for (JsDecorator decorator : PsiTreeUtil.findChildrenOfType(file, JsDecorator.class)) {
            JsCallExpression call = PsiTreeUtil.findChildOfType(decorator, JsCallExpression.class);
            if (call == null) {
                continue;
            }
            JsExpression callee = call.getExpression();
            if (callee == null) {
                continue;
            }
            Annotation ann = holder.createInfoAnnotation(callee.getTextRange(), null);
            ann.setTextAttributes(AngularHighlighterKeys.DECORATOR);
        }
    }
}
