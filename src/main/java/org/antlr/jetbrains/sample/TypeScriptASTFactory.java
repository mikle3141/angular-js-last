package org.antlr.jetbrains.sample;

import com.intellij.core.CoreASTFactory;
import com.intellij.psi.impl.source.tree.CompositeElement;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;
import org.antlr.intellij.adaptor.lexer.TokenIElementType;
import org.antlr.jetbrains.sample.parser.TypeScriptLexer;
import org.antlr.jetbrains.sample.psi.IdentifierPSINode;
import org.jetbrains.annotations.NotNull;

public class TypeScriptASTFactory extends CoreASTFactory {
    @NotNull
    @Override
    public CompositeElement createComposite(IElementType type) {
        return super.createComposite(type);
    }

    @NotNull
    @Override
    public LeafElement createLeaf(@NotNull IElementType type, CharSequence text) {
        if (type instanceof TokenIElementType &&
            ((TokenIElementType) type).getANTLRTokenType() == TypeScriptLexer.Identifier) {
            return new IdentifierPSINode(type, text);
        }
        return super.createLeaf(type, text);
    }
}
