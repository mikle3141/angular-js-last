package org.antlr.jetbrains.sample;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * Ограничивает Angular-индексацию исходниками проекта, без {@code node_modules} и артефактов сборки.
 */
public final class AngularIndexScope {
    private AngularIndexScope() {
    }

    public static boolean isAngularIndexableFile(@NotNull Project project, @NotNull VirtualFile file) {
        if (!file.isValid() || file.isDirectory() || isExcludedPath(file.getPath())) {
            return false;
        }
        ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);
        if (fileIndex.isInSourceContent(file)) {
            return true;
        }
        // Fallback: Angular CLI иногда помечает весь модуль как content root без src как source root.
        return fileIndex.isInContent(file) && isLikelyProjectSource(file.getPath());
    }

    private static boolean isLikelyProjectSource(@NotNull String path) {
        String normalized = path.replace('\\', '/');
        return normalized.contains("/src/") && normalized.endsWith(".ts");
    }

    public static boolean isExcludedPath(@NotNull String path) {
        String normalized = path.replace('\\', '/');
        return normalized.contains("/node_modules/")
                || normalized.contains("/dist/")
                || normalized.contains("/.angular/")
                || normalized.contains("/build/")
                || normalized.contains("/out-tsc/");
    }
}
