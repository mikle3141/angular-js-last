package com.gigaide.angular;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Обратная навигация: из HTML/CSS/SCSS файла к {@code templateUrl}/{@code styleUrls} в TS.
 */
public class AngularResourceGotoDeclarationHandler implements GotoDeclarationHandler {

    @Override
    public PsiElement @Nullable [] getGotoDeclarationTargets(@Nullable PsiElement sourceElement,
                                                             int offset,
                                                             Editor editor) {
        if (sourceElement == null) {
            return null;
        }
        PsiFile file = sourceElement.getContainingFile();
        if (file == null) {
            return null;
        }
        VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile == null || !isAngularResourceFile(virtualFile.getName())) {
            return null;
        }
        // На имени Angular-тега работает AngularHtmlGotoDeclarationHandler / PsiReference.
        if (AngularHtmlNavigationUtil.findComponentTagNameAt(sourceElement, offset, editor) != null) {
            return null;
        }
        List<PsiElement> targets = AngularComponentResourceIndex.resolveAllResourceReferences(
                sourceElement.getProject(), virtualFile);
        return targets.isEmpty() ? null : targets.toArray(PsiElement.EMPTY_ARRAY);
    }

    private static boolean isAngularResourceFile(@NotNull String fileName) {
        return fileName.endsWith(".html")
                || fileName.endsWith(".css")
                || fileName.endsWith(".scss")
                || fileName.endsWith(".sass")
                || fileName.endsWith(".less");
    }
}
