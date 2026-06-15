package org.antlr.jetbrains.sample;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Разрешение относительных путей {@code templateUrl}/{@code styleUrls} в {@link VirtualFile}.
 */
public final class AngularResourcePathResolver {
    private static final String[] RESOURCE_EXTENSIONS = {".html", ".css", ".scss", ".sass", ".less"};

    private AngularResourcePathResolver() {
    }

    @Nullable
    public static PsiFile resolveResourceFile(@NotNull Project project,
                                              @NotNull VirtualFile baseDir,
                                              @NotNull String resourcePath) {
        VirtualFile target = resolveRelativeResource(project, baseDir, resourcePath);
        return target == null ? null : PsiManager.getInstance(project).findFile(target);
    }

    public static boolean resourcePathMatchesFile(@NotNull Project project,
                                                  @NotNull VirtualFile baseDir,
                                                  @NotNull String resourcePath,
                                                  @NotNull VirtualFile targetFile) {
        VirtualFile resolved = resolveRelativeResource(project, baseDir, resourcePath);
        return targetFile.equals(resolved);
    }

    @Nullable
    public static VirtualFile resolveRelativeResource(@NotNull Project project,
                                                      @NotNull VirtualFile baseDir,
                                                      @NotNull String resourcePath) {
        VirtualFile relative = resolveRelativeToDirectory(baseDir, resourcePath);
        if (relative != null) {
            return relative;
        }

        Path basePath = Paths.get(baseDir.getPath());
        Path resolved = basePath.resolve(resourcePath).normalize();
        String pathStr = resolved.toString().replace('\\', '/');

        LocalFileSystem fs = LocalFileSystem.getInstance();
        VirtualFile vf = fs.findFileByPath(pathStr);
        if (vf != null && !vf.isDirectory()) {
            return vf;
        }
        vf = tryKnownExtensions(fs, pathStr);
        if (vf != null) {
            return vf;
        }
        VirtualFile byNio = VirtualFileManager.getInstance().refreshAndFindFileByNioPath(resolved);
        if (byNio != null && !byNio.isDirectory()) {
            return byNio;
        }
        return findResourceInProject(project, resourcePath);
    }

    @Nullable
    private static VirtualFile resolveRelativeToDirectory(@NotNull VirtualFile baseDir, @NotNull String resourcePath) {
        String rel = resourcePath;
        if (rel.startsWith("./")) {
            rel = rel.substring(2);
        }
        else if (rel.startsWith("../")) {
            VirtualFile parent = baseDir.getParent();
            if (parent == null) {
                return null;
            }
            return resolveRelativeToDirectory(parent, "./" + rel.substring(3));
        }
        if (rel.isEmpty()) {
            return null;
        }
        if (hasKnownExtension(rel)) {
            VirtualFile file = baseDir.findChild(rel);
            return file != null && !file.isDirectory() ? file : null;
        }
        for (String extension : RESOURCE_EXTENSIONS) {
            VirtualFile file = baseDir.findChild(rel + extension);
            if (file != null && !file.isDirectory()) {
                return file;
            }
        }
        VirtualFile file = baseDir.findChild(rel);
        return file != null && !file.isDirectory() ? file : null;
    }

    @Nullable
    private static VirtualFile tryKnownExtensions(@NotNull LocalFileSystem fs, @NotNull String pathStr) {
        if (hasKnownExtension(pathStr)) {
            return null;
        }
        for (String extension : RESOURCE_EXTENSIONS) {
            VirtualFile vf = fs.findFileByPath(pathStr + extension);
            if (vf != null && !vf.isDirectory()) {
                return vf;
            }
        }
        return null;
    }

    @Nullable
    private static VirtualFile findResourceInProject(@NotNull Project project, @NotNull String resourcePath) {
        String normalized = resourcePath;
        if (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        String expectedName = Paths.get(normalized).getFileName().toString();
        if (expectedName.isEmpty()) {
            return null;
        }
        GlobalSearchScope scope = GlobalSearchScope.projectScope(project);
        for (String extension : resourceExtensionsForLookup(normalized)) {
            String ext = extension.startsWith(".") ? extension.substring(1) : extension;
            for (VirtualFile candidate : FilenameIndex.getAllFilesByExt(project, ext, scope)) {
                if (expectedName.equals(candidate.getName())) {
                    return candidate;
                }
            }
        }
        return null;
    }

    @NotNull
    private static String[] resourceExtensionsForLookup(@NotNull String normalized) {
        if (hasKnownExtension(normalized)) {
            int dot = normalized.lastIndexOf('.');
            return new String[]{normalized.substring(dot)};
        }
        return RESOURCE_EXTENSIONS;
    }

    private static boolean hasKnownExtension(@NotNull String path) {
        for (String extension : RESOURCE_EXTENSIONS) {
            if (path.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }
}
