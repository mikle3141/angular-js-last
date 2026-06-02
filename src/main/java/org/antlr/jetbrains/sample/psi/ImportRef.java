package org.antlr.jetbrains.sample.psi;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.IncorrectOperationException;
import org.antlr.jetbrains.sample.TypeScriptPsiUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Reference for an identifier that appears in an import statement.
 * Resolves to the corresponding export (class of function) in the imported module.
 */
public class ImportRef extends SampleElementRef {

    public ImportRef(@NotNull IdentifierPSINode element) {
        super(element);
    }

    @Override
    public boolean isDefSubtree(PsiElement def) {
        if (def instanceof IdentifierPSINode) {
            return TypeScriptPsiUtil.isExportedIdentifier((IdentifierPSINode) def);
        }
        return false;
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        PsiElement importStatement = TypeScriptPsiUtil.findImportStatementAncestor(myElement);
        if (importStatement == null) {
            return null;
        }
        String modulePath = TypeScriptPsiUtil.getImportModulePath(importStatement);
        String importedName = myElement.getName();
        if (modulePath == null || importedName == null || importedName.isEmpty()) {
            return null;
        }

        PsiFile currentFile = myElement.getContainingFile();
        VirtualFile currentVf = currentFile != null ? currentFile.getVirtualFile() : null;
        if (currentVf == null) {
            return null;
        }
        VirtualFile baseDir = currentVf.getParent();
        if (baseDir == null || !baseDir.isDirectory()) {
            return null;
        }

        VirtualFile targetFile = resolveRelativeModule(myElement.getProject(), baseDir, modulePath);
        if (targetFile == null) {
            return null;
        }

        PsiFile targetPsi = PsiManager.getInstance(myElement.getProject()).findFile(targetFile);
        if (!(targetPsi instanceof TypeScriptPSIFileRoot)) {
            return null;
        }

        return TypeScriptPsiUtil.findExportedIdentifier((TypeScriptPSIFileRoot) targetPsi, importedName);
    }

    @Override
    public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
        return myElement.setName(newElementName);
    }

    @Override
    public boolean isReferenceTo(PsiElement def) {
        String refName = myElement.getName();
        if (refName == null) {
            return false;
        }
        if (def instanceof IdentifierPSINode) {
            return refName.equals(((IdentifierPSINode) def).getText());
        }
        return super.isReferenceTo(def);
    }

    @Nullable
    private static VirtualFile resolveRelativeModule(@NotNull Project project,
                                                       @NotNull VirtualFile baseDir,
                                                       @NotNull String modulePath) {
        VirtualFile relative = resolveRelativeToDirectory(baseDir, modulePath);
        if (relative != null) {
            return relative;
        }

        Path basePath = Paths.get(baseDir.getPath());
        Path resolved = basePath.resolve(modulePath).normalize();
        String pathStr = resolved.toString().replace('\\', '/');

        LocalFileSystem fs = LocalFileSystem.getInstance();
        VirtualFile vf = fs.findFileByPath(pathStr);
        if (vf != null && !vf.isDirectory()) {
            return vf;
        }
        if (!pathStr.endsWith(".ts") && !pathStr.endsWith(".tsx")) {
            vf = fs.findFileByPath(pathStr + ".ts");
            if (vf != null && !vf.isDirectory()) {
                return vf;
            }
            vf = fs.findFileByPath(pathStr + ".tsx");
            if (vf != null && !vf.isDirectory()) {
                return vf;
            }
        }
        vf = fs.findFileByPath(pathStr);
        if (vf != null && vf.isDirectory()) {
            VirtualFile index = vf.findChild("index.ts");
            if (index != null) {
                return index;
            }
            index = vf.findChild("index.tsx");
            if (index != null) {
                return index;
            }
        }
        VirtualFile byNio = VirtualFileManager.getInstance().refreshAndFindFileByNioPath(resolved);
        if (byNio != null && !byNio.isDirectory()) {
            return byNio;
        }
        return findModuleInProject(project, modulePath);
    }

    @Nullable
    private static VirtualFile resolveRelativeToDirectory(@NotNull VirtualFile baseDir, @NotNull String modulePath) {
        String rel = modulePath;
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
        if (rel.endsWith(".ts") || rel.endsWith(".tsx")) {
            VirtualFile file = baseDir.findChild(rel);
            return file != null && !file.isDirectory() ? file : null;
        }
        VirtualFile file = baseDir.findChild(rel + ".ts");
        if (file != null && !file.isDirectory()) {
            return file;
        }
        file = baseDir.findChild(rel + ".tsx");
        if (file != null && !file.isDirectory()) {
            return file;
        }
        VirtualFile dir = baseDir.findChild(rel);
        if (dir != null && dir.isDirectory()) {
            VirtualFile index = dir.findChild("index.ts");
            if (index != null) {
                return index;
            }
            index = dir.findChild("index.tsx");
            if (index != null) {
                return index;
            }
        }
        return null;
    }

    @Nullable
    private static VirtualFile findModuleInProject(@NotNull Project project, @NotNull String modulePath) {
        String normalized = modulePath;
        if (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        if (!normalized.endsWith(".ts") && !normalized.endsWith(".tsx")) {
            normalized = normalized + ".ts";
        }
        String expectedName = Paths.get(normalized).getFileName().toString();
        for (VirtualFile candidate : FilenameIndex.getAllFilesByExt(project, "ts", GlobalSearchScope.projectScope(project))) {
            if (expectedName.equals(candidate.getName())) {
                return candidate;
            }
        }
        return null;
    }
}
