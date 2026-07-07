package com.gigaide.angular;

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
import com.gigaide.angular.index.AngularComponentResourceFileIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Обратный индекс: HTML/CSS/SCSS файл → строковые литералы {@code templateUrl}/{@code styleUrls} в TS.
 */
public final class AngularComponentResourceIndex {
    private static final Logger LOG = Logger.getInstance(AngularComponentResourceIndex.class);
    private static final Key<ResourceIndexCache> RESOURCE_INDEX_KEY = Key.create("angular.component.resource.index");

    private AngularComponentResourceIndex() {
    }

    @Nullable
    public static PsiElement resolveResourceReference(@NotNull Project project, @NotNull VirtualFile resourceFile) {
        List<PsiElement> targets = resolveAllResourceReferences(project, resourceFile);
        return targets.isEmpty() ? null : targets.get(0);
    }

    @NotNull
    public static List<PsiElement> resolveAllResourceReferences(@NotNull Project project,
                                                                @NotNull VirtualFile resourceFile) {
        List<PsiElement> fromFileIndex = resolveFromFileIndex(project, resourceFile);
        if (!fromFileIndex.isEmpty()) {
            return fromFileIndex;
        }
        List<PsiElement> fromScan = getOrBuildScanIndex(project).resolve(resourceFile);
        LOG.debug("[AngularResource] resolved via scan index for " + resourceFile.getName()
                + ", targets=" + fromScan.size());
        return fromScan;
    }

    @NotNull
    private static List<PsiElement> resolveFromFileIndex(@NotNull Project project,
                                                         @NotNull VirtualFile resourceFile) {
        GlobalSearchScope scope = GlobalSearchScope.projectScope(project);
        Collection<VirtualFile> tsFiles = FileBasedIndex.getInstance().getContainingFiles(
                AngularComponentResourceFileIndex.NAME,
                AngularComponentResourceFileIndex.resourceKey(resourceFile),
                scope
        );
        if (tsFiles.isEmpty()) {
            return Collections.emptyList();
        }
        List<PsiElement> result = new ArrayList<>();
        PsiManager psiManager = PsiManager.getInstance(project);
        for (VirtualFile vf : tsFiles) {
            if (!AngularIndexScope.isAngularIndexableFile(project, vf)) {
                continue;
            }
            PsiFile file = psiManager.findFile(vf);
            if (file == null) {
                continue;
            }
            VirtualFile baseDir = vf.getParent();
            if (baseDir == null) {
                continue;
            }
            collectMatchingLiterals(project, file, baseDir, resourceFile, result);
        }
        return result;
    }

    private static void collectMatchingLiterals(@NotNull Project project,
                                              @NotNull PsiFile file,
                                              @NotNull VirtualFile baseDir,
                                              @NotNull VirtualFile resourceFile,
                                              @NotNull List<PsiElement> result) {
        AngularPsiUtil.collectComponentResourceLiterals(file, (literal, property) -> {
            String path = AngularPsiUtil.unquoteStringLiteral(literal);
            if (path.isEmpty()) {
                return;
            }
            if (AngularResourcePathResolver.resourcePathMatchesFile(project, baseDir, path, resourceFile)) {
                result.add(literal);
            }
        });
    }

    private static @NotNull ResourceIndex getOrBuildScanIndex(@NotNull Project project) {
        long currentModCount = PsiModificationTracker.getInstance(project).getModificationCount();
        ResourceIndexCache cache = project.getUserData(RESOURCE_INDEX_KEY);
        if (cache != null && cache.modificationCount == currentModCount) {
            return cache.index;
        }
        ResourceIndex rebuilt = buildScanIndex(project);
        project.putUserData(RESOURCE_INDEX_KEY, new ResourceIndexCache(currentModCount, rebuilt));
        return rebuilt;
    }

    private static @NotNull ResourceIndex buildScanIndex(@NotNull Project project) {
        Collection<VirtualFile> files = FilenameIndex.getAllFilesByExt(project, "ts", GlobalSearchScope.projectScope(project));
        Map<VirtualFile, List<SmartPsiElementPointer<PsiElement>>> index = new LinkedHashMap<>();
        PsiManager psiManager = PsiManager.getInstance(project);

        for (VirtualFile vf : files) {
            if (!AngularIndexScope.isAngularIndexableFile(project, vf)) {
                continue;
            }
            VirtualFile baseDir = vf.getParent();
            if (baseDir == null) {
                continue;
            }
            PsiFile file = psiManager.findFile(vf);
            if (file == null || !AngularPsiUtil.hasComponentDecorators(file)) {
                continue;
            }
            AngularPsiUtil.collectComponentResourceLiterals(file, (literal, property) -> {
                String path = AngularPsiUtil.unquoteStringLiteral(literal);
                if (path.isEmpty()) {
                    return;
                }
                VirtualFile target = AngularResourcePathResolver.resolveRelativeResource(project, baseDir, path);
                if (target == null) {
                    return;
                }
                SmartPsiElementPointer<PsiElement> pointer =
                        SmartPointerManager.getInstance(project).createSmartPsiElementPointer(literal);
                index.computeIfAbsent(target, ignored -> new ArrayList<>()).add(pointer);
            });
        }
        return new ResourceIndex(index);
    }

    private static final class ResourceIndex {
        private final Map<VirtualFile, List<SmartPsiElementPointer<PsiElement>>> resourceToLiterals;

        private ResourceIndex(@NotNull Map<VirtualFile, List<SmartPsiElementPointer<PsiElement>>> resourceToLiterals) {
            this.resourceToLiterals = resourceToLiterals;
        }

        private @NotNull List<PsiElement> resolve(@NotNull VirtualFile resourceFile) {
            List<SmartPsiElementPointer<PsiElement>> pointers = resourceToLiterals.get(resourceFile);
            if (pointers == null || pointers.isEmpty()) {
                return Collections.emptyList();
            }
            List<PsiElement> result = new ArrayList<>(pointers.size());
            for (SmartPsiElementPointer<PsiElement> pointer : pointers) {
                PsiElement element = pointer.getElement();
                if (element != null) {
                    result.add(element);
                }
            }
            return result;
        }
    }

    private static final class ResourceIndexCache {
        private final long modificationCount;
        private final ResourceIndex index;

        private ResourceIndexCache(long modificationCount, @NotNull ResourceIndex index) {
            this.modificationCount = modificationCount;
            this.index = index;
        }
    }
}
