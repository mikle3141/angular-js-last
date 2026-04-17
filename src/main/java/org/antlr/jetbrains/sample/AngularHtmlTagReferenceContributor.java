package org.antlr.jetbrains.sample;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.*;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlTokenType;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AngularHtmlTagReferenceContributor extends PsiReferenceContributor {
    private static final Logger LOG = Logger.getInstance(AngularHtmlTagReferenceContributor.class);
    private static final Pattern SELECTOR_PATTERN = Pattern.compile("selector\\s*:\\s*(['\"`])([^'\"]+)\\1");

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(
                XmlPatterns.psiElement(),
                new PsiReferenceProvider() {
                    @Override
                    public @NotNull PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                                                    ProcessingContext context) {
                        LOG.debug("[AngularRef] getReferencesByElement=" + element.getClass().getName() + " text=" + element.getText());
                        IElementType tokenType = element.getNode() != null ? element.getNode().getElementType() : null;
                        if (tokenType != XmlTokenType.XML_NAME && tokenType != XmlTokenType.XML_TAG_NAME) {
                            LOG.debug("[AngularRef] skip tokenType=" + tokenType);
                            return PsiReference.EMPTY_ARRAY;
                        }
                        XmlTag tag = PsiTreeUtil.getParentOfType(element, XmlTag.class, false);
                        if (tag == null) {
                            return PsiReference.EMPTY_ARRAY;
                        }
                        String tagName = element.getText();
                        if (!isComponentTag(tagName)) {
                            LOG.debug("[AngularRef] skip non-component tag=" + tagName);
                            return PsiReference.EMPTY_ARRAY;
                        }
                        PsiFile containingFile = tag.getContainingFile();
                        if (containingFile == null || !containingFile.getName().endsWith(".html")) {
                            LOG.debug("[AngularRef] skip non-html file=" + (containingFile == null ? "null" : containingFile.getName()));
                            return PsiReference.EMPTY_ARRAY;
                        }
                        LOG.debug("[AngularRef] create reference for tag=" + tagName);
                        return new PsiReference[]{new AngularComponentTagReference(element, tagName)};
                    }
                }
        );
    }

    private static boolean isComponentTag(@Nullable String tagName) {
        return tagName != null && !tagName.isEmpty() && tagName.indexOf('-') > 0;
    }

    private static final class AngularComponentTagReference extends PsiReferenceBase<PsiElement> {
        private final String selector;

        public AngularComponentTagReference(@NotNull PsiElement element, @NotNull String selector) {
            super(element, TextRange.from(0, selector.length()));
            this.selector = selector;
        }

        @Override
        public @Nullable PsiElement resolve() {
            Project project = myElement.getProject();
            Collection<VirtualFile> files = FilenameIndex.getAllFilesByExt(project, "ts", GlobalSearchScope.projectScope(project));
            LOG.debug("[AngularRef] resolve selector=" + selector + ", ts files=" + files.size());
            PsiManager psiManager = PsiManager.getInstance(project);

            for (VirtualFile vf : files) {
                PsiFile psi = psiManager.findFile(vf);
                if (psi == null) {
                    continue;
                }
                PsiElement target = findComponentClassBySelector(psi, selector);
                if (target != null) {
                    LOG.debug("[AngularRef] resolved selector=" + selector + " in " + psi.getName());
                    return target;
                }
            }
            LOG.debug("[AngularRef] unresolved selector=" + selector);
            return null;
        }

        @Override
        public @NotNull Object @NotNull [] getVariants() {
            return EMPTY_ARRAY;
        }
    }

    private static @Nullable PsiElement findComponentClassBySelector(@NotNull PsiFile tsFile,
                                                                     @NotNull String targetSelector) {
        String text = tsFile.getText();
        Matcher selectorMatcher = SELECTOR_PATTERN.matcher(text);
        while (selectorMatcher.find()) {
            String selectorValue = selectorMatcher.group(2);
            if (!targetSelector.equals((selectorValue))) {
                continue;
            }
            // Resolve directly to selector value in @Component metadata.
            PsiElement selectorLeaf = tsFile.findElementAt(selectorMatcher.start(2));
            if (selectorLeaf != null) {
                return selectorLeaf;
            }
        }
        return null;
    }
}
