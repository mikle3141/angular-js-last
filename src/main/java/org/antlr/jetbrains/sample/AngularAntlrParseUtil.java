package org.antlr.jetbrains.sample;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiManager;
import com.intellij.openapi.vfs.VfsUtilCore;
import org.antlr.jetbrains.sample.psi.TypeScriptPSIFileRoot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Парсит содержимое {@code .ts} через ANTLR, даже если IDE открыла файл встроенным TypeScript PSI.
 * <p>
 * Смещения в in-memory PSI совпадают с реальным файлом, поэтому навигация идёт через
 * {@code realFile.findElementAt(offset)}.
 */
public final class AngularAntlrParseUtil {
    private AngularAntlrParseUtil() {
    }

    @NotNull
    public static PsiFile antlrViewOfFile(@NotNull Project project, @NotNull VirtualFile file) {
        PsiFile cached = PsiManager.getInstance(project).findFile(file);
        if (cached instanceof TypeScriptPSIFileRoot) {
            return cached;
        }
        String text = loadText(file);
        return PsiFileFactory.getInstance(project)
                .createFileFromText(file.getName(), TypeScriptLanguage.INSTANCE, text);
    }

    @Nullable
    public static PsiElement mapOffsetToRealElement(@NotNull Project project,
                                                    @NotNull VirtualFile file,
                                                    int offset) {
        PsiFile realFile = PsiManager.getInstance(project).findFile(file);
        if (realFile == null) {
            return null;
        }
        return realFile.findElementAt(offset);
    }

    @NotNull
    private static String loadText(@NotNull VirtualFile file) {
        try {
            CharSequence text = VfsUtilCore.loadText(file);
            return text == null ? "" : text.toString();
        }
        catch (IOException e) {
            return "";
        }
    }
}
