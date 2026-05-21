package org.antlr.jetbrains.sample;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.*;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlTokenType;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AngularHtmlTagReferenceContributor extends PsiReferenceContributor {
    private static final Logger LOG = Logger.getInstance(AngularHtmlTagReferenceContributor.class);
    private static final Pattern SELECTOR_PATTERN = Pattern.compile("selector\\s*:\\s*(['\"`])([^'\"]+)\\1");
    private static final Key<SelectorIndexCache> SELECTOR_INDEX_KEY = Key.create("angular.selector.index");

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
                            // In incomplete HTML (e.g. "<ap"), EDE passes HtmlTagImpl (token HTML_TAG),
                            // so we derive the reference range/name from the whole tag text.
                            tag = xmlTag;
                            tagName = xmlTag.getName();
                            rangeInElement = findTagNameRange(element.getText(), tagName);
                            if (rangeInElement == null) {
                                LOG.debug("[AngularRef] skip: unable to detect tag-name range for tag element");
                                return PsiReference.EMPTY_ARRAY;
                            }
                        } else {
                            IElementType tokenType = element.getNode() != null ? element.getNode().getElementType() : null;
                            if (tokenType != XmlTokenType.XML_NAME && tokenType != XmlTokenType.XML_TAG_NAME) {
                                LOG.debug("[AngularRef] skip tokenType=" + tokenType);
                                return PsiReference.EMPTY_ARRAY;
                            }
                            tag = PsiTreeUtil.getParentOfType(element, XmlTag.class, false);
                            if (tag == null) {
                                return PsiReference.EMPTY_ARRAY;
                            }
                            tagName = element.getText();
                            rangeInElement = TextRange.from(0, tagName.length());
                        }
                        if (tagName == null || tagName.isEmpty()) {
                            return PsiReference.EMPTY_ARRAY;
                        }
                        if (!tagName.contains("-")) {
                            LOG.debug("[AngularRef] skip non-component tag (no hyphen): " + tagName);
                            return PsiReference.EMPTY_ARRAY;
                        }
                        PsiFile containingFile = tag.getContainingFile();
                        if (containingFile == null || !containingFile.getName().endsWith(".html")) {
                            LOG.debug("[AngularRef] skip non-html file=" + (containingFile == null ? "null" : containingFile.getName()));
                            return PsiReference.EMPTY_ARRAY;
                        }
                        LOG.debug("[AngularRef] create reference for tag=" + tagName);
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
            Project project = myElement.getProject();
            PsiElement target = resolveSelector(project, selector);
            if (target != null) {
                LOG.debug("[AngularRef] resolved selector=" + selector);
                return target;
            }
            LOG.debug("[AngularRef] unresolved selector=" + selector);
            return null;
        }

        @Override
        public @NotNull Object @NotNull [] getVariants() {
            Project project = myElement.getProject();
            return getAllSelectors(project).stream()
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
        while (i < text.length() && Character.isWhitespace((text.charAt(i)))) {
            i++;
        }
        int start = i;
        int end = Math.min(start + tagName.length(), text.length());
        if (start >= end) {
            return null;
        }
        return TextRange.create(start, end);
    }

    private static @NotNull SelectorIndex getOrBuildSelectorIndex(@NotNull Project project) {
        long currentModCount = PsiModificationTracker.getInstance(project).getModificationCount();
        SelectorIndexCache cache = project.getUserData(SELECTOR_INDEX_KEY);
        if (cache != null && cache.modificationCount == currentModCount) {
            return cache.index;
        }
        SelectorIndex rebuilt = buildSelectorIndex(project);
        project.putUserData(SELECTOR_INDEX_KEY, new SelectorIndexCache(currentModCount, rebuilt));
        return rebuilt;
    }

    static @NotNull List<String> getAllSelectors(@NotNull Project project) {
        return getOrBuildSelectorIndex(project).allSelectors();
    }

    static @Nullable PsiElement resolveSelector(@NotNull Project project, @NotNull String selector) {
        SelectorIndex index = getOrBuildSelectorIndex(project);
        LOG.debug("[AngularRef] resolveSelector: '" + selector + "', index size=" + index.allSelectors().size() + ", contains=" + index.allSelectors().contains(selector));
        PsiElement result = index.resolve(selector);
        if (result != null) {
            LOG.debug("[AngularRef] resolved selector '" + selector + "' to " + result.getContainingFile().getName() + ":" + result.getTextOffset());
        }
        return result;
    }

    private static @NotNull SelectorIndex buildSelectorIndex(@NotNull Project project) {
        Collection<VirtualFile> files = FilenameIndex.getAllFilesByExt(project, "ts", GlobalSearchScope.projectScope(project));
        LOG.debug("[AngularRef] building selector index from ts files=" + files.size());
        PsiManager psiManager = PsiManager.getInstance(project);
        Map<String, SmartPsiElementPointer<PsiElement>> selectors = new LinkedHashMap<>();

        for (VirtualFile vf : files) {
            PsiFile psi = psiManager.findFile(vf);
            if (psi == null) {
                LOG.debug("[AngularRef] psi == null for file " + vf.getPath());
                continue;
            }
            String text = psi.getText();
            if (!text.contains("selector") || !text.contains("@Component")) {
                LOG.debug("[AngularRef] skipping file " + psi.getName() + " (no selector or @Component)");
                continue;
            }
            LOG.debug("[AngularRef] checking file " + psi.getName() + ", text length=" + text.length());
            collectSelectors(psi, text, selectors);
        }
        LOG.debug("[AngularRef] selector index built size=" + selectors.size());
        return new SelectorIndex(selectors);
    }

    private static void collectSelectors(@NotNull PsiFile tsFile,
                                         @NotNull String text,
                                         @NotNull Map<String, SmartPsiElementPointer<PsiElement>> target) {
        Matcher selectorMatcher = SELECTOR_PATTERN.matcher(text);
        int foundCount = 0;
        int skippedCount = 0;
        while (selectorMatcher.find()) {
            int matchStart = selectorMatcher.start(2);
            int matchEnd = selectorMatcher.end(2);
            String matchedText = selectorMatcher.group(2);
            // Check if this match is inside a comment
            if (isInsideComment(text, matchStart, matchEnd)) {
                LOG.debug("[AngularRef] skipping selector '" + matchedText + "' at [" + matchStart + "," + matchEnd + ") - inside comment");
                skippedCount++;
                continue;
            }
            PsiElement selectorLeaf = tsFile.findElementAt(matchStart);
            if (selectorLeaf != null) {
                String selectorValue = selectorMatcher.group(2);
                target.putIfAbsent(
                        selectorValue,
                        SmartPointerManager.getInstance(tsFile.getProject()).createSmartPsiElementPointer(selectorLeaf)
                );
                LOG.debug("[AngularRef] found selector '" + selectorValue + "' at " + selectorLeaf.getText());
                foundCount++;
            } else {
                LOG.debug("[AngularRef] null element at matchStart=" + matchStart);
            }
        }
        LOG.debug("[AngularRef] collectSelectors: found=" + foundCount + ", skipped=" + skippedCount + " in " + tsFile.getName());
    }

    static boolean isInsideComment(@NotNull String text, int start, int end) {
        int i = 0;
        while (i < text.length()) {
            // Check for block comment
            if (i + 1 < text.length() && text.charAt(i) == '/' && text.charAt(i + 1) == '*') {
                int commentStart = i;
                i += 2;
                while (i + 1 < text.length() && !(text.charAt(i) == '*' && text.charAt(i + 1) == '/')) {
                    i++;
                }
                if (i + 1 < text.length()) {
                    i += 2; // Skip */
                }
                int commentEnd = i;
                // Check if [start, end) overlaps with [commentStart, commentEnd)
                if (start < commentEnd && end > commentStart) {
                    return true;
                }
                continue;
            }
            // Check for line comment
            if (i + 1 < text.length() && text.charAt(i) == '/' && text.charAt(i + 1) == '/') {
                int commentStart = i;
                // Skip until end of line
                while (i < text.length() && text.charAt(i) != '\n') {
                    i++;
                }
                if (i < text.length()) {
                    i++; // Skip newline
                }
                int commentEnd = i;
                // Check if [start, end) overlaps with [commentStart, commentEnd)
                if (start < commentEnd && end > commentStart) {
                    return true;
                }
                continue;
            }
            // Skip strings - we don't need to check them because the regex already matches
            // only content inside quotes, and those quotes are not inside comments.
            if (text.charAt(i) == '`' || text.charAt(i) == '\'' || text.charAt(i) == '"') {
                char quote = text.charAt(i);
                i++;
                while (i < text.length()) {
                    char c = text.charAt(i);
                    if (c == '\\' && i + 1 < text.length()) {
                        i += 2; // Skip escaped char
                    } else if (c == quote) {
                        i++;
                        break;
                    } else {
                        i++;
                    }
                }
                continue;
            }
            i++;
        }
        return false;
    }

    private static final class SelectorIndex {
        private final Map<String, SmartPsiElementPointer<PsiElement>> selectors;

        private SelectorIndex(@NotNull Map<String, SmartPsiElementPointer<PsiElement>> selectors) {
            this.selectors = selectors;
        }

        private @Nullable PsiElement resolve(@NotNull String selector) {
            SmartPsiElementPointer<PsiElement> pointer = selectors.get(selector);
            if (pointer == null) {
                LOG.debug("[AngularRef] selector '" + selector + "' not found in index");
                return null;
            }
            PsiElement element = pointer.getElement();
            if (element == null) {
                LOG.debug("[AngularRef] pointer for selector '" + selector + "' returned null element (stale reference)");
            }
            return element;
        }

        private @NotNull List<String> allSelectors() {
            if (selectors.isEmpty()) {
                return Collections.emptyList();
            }
            return selectors.keySet().stream().sorted().collect(Collectors.toList());
        }
    }

    private static final class SelectorIndexCache {
        private final long modificationCount;
        private final SelectorIndex index;

        private SelectorIndexCache(long modificationCount, @NotNull SelectorIndex index) {
            this.modificationCount = modificationCount;
            this.index = index;
        }
    }
}
