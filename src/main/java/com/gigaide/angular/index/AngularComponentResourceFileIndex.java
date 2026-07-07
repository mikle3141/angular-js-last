package com.gigaide.angular.index;

import com.gigaide.javascript.fileTypes.TsFileType;
import com.gigaide.angular.AngularIndexScope;
import com.gigaide.angular.AngularPsiUtil;
import com.gigaide.angular.AngularResourcePathResolver;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.indexing.*;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * File-based index: URL ресурсного файла (HTML/CSS/SCSS) → {@code .ts} файлы с
 * {@code templateUrl}/{@code styleUrl}/{@code styleUrls}, ссылающимися на этот ресурс.
 */
public final class AngularComponentResourceFileIndex extends ScalarIndexExtension<String> {
    public static final ID<String, Void> NAME = ID.create("com.gigaide.angular.component.resource");

    private static final DataIndexer<String, Void, FileContent> INDEXER = inputData -> {
        if (AngularIndexScope.isExcludedPath(inputData.getFile().getPath())) {
            return Collections.emptyMap();
        }
        Project project = inputData.getProject();
        if (project == null) {
            return Collections.emptyMap();
        }
        PsiFile psiFile = inputData.getPsiFile();
        if (psiFile == null) {
            return Collections.emptyMap();
        }
        VirtualFile tsFile = inputData.getFile();
        VirtualFile baseDir = tsFile.getParent();
        if (baseDir == null) {
            return Collections.emptyMap();
        }
        Map<String, Void> resourceKeys = new HashMap<>();
        AngularPsiUtil.collectComponentResourceLiterals(psiFile, (literal, property) -> {
            String path = AngularPsiUtil.unquoteStringLiteral(literal);
            if (path.isEmpty()) {
                return;
            }
            VirtualFile target = AngularResourcePathResolver.resolveRelativeResource(project, baseDir, path);
            if (target == null) {
                return;
            }
            resourceKeys.put(resourceKey(target), null);
        });
        return resourceKeys;
    };

    @NotNull
    public static String resourceKey(@NotNull VirtualFile resourceFile) {
        return resourceFile.getUrl();
    }

    @Override
    public @NotNull ID<String, Void> getName() {
        return NAME;
    }

    @Override
    public @NotNull DataIndexer<String, Void, FileContent> getIndexer() {
        return INDEXER;
    }

    @Override
    public @NotNull KeyDescriptor<String> getKeyDescriptor() {
        return EnumeratorStringDescriptor.INSTANCE;
    }

    @Override
    public @NotNull FileBasedIndex.InputFilter getInputFilter() {
        return new DefaultFileTypeSpecificInputFilter(TsFileType.INSTANCE);
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @Override
    public int getVersion() {
        return 1;
    }
}
