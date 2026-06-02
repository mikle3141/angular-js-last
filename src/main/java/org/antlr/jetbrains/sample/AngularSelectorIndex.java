package org.antlr.jetbrains.sample;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiModificationTracker;
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

/**
 * Единый кэшируемый индекс Angular selector'ов по всем {@code .ts} файлам проекта.
 * Используется для навигации, references и автодополнения в HTML-шаблонах.
 */
public final class AngularSelectorIndex {
    private static final Logger LOG = Logger.getInstance(AngularSelectorIndex.class);
    private static final Pattern SELECTOR_PATTERN = Pattern.compile("selector\\s*:\\s*(['\"`])([^'\"]+)\\1");
    private static final Key<SelectorIndexCache> SELECTOR_INDEX_KEY = Key.create("angular.selector.index");

    private AngularSelectorIndex() {
    }

    @NotNull
    public static List<String> getAllSelectors(@NotNull Project project) {
        return getOrBuildIndex(project).allSelectors();
    }

    @Nullable
    public static PsiElement resolveSelector(@NotNull Project project, @NotNull String selector) {
        SelectorIndex index = getOrBuildIndex(project);
        LOG.debug("[AngularSelector] resolveSelector: '" + selector + "', index size=" + index.allSelectors().size());
        PsiElement result = index.resolve(selector);
        if (result != null) {
            LOG.debug("[AngularSelector] resolved selector '" + selector + "' to "
                    + result.getContainingFile().getName() + ":" + result.getTextOffset());
        }
        return result;
    }

    /**
     * Проверяет, попадает ли диапазон {@code [start, end)} внутрь комментария или строкового литерала.
     */
    static boolean isInsideComment(@NotNull String text, int start, int end) {
        int i = 0;
        while (i < text.length()) {
            if (i + 1 < text.length() && text.charAt(i) == '/' && text.charAt(i + 1) == '*') {
                int commentStart = i;
                i += 2;
                while (i + 1 < text.length() && !(text.charAt(i) == '*' && text.charAt(i + 1) == '/')) {
                    i++;
                }
                if (i + 1 < text.length()) {
                    i += 2;
                }
                if (start < i && end > commentStart) {
                    return true;
                }
                continue;
            }
            if (i + 1 < text.length() && text.charAt(i) == '/' && text.charAt(i + 1) == '/') {
                int commentStart = i;
                while (i < text.length() && text.charAt(i) != '\n') {
                    i++;
                }
                if (i < text.length()) {
                    i++;
                }
                if (start < i && end > commentStart) {
                    return true;
                }
                continue;
            }
            if (text.charAt(i) == '`' || text.charAt(i) == '\'' || text.charAt(i) == '"') {
                char quote = text.charAt(i);
                i++;
                while (i < text.length()) {
                    char c = text.charAt(i);
                    if (c == '\\' && i + 1 < text.length()) {
                        i += 2;
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

    private static @NotNull SelectorIndex getOrBuildIndex(@NotNull Project project) {
        long currentModCount = PsiModificationTracker.getInstance(project).getModificationCount();
        SelectorIndexCache cache = project.getUserData(SELECTOR_INDEX_KEY);
        if (cache != null && cache.modificationCount == currentModCount) {
            return cache.index;
        }
        SelectorIndex rebuilt = buildIndex(project);
        project.putUserData(SELECTOR_INDEX_KEY, new SelectorIndexCache(currentModCount, rebuilt));
        return rebuilt;
    }

    private static @NotNull SelectorIndex buildIndex(@NotNull Project project) {
        Collection<VirtualFile> files = FilenameIndex.getAllFilesByExt(project, "ts", GlobalSearchScope.projectScope(project));
        LOG.debug("[AngularSelector] building index from ts files=" + files.size());
        PsiManager psiManager = PsiManager.getInstance(project);
        Map<String, SmartPsiElementPointer<PsiElement>> selectors = new LinkedHashMap<>();

        for (VirtualFile vf : files) {
            PsiFile psi = psiManager.findFile(vf);
            if (psi == null) {
                continue;
            }
            String text = psi.getText();
            if (!text.contains("selector") || !text.contains("@Component")) {
                continue;
            }
            collectSelectors(psi, text, selectors);
        }
        LOG.debug("[AngularSelector] index built size=" + selectors.size());
        return new SelectorIndex(selectors);
    }

    private static void collectSelectors(@NotNull PsiFile tsFile,
                                         @NotNull String text,
                                         @NotNull Map<String, SmartPsiElementPointer<PsiElement>> target) {
        Matcher selectorMatcher = SELECTOR_PATTERN.matcher(text);
        while (selectorMatcher.find()) {
            int matchStart = selectorMatcher.start(2);
            int matchEnd = selectorMatcher.end(2);
            String selectorValue = selectorMatcher.group(2);
            if (isInsideComment(text, matchStart, matchEnd)) {
                LOG.debug("[AngularSelector] skipping selector '" + selectorValue + "' inside comment");
                continue;
            }
            PsiElement selectorLeaf = tsFile.findElementAt(matchStart);
            if (selectorLeaf != null) {
                target.putIfAbsent(
                        selectorValue,
                        SmartPointerManager.getInstance(tsFile.getProject()).createSmartPsiElementPointer(selectorLeaf)
                );
            }
        }
    }

    private static final class SelectorIndex {
        private final Map<String, SmartPsiElementPointer<PsiElement>> selectors;

        private SelectorIndex(@NotNull Map<String, SmartPsiElementPointer<PsiElement>> selectors) {
            this.selectors = selectors;
        }

        private @Nullable PsiElement resolve(@NotNull String selector) {
            SmartPsiElementPointer<PsiElement> pointer = selectors.get(selector);
            return pointer == null ? null : pointer.getElement();
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
