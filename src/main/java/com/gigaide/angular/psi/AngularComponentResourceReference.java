package com.gigaide.angular.psi;

import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.gigaide.angular.AngularResourcePathResolver;
import com.gigaide.angular.AngularPsiUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AngularComponentResourceReference extends PsiReferenceBase<PsiElement> {

    public AngularComponentResourceReference(@NotNull PsiElement element, @NotNull TextRange rangeInElement) {
        super(element, rangeInElement);
    }

    @NotNull
    public static TextRange quotedContentRange(@NotNull PsiElement element) {
        String text = element.getText();
        if (text.length() >= 2) {
            char quote = text.charAt(0);
            if ((quote == '\'' || quote == '"') && text.charAt(text.length() - 1) == quote) {
                return TextRange.create(1, text.length() - 1);
            }
        }
        return TextRange.from(0, text.length());
    }

    @Override
    public @Nullable PsiElement resolve() {
        String path = AngularPsiUtil.unquoteStringLiteral(myElement);
        if (path.isEmpty()) {
            return null;
        }
        PsiFile currentFile = myElement.getContainingFile();
        if (currentFile == null) {
            return null;
        }
        VirtualFile baseDir = currentFile.getVirtualFile();
        if (baseDir == null) {
            return null;
        }
        baseDir = baseDir.getParent();
        if (baseDir == null || !baseDir.isDirectory()) {
            return null;
        }
        return AngularResourcePathResolver.resolveResourceFile(myElement.getProject(), baseDir, path);
    }
}
