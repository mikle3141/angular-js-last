package org.antlr.jetbrains.sample;

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

public final class AngularHighlighterKeys {
    public static final TextAttributesKey DECORATOR =
            createTextAttributesKey("ANGULAR_DECORATOR", DefaultLanguageHighlighterColors.METADATA);

    private AngularHighlighterKeys() {
    }
}
