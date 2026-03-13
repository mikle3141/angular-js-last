package org.antlr.jetbrains.sample.structview;

import com.intellij.navigation.ItemPresentation;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public class TypeScriptStructureViewRootElement extends TypeScriptStructureViewElement {
    public TypeScriptStructureViewRootElement(PsiFile element) {
        super(element);
    }

    @NotNull
    @Override
    public ItemPresentation getPresentation() {
        return new TypeScriptRootPresentation((PsiFile) element);
    }
}
