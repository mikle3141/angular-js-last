package org.antlr.jetbrains.sample.parser;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.TokenStream;

/**
 * Base class for TypeScriptParser. Provides stub implementations for
 * semantic predicates used in the TypeScript parser grammar.
 */
public abstract class TypeScriptParserBase extends Parser {

    public TypeScriptParserBase(TokenStream input) {
        super(input);
    }

    public boolean notLineTerminator() {
        return true;
    }

    public boolean notOpenBraceAndNotFunctionAndNotInterface() {
        return true;
    }

    public boolean p(String s) {
        return true;
    }

    public boolean n(String s) {
        return true;
    }

    public boolean lineTerminatorAhead() {
        return false;
    }

    public boolean closeBrace() {
        return false;
    }
}
