package org.antlr.jetbrains.sample.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import org.antlr.intellij.adaptor.psi.ScopeNode;
import org.antlr.jetbrains.sample.Icons;
import org.antlr.jetbrains.sample.TypeScriptFileType;
import org.antlr.jetbrains.sample.TypeScriptLanguage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class TypeScriptPSIFileRoot extends PsiFileBase implements ScopeNode {
    public TypeScriptPSIFileRoot(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, TypeScriptLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return TypeScriptFileType.INSTANCE;
    }

    @Override
    public String toString() {
        return "TypeScript file";
    }

    @Override
    public Icon getIcon(int flags) {
        return Icons.SAMPLE_ICON;
    }

    @Nullable
    @Override
    public ScopeNode getContext() {
        return null;
    }

    @Nullable
    @Override
    public PsiElement resolve(PsiNamedElement element) {
        return null;
    }
}
