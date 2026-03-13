package org.antlr.jetbrains.sample;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Map;

public class TypeScriptColorSettingsPage implements ColorSettingsPage {
    private static final AttributesDescriptor[] DESCRIPTORS = new AttributesDescriptor[]{
        new AttributesDescriptor("Identifier", TypeScriptSyntaxHighlighter.ID),
        new AttributesDescriptor("Keyword", TypeScriptSyntaxHighlighter.KEYWORD),
        new AttributesDescriptor("Decorator", TypeScriptSyntaxHighlighter.DECORATOR),
        new AttributesDescriptor("String", TypeScriptSyntaxHighlighter.STRING),
        new AttributesDescriptor("Number", TypeScriptSyntaxHighlighter.NUMBER),
        new AttributesDescriptor("Line comment", TypeScriptSyntaxHighlighter.LINE_COMMENT),
        new AttributesDescriptor("Block comment", TypeScriptSyntaxHighlighter.BLOCK_COMMENT),
    };

    @Nullable
    @Override
    public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return null;
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return Icons.SAMPLE_ICON;
    }

    @NotNull
    @Override
    public SyntaxHighlighter getHighlighter() {
        return new TypeScriptSyntaxHighlighter();
    }

    @NotNull
    @Override
    public String getDemoText() {
        return "/* block comment */\n" +
            "@Component\n" +
            "class App { }\n" +
            "function greet(name: string): void {\n" +
            "   // line comment\n" +
            "   const x = 42;\n" +
            "   console.log(\"Hello \" + name);\n" +
            "}\n" +
            "const y: number = 1.5;\n";
    }

    @NotNull
    @Override
    public AttributesDescriptor[] getAttributeDescriptors() {
        return DESCRIPTORS;
    }

    @NotNull
    @Override
    public ColorDescriptor[] getColorDescriptors() {
        return ColorDescriptor.EMPTY_ARRAY;
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return "TypeScript (ANTLR)";
    }
}
