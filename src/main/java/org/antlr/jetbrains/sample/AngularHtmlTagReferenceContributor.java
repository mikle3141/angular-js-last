package org.antlr.jetbrains.sample;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlTokenType;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AngularHtmlTagReferenceContributor extends PsiReferenceContributor {
    private static final Logger LOG = Logger.getInstance(AngularHtmlTagReferenceContributor.class);

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(
                XmlPatterns.psiElement(),
                new PsiReferenceProvider() {
                    @Override
                    public @NotNull PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                                                    ProcessingContext context) {
                        LOG.debug("[AngularRef] getReferencesByElement=" + element.getClass().getName() + " text=" + element.getText());
                        XmlTag tag;
                        String tagName;
                        TextRange rangeInElement;

                        if (element instanceof XmlTag xmlTag) {
                            tag = xmlTag;
                            tagName = xmlTag.getName();
                            rangeInElement = findTagNameRange(element.getText(), tagName);
                            if (rangeInElement == null) {
                                return PsiReference.EMPTY_ARRAY;
                            }
                        } else {
                            IElementType tokenType = element.getNode() != null ? element.getNode().getElementType() : null;
                            if (tokenType != XmlTokenType.XML_NAME && tokenType != XmlTokenType.XML_TAG_NAME) {
                                return PsiReference.EMPTY_ARRAY;
                            }
                            tag = PsiTreeUtil.getParentOfType(element, XmlTag.class, false);
                            if (tag == null) {
                                return PsiReference.EMPTY_ARRAY;
                            }
                            tagName = element.getText();
                            rangeInElement = TextRange.from(0, tagName.length());
                        }
                        if (tagName == null || tagName.isEmpty() || !tagName.contains("-")) {
                            return PsiReference.EMPTY_ARRAY;
                        }
                        PsiFile containingFile = tag.getContainingFile();
                        if (containingFile == null || !containingFile.getName().endsWith(".html")) {
                            return PsiReference.EMPTY_ARRAY;
                        }
                        return new PsiReference[]{new AngularComponentTagReference(element, tagName, rangeInElement)};
                    }
                }
        );
    }

    private static final class AngularComponentTagReference extends PsiReferenceBase<PsiElement> {
        private final String selector;

        public AngularComponentTagReference(@NotNull PsiElement element,
                                            @NotNull String selector,
                                            @NotNull TextRange rangeInElement) {
            super(element, rangeInElement);
            this.selector = selector;
        }

        @Override
        public @Nullable PsiElement resolve() {
            return AngularSelectorIndex.resolveSelector(myElement.getProject(), selector);
        }

        @Override
        public @NotNull Object @NotNull [] getVariants() {
            Project project = myElement.getProject();
            return AngularSelectorIndex.getAllSelectors(project).stream()
                    .map(LookupElementBuilder::create)
                    .toArray();
        }
    }

    private static @Nullable TextRange findTagNameRange(@NotNull String text, @NotNull String tagName) {
        if (tagName.isEmpty()) {
            return null;
        }
        int lt = text.indexOf('<');
        if (lt < 0) {
            return null;
        }
        int i = lt + 1;
        if (i < text.length() && text.charAt(i) == '/') {
            i++;
        }
        while (i < text.length() && Character.isWhitespace(text.charAt(i))) {
            i++;
        }
        int start = i;
        int end = Math.min(start + tagName.length(), text.length());
        if (start >= end) {
            return null;
        }
        return TextRange.create(start, end);
    }
}
