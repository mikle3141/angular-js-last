package org.antlr.jetbrains.sample.structview;

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.NavigationItem;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import org.antlr.intellij.adaptor.xpath.XPath;
import org.antlr.jetbrains.sample.TypeScriptLanguage;
import org.antlr.jetbrains.sample.psi.TypeScriptPSIFileRoot;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TypeScriptStructureViewElement implements StructureViewTreeElement, SortableTreeElement {
    protected final PsiElement element;

    public TypeScriptStructureViewElement(PsiElement element) {
        this.element = element;
    }

    @Override
    public Object getValue() {
        return element;
    }

    @Override
    public void navigate(boolean requestFocus) {
        if (element instanceof NavigationItem) {
            ((NavigationItem) element).navigate(requestFocus);
        }
    }

    @Override
    public boolean canNavigate() {
        return element instanceof NavigationItem && ((NavigationItem) element).canNavigate();
    }

    @Override
    public boolean canNavigateToSource() {
        return element instanceof NavigationItem && ((NavigationItem) element).canNavigateToSource();
    }

    @NotNull
    @Override
    public String getAlphaSortKey() {
        String s = element instanceof PsiNamedElement ? ((PsiNamedElement) element).getName() : null;
        return s != null ? s : element.getText();
    }

    @NotNull
    @Override
    public ItemPresentation getPresentation() {
        return new TypeScriptItemPresentation(element);
    }

    @NotNull
    @Override
    public TreeElement[] getChildren() {
        if (element instanceof TypeScriptPSIFileRoot) {
            Collection<? extends PsiElement> elements = XPath.findAll(
                TypeScriptLanguage.INSTANCE, element, "/program/sourceElements/sourceElement");
            List<TreeElement> treeElements = new ArrayList<>(elements.size());
            for (PsiElement el : elements) {
                treeElements.add(new TypeScriptStructureViewElement(el));
            }
            return treeElements.toArray(new TreeElement[0]);
        }
        return new TreeElement[0];
    }
}
