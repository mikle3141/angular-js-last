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
import com.intellij.util.indexing.FileBasedIndex;
import org.antlr.jetbrains.sample.index.AngularComponentSelectorFileIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Единый кэшируемый индекс Angular selector'ов по всем {@code .ts} файлам проекта.
 */
public final class AngularSelectorIndex {
    private static final Logger LOG = Logger.getInstance(AngularSelectorIndex.class);
    private static final Key<SelectorIndexCache> SELECTOR_INDEX_KEY = Key.create("angular.selector.index");

    private AngularSelectorIndex() {
    }

    @NotNull
    public static List<String> getAllSelectors(@NotNull Project project) {
        List<String> fromFileIndex = collectAllSelectorsFromFileIndex(project);
        if (!fromFileIndex.isEmpty()) {
            return fromFileIndex;
        }
        return getOrBuildScanIndex(project).allSelectors();
    }

    @Nullable
    public static PsiElement resolveSelector(@NotNull Project project, @NotNull String selector) {
        PsiElement fromFileIndex = resolveSelectorFromFileIndex(project, selector);
        if (fromFileIndex != null) {
            return fromFileIndex;
        }
        SelectorIndex index = getOrBuildScanIndex(project);
        LOG.debug("[AngularSelector] resolveSelector: '" + selector + "', scan index size=" + index.allSelectors().size());
        PsiElement result = index.resolve(selector);
        if (result != null) {
            LOG.debug("[AngularSelector] resolved selector '" + selector + "' to "
                    + result.getContainingFile().getName() + ":" + result.getTextOffset());
        }
        return result;
    }

    @Nullable
    private static PsiElement resolveSelectorFromFileIndex(@NotNull Project project, @NotNull String selector) {
        GlobalSearchScope scope = GlobalSearchScope.projectScope(project);
        Collection<VirtualFile> files = FileBasedIndex.getInstance()
                .getContainingFiles(AngularComponentSelectorFileIndex.NAME, selector, scope);
        if (files.isEmpty()) {
            return null;
        }
        PsiManager psiManager = PsiManager.getInstance(project);
        for (VirtualFile vf : files) {
            if (!AngularIndexScope.isAngularIndexableFile(project, vf)) {
                continue;
            }
            PsiFile file = psiManager.findFile(vf);
            if (file == null) {
                continue;
            }
            PsiElement[] target = new PsiElement[1];
            AngularPsiUtil.collectComponentSelectors(file, (s, literal) -> {
                if (selector.equals(s) && target[0] == null) {
                    target[0] = literal;
                }
            });
            if (target[0] != null) {
                return target[0];
            }
        }
        return null;
    }

    @NotNull
    private static List<String> collectAllSelectorsFromFileIndex(@NotNull Project project) {
        Set<String> selectors = new LinkedHashSet<>();
        GlobalSearchScope scope = GlobalSearchScope.projectScope(project);
        FileBasedIndex.getInstance().processAllKeys(
                AngularComponentSelectorFileIndex.NAME,
                key -> {
                    if (!FileBasedIndex.getInstance()
                            .getContainingFiles(AngularComponentSelectorFileIndex.NAME, key, scope).isEmpty()) {
                        selectors.add(key);
                    }
                    return true;
                },
                project
        );
        if (selectors.isEmpty()) {
            return Collections.emptyList();
        }
        return selectors.stream().sorted().collect(Collectors.toList());
    }

    private static @NotNull SelectorIndex getOrBuildScanIndex(@NotNull Project project) {
        long currentModCount = PsiModificationTracker.getInstance(project).getModificationCount();
        SelectorIndexCache cache = project.getUserData(SELECTOR_INDEX_KEY);
        if (cache != null && cache.modificationCount == currentModCount) {
            return cache.index;
        }
        SelectorIndex rebuilt = buildScanIndex(project);
        project.putUserData(SELECTOR_INDEX_KEY, new SelectorIndexCache(currentModCount, rebuilt));
        return rebuilt;
    }

    private static @NotNull SelectorIndex buildScanIndex(@NotNull Project project) {
        Collection<VirtualFile> files = FilenameIndex.getAllFilesByExt(project, "ts", GlobalSearchScope.projectScope(project));
        LOG.debug("[AngularSelector] building scan index from ts files=" + files.size());
        Map<String, SmartPsiElementPointer<PsiElement>> selectors = new LinkedHashMap<>();

        PsiManager psiManager = PsiManager.getInstance(project);
        for (VirtualFile vf : files) {
            if (!AngularIndexScope.isAngularIndexableFile(project, vf)) {
                continue;
            }
            PsiFile file = psiManager.findFile(vf);
            if (file == null || !AngularPsiUtil.hasComponentDecorators(file)) {
                continue;
            }
            collectSelectors(project, file, selectors);
        }
        LOG.debug("[AngularSelector] scan index built size=" + selectors.size());
        return new SelectorIndex(selectors);
    }

    private static void collectSelectors(@NotNull Project project,
                                         @NotNull PsiFile file,
                                         @NotNull Map<String, SmartPsiElementPointer<PsiElement>> target) {
        AngularPsiUtil.collectComponentSelectors(file, (selector, literal) -> target.putIfAbsent(
                selector,
                SmartPointerManager.getInstance(project).createSmartPsiElementPointer(literal)
        ));
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
