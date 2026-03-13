package org.antlr.jetbrains.sample;

import com.intellij.lang.Language;

/**
 * TypeScript language support via ANTLR grammar.
 * ID "ANTLRTypeScript" to avoid conflict with built-in TypeScript.
 */
public class TypeScriptLanguage extends Language {
    public static final TypeScriptLanguage INSTANCE = new TypeScriptLanguage();

    private TypeScriptLanguage() {
        super("ANTLRTypeScript");
    }
}
