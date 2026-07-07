package com.gigaide.angular;

import com.gigaide.javascript.TsLanguage;
import com.gigaide.javascript.psi.js.JsLiteralExpression;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.util.ProcessingContext;
import com.gigaide.angular.psi.AngularComponentResourceReference;
import org.jetbrains.annotations.NotNull;

public class AngularComponentResourceReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(
                PlatformPatterns.psiElement(JsLiteralExpression.class).withLanguage(TsLanguage.INSTANCE),
                new PsiReferenceProvider() {
                    @Override
                    public PsiReference @NotNull [] getReferencesByElement(@NotNull com.intellij.psi.PsiElement element,
                                                                           @NotNull ProcessingContext context) {
                        if (!AngularPsiUtil.isComponentResourceUrlLiteral(element)) {
                            return PsiReference.EMPTY_ARRAY;
                        }
                        return new PsiReference[]{
                                new AngularComponentResourceReference(
                                        element,
                                        AngularComponentResourceReference.quotedContentRange(element)
                                )
                        };
                    }
                }
        );
    }
}
