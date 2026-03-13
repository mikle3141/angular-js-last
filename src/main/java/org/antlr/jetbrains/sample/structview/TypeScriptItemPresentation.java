package org.antlr.jetbrains.sample.structview;

import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.psi.PsiElement;
import org.antlr.jetbrains.sample.Icons;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class TypeScriptItemPresentation implements ItemPresentation {
    protected final PsiElement element;

    protected TypeScriptItemPresentation(PsiElement element) {
        this.element = element;
    }

    @Nullable
    @Override
    public Icon getIcon(boolean unused) {
        return Icons.FUNC_ICON;
    }

    @Nullable
    @Override
    public String getPresentableText() {
        ASTNode node = element.getNode();
        String text = node != null ? node.getText() : element.getText();
        return text != null && text.length() > 60 ? text.substring(0, 57) + "..." : text;
    }

    @Nullable
    @Override
    public String getLocationString() {
        return null;
    }
}
