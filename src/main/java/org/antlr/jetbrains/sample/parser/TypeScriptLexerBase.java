package org.antlr.jetbrains.sample.parser;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Lexer;

/**
 * Base class for TypeScriptLexer. Provides stub implementations for
 * semantic predicates and actions used in the TypeScript lexer grammar.
 */
public abstract class TypeScriptLexerBase extends Lexer {

    protected int templateDepth = 0;
    protected int braceDepth = 0;

    public TypeScriptLexerBase(CharStream input) {
        super(input);
    }

    public boolean IsRegexPossible() {
        return false;
    }

    public void ProcessOpenBrace() {
        braceDepth++;
    }

    public void ProcessCloseBrace() {
        braceDepth--;
    }

    public boolean IsInTemplateString() {
        return templateDepth > 0;
    }

    public boolean IsStrictMode() {
        return false;
    }

    public void ProcessStringLiteral() {
    }

    public void IncreaseTemplateDepth() {
        templateDepth++;
    }

    public void DecreaseTemplateDepth() {
        templateDepth--;
    }

    public void StartTemplateString() {
        templateDepth++;
    }
}
