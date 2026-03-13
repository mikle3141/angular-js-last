package org.antlr.jetbrains.sample;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class TypeScriptExternalAnnotator extends ExternalAnnotator<PsiFile, List<Object>> {

    @Nullable
    @Override
    public PsiFile collectInformation(@NotNull PsiFile file) {
        return file;
    }

    @Nullable
    @Override
    public List<Object> doAnnotate(PsiFile file) {
        return Collections.emptyList();
    }

    @Override
    public void apply(@NotNull PsiFile file, List<Object> issues, @NotNull AnnotationHolder holder) {
    }
}
