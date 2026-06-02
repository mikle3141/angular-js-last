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
import org.antlr.jetbrains.sample.psi.TypeScriptPSIFileRoot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Единый кэшируемый индекс Angular selector'ов по всем {@code .ts} файлам проекта.
 * <p>
 * Индекс строится один раз на «поколение» PSI ({@link PsiModificationTracker}) и
 * переиспользуется в {@link AngularHtmlTagReferenceContributor},
 * {@link AngularHtmlGotoDeclarationHandler} и {@link AngularHtmlTagCompletionContributor}.
 * <p>
 * Selector'ы ищутся через PSI/XPath в декораторах {@code @Component}, без regex по тексту файла.
 */
public final class AngularSelectorIndex {
    private static final Logger LOG = Logger.getInstance(AngularSelectorIndex.class);
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
            if (!(psi instanceof TypeScriptPSIFileRoot tsFile)) {
                continue;
            }
            if (!TypeScriptPsiUtil.hasComponentDecorators(tsFile)) {
                continue;
            }
            collectSelectors(tsFile, selectors);
        }
        LOG.debug("[AngularSelector] index built size=" + selectors.size());
        return new SelectorIndex(selectors);
    }

    private static void collectSelectors(@NotNull PsiFile tsFile,
                                         @NotNull Map<String, SmartPsiElementPointer<PsiElement>> target) {
        TypeScriptPsiUtil.collectComponentSelectors(tsFile, (selector, literal) -> target.putIfAbsent(
                selector,
                SmartPointerManager.getInstance(tsFile.getProject()).createSmartPsiElementPointer(literal)
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
