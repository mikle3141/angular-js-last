package org.antlr.jetbrains.sample.psi;

import com.intellij.psi.PsiReference;
import com.intellij.psi.tree.IElementType;
import org.antlr.intellij.adaptor.psi.ANTLRPsiLeafNode;
import org.antlr.jetbrains.sample.TypeScriptPsiUtil;
import org.jetbrains.annotations.NotNull;

public class TypeScriptStringLiteralPSINode extends ANTLRPsiLeafNode {

    public TypeScriptStringLiteralPSINode(@NotNull IElementType type, @NotNull CharSequence text) {
        super(type, text);
    }

    @Override
    public @NotNull PsiReference @NotNull [] getReferences() {
        if (!TypeScriptPsiUtil.isComponentResourceUrlLiteral(this)) {
            return PsiReference.EMPTY_ARRAY;
        }
        return new PsiReference[]{
                new AngularComponentResourceReference(this, AngularComponentResourceReference.quotedContentRange(this))
        };
    }
}
